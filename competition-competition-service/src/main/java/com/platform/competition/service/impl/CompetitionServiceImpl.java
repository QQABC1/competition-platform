package com.platform.competition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.competition.dto.CompetitionAuditDTO;
import com.platform.competition.dto.CompetitionPublishDTO;
import com.platform.competition.entity.Competition;
import com.platform.competition.entity.Organizer;
import com.platform.competition.mapper.CompetitionMapper;
import com.platform.competition.mapper.OrganizerMapper;
import com.platform.competition.service.CompetitionService;
import com.platform.competition.vo.CompetitionAuditVO;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CompetitionServiceImpl extends ServiceImpl<CompetitionMapper, Competition> implements CompetitionService {
    @Autowired
    OrganizerMapper organizerMapper;

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
}