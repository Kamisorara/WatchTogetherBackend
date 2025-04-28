package com.wt.service.minio;

import com.wt.common.utils.MinioUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.UUID;

@Service
public class MinioService {

    @Resource
    private MinioUtil minioUtil;

    // 上传图片并返回可访问URL
    public String uploadImg(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String objectName = UUID.randomUUID() + "_" + originalFilename;

        // 使用MinioUtil上传文件
        minioUtil.upload(objectName, file.getInputStream(), file.getSize(), file.getContentType());

        // 生成预签名URL，7天有效期
        return minioUtil.getObjectUrl(objectName, 60 * 60 * 24 * 7);
    }
}