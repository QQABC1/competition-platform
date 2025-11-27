package com.platform.competition.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.common.api.R;
import com.platform.competition.entity.Category;
import com.platform.competition.entity.Organizer;
import com.platform.competition.service.CategoryService;
import com.platform.competition.service.OrganizerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@Tag(name = "基础字典", description = "提供竞赛分类、主办单位等基础数据查询")
public class DictionaryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrganizerService organizerService;

    @GetMapping("/category/list")
    @Operation(summary = "获取竞赛分类列表", description = "用于发布页下拉框或首页筛选")
    public R<List<Category>> getCategoryList() {
        // 查询状态为1(启用)的分类，按sort排序
        List<Category> list = categoryService.list(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
        );
        return R.ok(list);
    }

    @GetMapping("/organizer/list")
    @Operation(summary = "获取主办单位列表", description = "包含默认联系人和电话，支持前端自动填充")
    public R<List<Organizer>> getOrganizerList() {
        // 查询所有主办单位
        List<Organizer> list = organizerService.list();
        return R.ok(list);
    }
}