package com.platform.competition.utils;

import com.platform.competition.config.MinioProp;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
public class MinioUtils {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProp minioProp;

    /**
     * 上传文件
     * @param file 前端传来的文件
     * @param bizType 业务类型(作为文件夹名)
     * @return 文件的完整访问URL
     */
    public String uploadFile(MultipartFile file, String bizType) {
        try {
            // 1. 获取文件名后缀 (例如 .jpg)
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 2. 生成唯一文件名 (防止覆盖): uuid.jpg
            String fileName = bizType + "/" + UUID.randomUUID().toString() + suffix;

            // 3. 上传到 MinIO
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProp.getBucketName())
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 4. 拼接返回 URL
            // 格式: http://localhost:9000/competition-public/cover/xxxx.jpg
            return minioProp.getEndpoint() + "/" + minioProp.getBucketName() + "/" + fileName;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }
}