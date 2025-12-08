package com.platform.competition.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.common.api.R;
import com.platform.competition.dto.CompetitionQueryDTO;
import com.platform.competition.service.CompetitionService;
import com.platform.competition.vo.CompetitionDetailVO;
import com.platform.competition.vo.CompetitionListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/c/competition")
@Api(tags = "C端-竞赛广场")
public class ClientCompetitionController {

    @Autowired
    private CompetitionService competitionService;

    @PostMapping("/list")
    @ApiOperation(value = "获取竞赛列表(分页+筛选)")
    public R<IPage<CompetitionListVO>> getClientList(@RequestBody CompetitionQueryDTO queryDTO) {

        IPage<CompetitionListVO> result = competitionService.getClientList(queryDTO);

        return R.ok(result);
    }
    @GetMapping("/detail/{id}")
    @ApiOperation(value = "获取竞赛详情")
    public R<CompetitionDetailVO> getDetail(
            @Parameter(description = "竞赛ID", required = true) @PathVariable Long id) {

        CompetitionDetailVO vo = competitionService.getCompetitionDetail(id);

        return R.ok(vo);
    }
}