package com.platform.competition.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.api.R;
import com.platform.competition.entity.Competition;
import com.platform.competition.service.CompetitionService;
import com.platform.competition.vo.CompetitionAuditVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@Api(tags = "审核端-竞赛管理")
public class AdminController {

    @Autowired
    private CompetitionService competitionService;

    @GetMapping("/audit/list")
    @ApiOperation(value = "待审核列表")
    @Parameters({
            @Parameter(name = "page", description = "页码", example = "1"),
            @Parameter(name = "size", description = "每页条数", example = "10")
    })
    public R<IPage<CompetitionAuditVO>> getAuditList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        // 构造 MyBatis-Plus 分页对象
        Page<Competition> pageParam = new Page<>(page, size);

        // 调用 Service
        IPage<CompetitionAuditVO> result = competitionService.getPendingAuditList(pageParam);

        return R.ok(result);
    }
}