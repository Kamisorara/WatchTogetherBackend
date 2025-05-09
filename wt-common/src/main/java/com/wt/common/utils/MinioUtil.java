package com.wt.common.utils;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

@Component
public class MinioUtil {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucketName;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        try {
            // 自动创建桶
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO 初始化失败", e);
        }
    }

    // 上传文件
    public void upload(String objectName, InputStream stream, long size, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(stream, size, -1)
                        .contentType(contentType)
                        .build()
        );
    }

    // 下载文件
    public InputStream download(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    // 生成预览URL
    public String getObjectUrl(String objectName, int expiresSeconds) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expiresSeconds)
                        .build()
        );
    }

    /**
     * 分块上传大文件
     *
     * @param objectName  存储对象名
     * @param inputStream 文件流
     * @param fileSize    文件大小
     * @param contentType 文件类型
     * @return 访问URL
     */
    public String uploadLargeFile(String objectName, InputStream inputStream, long fileSize, String contentType) throws Exception {
        // 创建临时文件以确保完整读取
        File tempFile = null;
        FileInputStream fis = null;

        try {
            System.out.println("开始上传文件: " + objectName + ", 大小: " + fileSize / (1024 * 1024) + "MB");

            // 创建临时文件
            tempFile = File.createTempFile("video-upload-", ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024 * 1024]; // 1MB 缓冲区
                int bytesRead;
                long totalBytesRead = 0;

                // 将输入流内容写入临时文件
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // 打印上传进度
                    if (totalBytesRead % (5 * 1024 * 1024) == 0) { // 每5MB打印一次
                        System.out.println("已写入临时文件: " + totalBytesRead / (1024 * 1024) + "MB");
                    }
                }

                System.out.println("文件已完全写入临时文件，总大小: " + tempFile.length() / (1024 * 1024) + "MB");
            }

            // 检查存储桶是否存在
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                System.out.println("创建存储桶: " + bucketName);
            }

            // 从临时文件创建新的输入流
            fis = new FileInputStream(tempFile);

            // 执行上传，使用小于文件大小的分块大小
            int partSize = 10 * 1024 * 1024; // 10MB 分块大小

            // 不再保存响应结果，避免空指针问题
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .contentType(contentType)
                            .stream(fis, tempFile.length(), partSize)
                            .build()
            );

            // 上传后验证文件是否存在
            try {
                StatObjectResponse stat = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
                System.out.println("文件验证成功: " + objectName + ", 大小: " + stat.size() + "字节");
            } catch (Exception e) {
                throw new RuntimeException("文件上传后无法验证其存在: " + e.getMessage());
            }

            // 返回访问URL
            return getObjectUrl(objectName, 60 * 60 * 24 * 7); // 7天有效期
        } catch (Exception e) {
            System.err.println("视频上传失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            // 关闭流
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ignored) {
                }
            }

            // 删除临时文件
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    tempFile.deleteOnExit();
                }
            }
        }
    }


}