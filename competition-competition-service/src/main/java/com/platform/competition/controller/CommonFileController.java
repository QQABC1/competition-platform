package com.platform.competition.controller;

import com.platform.common.api.R;
import com.platform.competition.utils.MinioUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@Api(tags = "通用资源")
public class CommonFileController {

    @Autowired
    private MinioUtils minioUtils;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @ApiOperation(value = "上传文件")
    public R<String> upload(
            @Parameter(description = "文件对象", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "业务类型(cover/attachment)", example = "cover")
            @RequestParam(value = "bizType", defaultValue = "temp") String bizType
    ) {
        // 调用工具类上传
        String url = minioUtils.uploadFile(file, bizType);
        return R.ok(url);
    }
}