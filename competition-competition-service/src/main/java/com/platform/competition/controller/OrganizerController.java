package com.platform.competition.controller;

import com.platform.common.api.R;
import com.platform.competition.dto.CompetitionPublishDTO;
import com.platform.competition.service.CompetitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/organizer")
@Tag(name = "B端-竞赛管理", description = "提供竞赛发布、管理相关接口")
public class OrganizerController {

    @Autowired
    private CompetitionService competitionService;

    @PostMapping("/publish")
    @Operation(summary = "发布竞赛", description = "提交后状态为待审核")
    public R<Void> publish(@RequestBody @Validated CompetitionPublishDTO dto,
                           HttpServletRequest request) {

        // 1. 从 Gateway 传递的 Header 中获取当前登录用户ID
        String userIdStr = request.getHeader("Service-User-Id");
        if (userIdStr == null) {
            // 如果本地调试没走网关，给个默认值方便测试，生产环境应抛异常
            // throw new BusinessException(401, "未获取到用户信息");
            userIdStr = "1";
        }
        Long userId = Long.parseLong(userIdStr);

        // 2. 调用Service
        competitionService.publish(userId, dto);

        return R.ok();
    }
}