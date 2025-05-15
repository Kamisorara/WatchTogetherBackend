package com.wt.service.minio;

import com.wt.common.utils.MinioUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * MinIO对象存储服务类
 * 提供图片和视频文件上传功能，并生成可访问的URL
 * 封装MinioUtil操作，支持不同类型文件的存储和管理
 */
@Service
public class MinioService {

    @Resource
    private MinioUtil minioUtil;

    /**
     * 上传图片并返回可访问URL
     * 为上传的图片生成UUID作为唯一标识，并保存到MinIO存储桶中
     *
     * @param file 要上传的图片文件，MultipartFile格式
     * @return 生成的带有时效性的预签名访问URL，有效期为6天
     */
    public String uploadImg(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String objectName = UUID.randomUUID() + "_" + originalFilename;

        // 使用MinioUtil上传文件
        minioUtil.upload(objectName, file.getInputStream(), file.getSize(), file.getContentType());

        // 生成预签名URL，一个月有效期
        return minioUtil.getObjectUrl(objectName, 60 * 60 * 24 * 6);
    }

    /**
     * 上传视频文件并返回可访问URL
     * 支持大文件上传，视频文件存储在指定的videos/目录下
     *
     * @param file 要上传的视频文件，MultipartFile格式
     * @return 生成的视频文件访问URL
     * @throws IllegalArgumentException 当上传的视频文件为空时抛出
     * @throws RuntimeException         当无法生成有效的视频访问URL时抛出
     */
    public String uploadVideo(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传的视频文件为空");
        }

        String originalFilename = file.getOriginalFilename();
        // 在对象名前添加videos/前缀便于管理
        String objectName = "videos/" + UUID.randomUUID() + "_" + (originalFilename != null ? originalFilename : "unknown.mp4");

//        System.out.println("视频文件信息 - 名称: " + originalFilename
//                + ", 大小: " + file.getSize() / (1024 * 1024) + "MB"
//                + ", 类型: " + file.getContentType());

        // 上传视频并验证
        String url = minioUtil.uploadLargeFile(objectName, file.getInputStream(), file.getSize(), file.getContentType());

        // 验证URL是否有效
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("无法生成有效的视频访问URL");
        }

        return url;
    }
}