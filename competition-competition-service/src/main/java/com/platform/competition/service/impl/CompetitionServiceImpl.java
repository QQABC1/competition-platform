package com.platform.competition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.exception.BusinessException;
import com.platform.competition.dto.CompetitionAuditDTO;
import com.platform.competition.dto.CompetitionPublishDTO;
import com.platform.competition.dto.CompetitionQueryDTO;
import com.platform.competition.entity.Category;
import com.platform.competition.entity.Competition;
import com.platform.competition.entity.Organizer;
import com.platform.competition.mapper.CategoryMapper;
import com.platform.competition.mapper.CompetitionMapper;
import com.platform.competition.mapper.OrganizerMapper;
import com.platform.competition.service.CompetitionService;
import com.platform.competition.vo.CompetitionAuditVO;
import com.platform.competition.vo.CompetitionListVO;
import org.checkerframework.checker.units.qual.C;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CompetitionServiceImpl extends ServiceImpl<CompetitionMapper, Competition> implements CompetitionService {
    @Autowired
    OrganizerMapper organizerMapper;

    @Autowired
    private CategoryMapper categoryMapper; // 用于查询分类名称
    @Autowired
    private StringRedisTemplate redisTemplate; // Redis操作
    @Autowired
    private ObjectMapper objectMapper; // Jackson序列化工具

    @Override
    public void publish(Long userId, CompetitionPublishDTO dto) {
        // 1. 业务逻辑校验
        if (dto.getRegEndTime().isBefore(dto.getRegStartTime())) {
            throw new BusinessException("报名结束时间不能早于开始时间");
        }
        if (dto.getCompEndTime().isBefore(dto.getCompStartTime())) {
            throw new BusinessException("比赛结束时间不能早于开始时间");
        }
        if (dto.getCompStartTime().isBefore(dto.getRegEndTime())) {
            throw new BusinessException("比赛开始时间不能早于报名结束时间");
        }

        // 2. DTO 转 Entity
        Competition competition = new Competition();
        BeanUtils.copyProperties(dto, competition);

        // 3. 处理字段映射差异 (DTO字段名 -> Entity字段名)
        // DTO中叫 coverImgUrl, Entity中叫 coverImg
        competition.setCoverImg(dto.getCoverImgUrl());
        // DTO中叫 attachmentUrl, Entity中叫 attachment
        competition.setAttachment(dto.getAttachmentUrl());

        // 4. 填充系统字段
        competition.setPublisherId(userId); // 从网关Header获取的用户ID
        competition.setStatus(0); // 默认状态：0-待审核

        // 5. 保存到数据库
        this.save(competition);
    }

    /**
     * 实现内存组装逻辑：
     * 1. 查待审核列表
     * 2. 提取 organizerId
     * 3. 查 organizer 表
     * 4. 组装 VO
     */
    @Override
    public IPage<CompetitionAuditVO> getPendingAuditList(Page<Competition> pageParam) {
        // 1. 第一步：查询主表 (Status=0, 按创建时间正序)
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Competition::getStatus, 0);
        wrapper.orderByAsc(Competition::getCreateTime);

        Page<Competition> resultPage = this.page(pageParam, wrapper);

        // 如果查不到数据，直接返回空页，避免后续报错
        if (resultPage.getRecords().isEmpty()) {
            Page<CompetitionAuditVO> emptyVoPage = new Page<>();
            BeanUtils.copyProperties(resultPage, emptyVoPage);
            return emptyVoPage;
        }

        // 2. 第二步：提取所有的 organizerId (去重)
        Set<Long> organizerIds = resultPage.getRecords().stream()
                .map(Competition::getOrganizerId)
                .collect(Collectors.toSet());

        // 3. 第三步：批量查询字典表，并转为 Map<Id, Name>
        Map<Long, String> organizerMap;
        if (!organizerIds.isEmpty()) {
            List<Organizer> organizers = organizerMapper.selectBatchIds(organizerIds);
            organizerMap = organizers.stream()
                    .collect(Collectors.toMap(Organizer::getId, Organizer::getName));
        } else {
            organizerMap = Collections.emptyMap();
        }

        // 4. 第四步：数据组装 (Entity -> VO)
        List<CompetitionAuditVO> voList = resultPage.getRecords().stream().map(item -> {
            CompetitionAuditVO vo = new CompetitionAuditVO();
            // 复制基础属性 (id, title, organizerId, createTime等)
            BeanUtils.copyProperties(item, vo);

            // 手动回填主办方名称
            String orgName = organizerMap.getOrDefault(item.getOrganizerId(), "未知单位");
            vo.setOrganizerName(orgName);

            return vo;
        }).collect(Collectors.toList());

        // 5. 构造返回的 Page 对象
        Page<CompetitionAuditVO> voPage = new Page<>();
        // 复制分页元数据 (total, size, current, pages)
        BeanUtils.copyProperties(resultPage, voPage, "records");
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public void auditCompetition(CompetitionAuditDTO dto) {
        //1.通过数据库查询该记录
        Competition competition = this.getById(dto.getId());
        //2.基本校验:是否存在
        if (competition == null) {
            throw new BusinessException("未找到该竞赛信息");
        }
        //3.核心校验:防止重复审核，只有状态为0才可以审核
        if (competition.getStatus() != 0) {
            throw new BusinessException("该竞赛已被审核，请勿重复操作");
        }
        //4.准备更新对象(不需要使用全量更新)
        Competition updateEntity = new Competition();
        updateEntity.setId(competition.getId());
        if (dto.getPass()) {
            //情况A：审核通过
            updateEntity.setStatus(1);
            //将拒绝理由清空，表示通过(以防第一次被拒绝，第二次申请通过后及时清理)
            updateEntity.setRejectReason("");
        } else {
            //情况B: 审核驳回
            //驳回理由必填检查
            if (!StringUtils.hasText(dto.getReason())) {
                throw new BusinessException("驳回操作必须填写理由");
            }
            updateEntity.setStatus(2);
            updateEntity.setRejectReason(dto.getReason());
        }
        //5. 执行数据库更新
        this.updateById(updateEntity);
        // 6. TODO 发送消息通知
        // 这里预留位置，后续结合 Notification 服务发送 RabbitMQ 消息
        // rabbitTemplate.convertAndSend("competition.audit", ...);
        System.out.println(">>> 模拟发送通知: 竞赛ID=" + dto.getId() + " 审核结果=" + dto.getPass());
    }


    @Override
    public IPage<CompetitionListVO> getClientList(CompetitionQueryDTO queryDTO) {
        // 1. 定义缓存 Key
        String cacheKey = "competition:list:home:page1";
        boolean isFirstPageNoFilter = isHomeFirstPage(queryDTO);

        // 2. TODO 尝试查缓存 (仅限无筛选的第一页)
        if (isFirstPageNoFilter) {
            String cacheValue = redisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cacheValue)) {
                try {
                    // 反序列化缓存数据返回 (这里为了简单，缓存整个 Page 对象结构比较麻烦，
                    // 实际建议只缓存 List records，这里演示简化逻辑，假设缓存了 records)
                    // 为保证代码健壮性，这里仅做逻辑示意，下面走数据库查询逻辑
                } catch (Exception e) {
                    // 缓存出错降级查库
                }
            }
        }

        // 3. 构建 MyBatis-Plus 查询条件
        Page<Competition> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<>();
        // 3.1 基础条件：只查已发布(1)
        wrapper.eq(Competition::getStatus, 1);
        // 3.2 动态筛选
        if(StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.like(Competition::getTitle, queryDTO.getKeyword());
        }
        if(queryDTO.getCategoryId() != null) {
            wrapper.eq(Competition::getCategoryId, queryDTO.getCategoryId());
        }
        if(queryDTO.getOrganizerId() != null) {
            wrapper.eq(Competition::getOrganizerId, queryDTO.getOrganizerId());
        }
        // 3.3 时间状态筛选
        LocalDateTime now = LocalDateTime.now();
        if(queryDTO.getTimeStatus() != null) {
            switch (queryDTO.getTimeStatus()) {
                case 1:// 报名中: 开始 <= now <= 结束
                    wrapper.le(Competition::getRegStartTime, now)
                            .gt(Competition::getRegEndTime, now);
                    break;
                case 2:// 进行中: 比赛开始 <= now <= 比赛结束
                    wrapper.le(Competition::getCompStartTime, now)
                            .gt(Competition::getCompEndTime, now);
                case 3:// 已结束: 比赛结束 < now
                    wrapper.lt(Competition::getCompEndTime, now);
                    break;
            }
        }
        // 3.4 排序：置顶优先，其次按创建时间倒序
        wrapper.orderByDesc(Competition::getIsTop)
                .orderByDesc(Competition::getCreateTime);
        // 4. 执行查询
        Page<Competition> resultPage = this.page(pageParam, wrapper);
        // 5. 转换 VO (Category Name 回填 + 状态文本计算)
        // 5.1 批量查分类名称 (简单内存组装)
        List<Category> categories = categoryMapper.selectList(null);
        Map<Long, String> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        List<CompetitionListVO> voList = resultPage.getRecords().stream().map(item -> {
            CompetitionListVO vo = new CompetitionListVO();
            BeanUtils.copyProperties(item, vo);
            // 回填分类名
            vo.setCategoryName(categoryMap.getOrDefault(item.getCategoryId(), "其他"));
            // 转换置顶状态 (TINYINT -> Boolean)
            vo.setIsTop((item.getIsTop() != null && item.getIsTop() == 1) ? 1 : 0);
            // 计算状态文本
            vo.setStatusText(calculateStatusText(item, now));

            return vo;
        }).collect(Collectors.toList());
        // 6. 重新封装 Page
        Page<CompetitionListVO> voPage = new Page<>();
        BeanUtils.copyProperties(resultPage, voPage, "records");
        voPage.setRecords(voList);
        // 7. 写入缓存 (仅限无筛选第一页)
        if (isFirstPageNoFilter) {
            try {
                // TODO缓存 10 分钟
                // redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(voPage), 10, TimeUnit.MINUTES);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return voPage;
    }
    // 辅助方法：判断是否为无筛选的首页
    private boolean isHomeFirstPage(CompetitionQueryDTO dto) {
        return dto.getPage() == 1 &&
                !StringUtils.hasText(dto.getKeyword()) &&
                dto.getCategoryId() == null &&
                dto.getOrganizerId() == null &&
                dto.getTimeStatus() == null;
    }
    // 辅助方法：判断是否为无筛选的首页
    private String calculateStatusText(Competition item, LocalDateTime now) {
        if(now.isBefore(item.getCreateTime())) {
            return "报名中";
        }else if (now.isAfter(item.getCompEndTime())){
            return "已结束";
        }else{
            return "进行中";
        }
    }
}