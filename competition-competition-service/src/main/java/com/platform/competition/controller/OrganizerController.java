package com.platform.competition.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.common.api.R;
import com.platform.common.exception.BusinessException;
import com.platform.competition.dto.CompetitionCreateDTO;
import com.platform.competition.dto.CompetitionPublishDTO;
import com.platform.competition.entity.Competition;
import com.platform.competition.service.CompetitionService;
import com.platform.competition.vo.CompetitionListVO;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/organizer")
@Tag(name = "B端-竞赛管理", description = "提供竞赛发布、管理相关接口")
public class OrganizerController {

    @Autowired
    private CompetitionService competitionService;


    @PostMapping("/create")
    @ApiOperation(value = "发布新竞赛")
    public R<Void> create(@RequestBody @Validated CompetitionCreateDTO dto,
                          HttpServletRequest request) {

        // 从 Header 获取当前登录用户ID (网关透传)
        Long userId = getUserIdFromHeader(request);

        competitionService.createCompetition(userId, dto);

        return R.ok();
    }

    @GetMapping("/list")
    @ApiOperation(value = "获取我发布的竞赛列表")
    @Parameters({
            @Parameter(name = "page", description = "页码", example = "1"),
            @Parameter(name = "size", description = "每页条数", example = "10"),
            @Parameter(name = "status", description = "状态(0待审核/1已发布/2已驳回)", example = "1"),
            @Parameter(name = "keyword", description = "搜索关键词")
    })
    public R<IPage<CompetitionListVO>> getMyList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {

        // 获取当前用户ID
        Long userId = getUserIdFromHeader(request);

        // 构造分页对象
        Page<Competition> pageParam = new Page<>(page, size);

        // 调用 Service
        IPage<CompetitionListVO> result = competitionService.getMyCompetitionList(pageParam, userId, status, keyword);

        return R.ok(result);
    }
    /** TODO上线后面需要修改
     * 提取公共方法：从Header获取用户ID
     */
    private Long getUserIdFromHeader(HttpServletRequest request) {
        String userIdStr = request.getHeader("Service-User-Id");
        // 开发测试阶段，如果没走网关，可以给个默认值 (例如 1L) 方便 Swagger 调试
        // 生产环境建议抛出 BusinessException("未登录")
        if (!StringUtils.hasText(userIdStr)) {
            // throw new BusinessException(401, "无法获取用户信息，请通过网关访问");
            return 1L; // 仅供测试使用
        }
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new BusinessException("非法用户ID");
        }
    }
}