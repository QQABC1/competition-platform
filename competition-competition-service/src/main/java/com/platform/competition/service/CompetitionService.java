package com.platform.competition.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.competition.dto.CompetitionPublishDTO;
import com.platform.competition.entity.Competition;

public interface CompetitionService extends IService<Competition> {
    // 发布竞赛
    void publish(Long userId, CompetitionPublishDTO dto);
}