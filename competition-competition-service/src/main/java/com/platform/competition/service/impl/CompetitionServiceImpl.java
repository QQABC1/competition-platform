package com.platform.competition.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.common.exception.BusinessException;
import com.platform.competition.dto.CompetitionPublishDTO;
import com.platform.competition.entity.Competition;
import com.platform.competition.mapper.CompetitionMapper;
import com.platform.competition.service.CompetitionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class CompetitionServiceImpl extends ServiceImpl<CompetitionMapper, Competition> implements CompetitionService {

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
}