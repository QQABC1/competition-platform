package com.platform.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.api.client.RemoteUserService;
import com.platform.api.domain.UserInternalVO;
import com.platform.common.api.R;
import com.platform.common.constant.RabbitMQConstants;
import com.platform.common.constant.RedisKeyConstants;
import com.platform.common.exception.BusinessException;
import com.platform.common.message.RegistrationAuditMessage;
import com.platform.registration.dto.RegistrationAuditDTO;
import com.platform.registration.dto.RegistrationDTO;
import com.platform.registration.dto.RegistrationQueryDTO;
import com.platform.registration.entity.Registration;
import com.platform.registration.mapper.RegistrationMapper;
import com.platform.registration.service.RegistrationService;
import com.platform.registration.vo.RegistrationInitVO;
import com.platform.registration.vo.RegistrationListVO;
import com.platform.registration.vo.RegistrationStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements RegistrationService {

    /**
     * 注入 api 模块定义的 Feign 接口
     * 记得在启动类添加 @EnableFeignClients(basePackages = "com.platform.api.client")
     */
    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    @Qualifier("registrationThreadPool")
    private Executor threadPool;

    // 实际项目中这里需要注入 UserFeignClient 来获取用户的学院信息
    // @Autowired
    // private UserFeignClient userFeignClient;

    @Override
    public IPage<RegistrationListVO> getOrganizerList(RegistrationQueryDTO queryDTO) {
        // 1. 构造查询条件
        Page<Registration> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();

        // 必须指定竞赛ID
        if (queryDTO.getCompetitionId() == null) {
            throw new BusinessException("竞赛ID不能为空");
        }
        wrapper.eq(Registration::getCompetitionId, queryDTO.getCompetitionId());

        // 状态筛选
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Registration::getStatus, queryDTO.getStatus());
        }

        // 关键词搜索 (姓名 或 学号)
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Registration::getStudentName, queryDTO.getKeyword())
                    .or()
                    .like(Registration::getStudentId, queryDTO.getKeyword()));
        }

        // 排序：时间倒序
        wrapper.orderByDesc(Registration::getCreateTime);

        // 2. 执行分页查询
        Page<Registration> resultPage = this.page(pageParam, wrapper);

        // 3. 转换 VO
        List<RegistrationListVO> voList = resultPage.getRecords().stream().map(item -> {
            RegistrationListVO vo = new RegistrationListVO();
            BeanUtils.copyProperties(item, vo);

            // TODO: 这里应该调用 UserFeignClient 获取学院信息
            // UserVO user = userFeignClient.getById(item.getUserId());
            // vo.setCollegeInfo(user != null ? user.getMajor() : "未知");
            vo.setCollegeInfo("计算机学院 (演示数据)");

            return vo;
        }).collect(Collectors.toList());

        // 4. 封装返回
        Page<RegistrationListVO> voPage = new Page<>();
        BeanUtils.copyProperties(resultPage, voPage, "records");
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public void auditRegistration(RegistrationAuditDTO dto) {
        // 1. 查询记录
        Registration reg = this.getById(dto.getId());
        if (reg == null) {
            throw new BusinessException("报名记录不存在");
        }

        // 2. 校验状态
        if (reg.getStatus() == 0) {
            throw new BusinessException("该报名已取消，无法审核");
        }

        // 3. 更新状态
        Registration updateEntity = new Registration();
        updateEntity.setId(dto.getId());

        int newStatus = 0;
        if (dto.getPass()) {
            // 通过 -> 状态 2
            updateEntity.setStatus(2);
            updateEntity.setAuditReason("审核通过");
            newStatus = 2;
        } else {
            // 驳回 -> 状态 3
            if (!StringUtils.hasText(dto.getReason())) {
                throw new BusinessException("驳回必须填写理由");
            }
            updateEntity.setStatus(3);
            updateEntity.setAuditReason(dto.getReason());
            newStatus = 3;
        }

        this.updateById(updateEntity);

        //  构建并发送异步通知消息
        RegistrationAuditMessage message = new RegistrationAuditMessage()
                .setRegistrationId(reg.getId())
                .setUserId(reg.getUserId()) // 假设实体里有userId
                .setCompetitionId(reg.getCompetitionId()) // 假设实体里有competitionId
                .setStatus(newStatus)
                .setAuditReason(dto.getReason())
                .setAuditTime(System.currentTimeMillis());


        // 发送消息到 RabbitMQ
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.REGISTRATION_AUDIT_ROUTING_KEY,
                message
        );
    }

    /**
     * 获取报名初始化信息 (Pre-check)
     * 1. 检查是否已报名
     * 2. 回显用户信息 (远程调用)
     */
    @Override
    public RegistrationInitVO getInitInfo(Long userId, Long competitionId) {
        RegistrationInitVO vo = new RegistrationInitVO();

        // ---------------------------------------------------------
        // 步骤 1: 查询本地数据库，检查该用户对该比赛的报名状态
        // ---------------------------------------------------------
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
                .eq(Registration::getCompetitionId, competitionId)
                .eq(Registration::getStatus, 1); // 1 表示有效报名

        Registration registration = this.getOne(wrapper);

        if (registration != null) {
            vo.setIsRegistered(true);
            vo.setRegistrationId(registration.getId());
        } else {
            vo.setIsRegistered(false);
            vo.setRegistrationId(null);
        }

        // ---------------------------------------------------------
        // 步骤 2: 远程调用 User 服务，获取学生基础信息用于回显
        // ---------------------------------------------------------
        try {
            R<UserInternalVO> userResp = remoteUserService.getUserInfo(userId);

            if (userResp != null && userResp.getCode() == 200 && userResp.getData() != null) {
                UserInternalVO user = userResp.getData();

                // 数据回填
                vo.setStudentName(user.getRealName());
                vo.setStudentId(user.getStudentId());
                vo.setMajor(user.getMajor());
                vo.setPhone(user.getPhone());

                // 判断信息是否完善 (假设 1 为已完善)
                // 如果未完善，前端会据此弹窗提示去绑定信息
                vo.setIsProfileComplete(user.getIsInfoComplete() != null && user.getIsInfoComplete() == 1);
            } else {
                // 如果调用成功但业务返回失败，或数据为空
                log.warn("获取用户信息失败或为空, userId: {}", userId);
                vo.setIsProfileComplete(false);
            }
        } catch (Exception e) {
            // 远程调用如果挂了（比如 User 服务没启动），这里做一个容错处理
            log.error("远程调用用户服务异常", e);
            vo.setIsProfileComplete(false);
        }

        return vo;
    }

    /**
     * 提交报名 (实现参考)
     */
    @Override
    public void apply(Long userId, RegistrationDTO.Apply dto) {
        // 1. 幂等性检查
        long count = this.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .eq(Registration::getCompetitionId, dto.getCompetitionId())
                .eq(Registration::getStatus, 1));

        if (count > 0) {
            throw new BusinessException("您已报名该竞赛，请勿重复提交");
        }

        // 2. 数据转换与入库
        Registration reg = new Registration();
        BeanUtils.copyProperties(dto, reg);
        reg.setUserId(userId);
        reg.setStatus(1); // 1: 已报名
        // 注意：DTO 中的 attachmentUrl 对应 Entity 中的 attachment
        reg.setAttachment(dto.getAttachmentUrl());

        this.save(reg);
    }


    /**
     * 高并发提交报名 (异步削峰)
     */
    @Override
    public void applyAsync(Long userId, RegistrationDTO.Apply dto) {
        Long compId = dto.getCompetitionId();

        // Redis Keys 设计
        String idempKey = RedisKeyConstants.IDEMPOTENT_KEY_PREFIX  + compId + ":" + userId; // 防重Key
        String capKey = RedisKeyConstants.CAPACITY_KEY_PREFIX + compId;                     // 名额Key
        String resKey = RedisKeyConstants.RESULT_KEY_PREFIX + compId + ":" + userId;  // 结果状态Key

        // ================= 1. 第一道防线：幂等防重拦截 =================
        // SETNX: 如果键不存在则设置成功(返回true)，存在则失败(返回false)。防连击、防重复提交。
        Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(idempKey, "1", 1, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(isFirst)) {
            throw new BusinessException("您的报名正在处理中或已成功，请勿重复点击");
        }

        // ================= 2. 第二道防线：预扣减名额 (防超卖) =================
        // 注意：竞赛发布时，必须提前将名额写入 capKey (即缓存预热)
        Long remain = redisTemplate.opsForValue().decrement(capKey);
        if (remain != null && remain < 0) {
            // 扣减后小于0说明名额已满，必须立刻回滚！
            redisTemplate.opsForValue().increment(capKey); // 把名额加回去
            redisTemplate.delete(idempKey);                // 删除防重Key，允许别人继续试
            throw new BusinessException("报名人数已满，请留意后续名额");
        }

        // ================= 3. 第三道防线：丢入线程池异步落库 =================
        threadPool.execute(() -> {
            try {
                // 构建入库实体
                Registration reg = new Registration();
                BeanUtils.copyProperties(dto, reg);
                reg.setUserId(userId);
                reg.setCompetitionId(compId);
                reg.setStatus(1); // 1: 待审核
                reg.setAttachment(dto.getAttachmentUrl());

                // TODO: 实际环境需调用 UserFeign 获取姓名学号快照
                reg.setStudentName("测试姓名");
                reg.setStudentId("测试学号");

                // 执行落库 (DB IO 操作)
                this.save(reg);

                // 成功：将结果写入 Redis，供前端轮询查询
                redisTemplate.opsForValue().set(resKey, "SUCCESS", 1, TimeUnit.HOURS);
                log.info("异步落库成功: compId={}, userId={}", compId, userId);

            } catch (DuplicateKeyException e) {
                // 捕获唯一索引冲突 (兜底防止数据库重复)
                log.warn("检测到重复报名: compId={}, userId={}", compId, userId);
                compensateRollback(capKey, idempKey, resKey, "您已报名过该竞赛");
            } catch (Exception e) {
                // 发生未知异常 (如数据库宕机)
                log.error("异步落库失败: compId={}, userId={}", compId, userId, e);
                // 执行补偿与回滚！
                compensateRollback(capKey, idempKey, resKey, "数据库繁忙，请重试");
            }
        });
    }

    /**
     * 异常情况下的补偿回滚方法
     */
    private void compensateRollback(String capKey, String idempKey, String resKey, String errorMsg) {
        // 1. 回滚名额
        redisTemplate.opsForValue().increment(capKey);
        // 2. 清除防重标识，允许用户稍后重试
        redisTemplate.delete(idempKey);
        // 3. 写入失败原因，通知前端
        redisTemplate.opsForValue().set(resKey, "FAIL:" + errorMsg, 1, TimeUnit.HOURS);
    }

    /**
     * 前端轮询查询报名结果
     */
    @Override
    public RegistrationStatusVO getApplyStatus(Long userId, Long compId) {
        String idempKey = RedisKeyConstants.IDEMPOTENT_KEY_PREFIX + compId + ":" + userId;
        String resKey = RedisKeyConstants.RESULT_KEY_PREFIX + compId + ":" + userId;

        RegistrationStatusVO vo = new RegistrationStatusVO();

        // 1. 优先查结果 Key
        String result = redisTemplate.opsForValue().get(resKey);
        if ("SUCCESS".equals(result)) {
            vo.setStatus(1);
            vo.setMessage("报名成功！即将跳转...");
            return vo;
        } else if (result != null && result.startsWith("FAIL:")) {
            vo.setStatus(-1);
            vo.setMessage("报名失败：" + result.split(":")[1]); // 截取具体的失败原因
            return vo;
        }

        // 2. 如果结果不存在，查防重 Key 是否存在
        Boolean isProcessing = redisTemplate.hasKey(idempKey);
        if (Boolean.TRUE.equals(isProcessing)) {
            vo.setStatus(0);
            vo.setMessage("系统正在飞速处理中，请稍候...");
            return vo;
        }

        // 3. 两者都不存在
        vo.setStatus(-1);
        vo.setMessage("未查询到报名请求，请重新报名");
        return vo;
    }
}
