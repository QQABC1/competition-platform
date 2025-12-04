package com.platform.competition.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.competition.dto.CompetitionAuditDTO;
import com.platform.competition.dto.CompetitionPublishDTO;
import com.platform.competition.dto.CompetitionQueryDTO;
import com.platform.competition.entity.Competition;
import com.platform.competition.vo.CompetitionAuditVO;
import com.platform.competition.vo.CompetitionListVO;

public interface CompetitionService extends IService<Competition> {
    // 发布竞赛
    void publish(Long userId, CompetitionPublishDTO dto);
    // 分页查询待审核列表
    IPage<CompetitionAuditVO> getPendingAuditList(Page<Competition> pageParam);

    /**
     * 审核竞赛
     * @param dto 审核参数
     */
    void auditCompetition(CompetitionAuditDTO dto);

    IPage<CompetitionListVO> getClientList(CompetitionQueryDTO queryDTO);
}
