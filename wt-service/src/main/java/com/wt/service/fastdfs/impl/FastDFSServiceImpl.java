package com.wt.service.fastdfs.impl;

import com.wt.service.fastdfs.FastDFSService;
import com.wt.service.helper.FastDFSClient;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FastDFSServiceImpl implements FastDFSService {
    @Resource
    private FastDFSClient fastDFSClient;

    @Value("${fdfs.web-server-url}")
    private String webUrl;

    @Override
    public String uploadImg(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        // 获取扩展名
        String originalFilename = file.getOriginalFilename();
        // 获取后缀名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        // 获取文件大小
        long size = file.getSize();
        String urlTrail = fastDFSClient.uploadFile(bytes, size, suffix);
        return webUrl + urlTrail;
    }
}
