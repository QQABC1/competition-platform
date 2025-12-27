package com.platform.competition.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.competition.dto.CompetitionAuditDTO;
import com.platform.competition.dto.CompetitionCreateDTO;
import com.platform.competition.dto.CompetitionPublishDTO;
import com.platform.competition.dto.CompetitionQueryDTO;
import com.platform.competition.entity.Competition;
import com.platform.competition.vo.CompetitionAuditVO;
import com.platform.competition.vo.CompetitionDetailVO;
import com.platform.competition.vo.CompetitionCListVO;
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

    /**
     * 获取赛事列表首页广场
     * @param queryDTO
     * @return
     */
    IPage<CompetitionCListVO> getClientList(CompetitionQueryDTO queryDTO);

    /**
     * 展示竞赛详情
     * @param id
     * @return
     */
    CompetitionDetailVO getCompetitionDetail(Long id);

    /**
     * B端-创建竞赛
     * @param userId 当前登录用户ID (发布人)
     * @param dto 表单数据
     */
    void createCompetition(Long userId, CompetitionCreateDTO dto);

    /**
     * B端-查询我发布的竞赛列表
     * @param pageParam 分页参数
     * @param userId 当前登录用户ID
     * @param status 状态筛选 (可选)
     * @param keyword 关键词搜索 (可选)
     * @return 分页VO
     */
    IPage<CompetitionListVO> getMyCompetitionList(Page<Competition> pageParam, Long userId, Integer status, String keyword);
}
