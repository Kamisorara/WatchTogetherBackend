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

    /**
     * 上传图片并返回可访问URL
     */
    public String uploadImg(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String objectName = UUID.randomUUID() + "_" + originalFilename;

        // 使用MinioUtil上传文件
        minioUtil.upload(objectName, file.getInputStream(), file.getSize(), file.getContentType());

        // 生成预签名URL，一个月有效期
        return minioUtil.getObjectUrl(objectName, 60 * 60 * 24 * 30);
    }

    public String uploadVideo(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传的视频文件为空");
        }

        String originalFilename = file.getOriginalFilename();
        // 在对象名前添加videos/前缀便于管理
        String objectName = "videos/" + UUID.randomUUID() + "_" + (originalFilename != null ? originalFilename : "unknown.mp4");

        System.out.println("视频文件信息 - 名称: " + originalFilename
                + ", 大小: " + file.getSize() / (1024 * 1024) + "MB"
                + ", 类型: " + file.getContentType());

        // 上传视频并验证
        String url = minioUtil.uploadLargeFile(objectName, file.getInputStream(), file.getSize(), file.getContentType());

        // 验证URL是否有效
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("无法生成有效的视频访问URL");
        }

        return url;
    }
}