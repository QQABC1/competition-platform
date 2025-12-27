package com.platform.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.api.client.RemoteUserService;
import com.platform.api.domain.UserInternalVO;
import com.platform.common.api.R;
import com.platform.common.exception.BusinessException;
import com.platform.registration.dto.RegistrationAuditDTO;
import com.platform.registration.dto.RegistrationDTO;
import com.platform.registration.dto.RegistrationQueryDTO;
import com.platform.registration.entity.Registration;
import com.platform.registration.mapper.RegistrationMapper;
import com.platform.registration.service.RegistrationService;
import com.platform.registration.vo.RegistrationInitVO;
import com.platform.registration.vo.RegistrationListVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
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

        if (dto.getPass()) {
            // 通过 -> 状态 2
            updateEntity.setStatus(2);
            updateEntity.setAuditReason("审核通过");
        } else {
            // 驳回 -> 状态 3
            if (!StringUtils.hasText(dto.getReason())) {
                throw new BusinessException("驳回必须填写理由");
            }
            updateEntity.setStatus(3);
            updateEntity.setAuditReason(dto.getReason());
        }

        this.updateById(updateEntity);

        // TODO: 发送消息通知学生 (MQ)
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
}