package com.watchtogether.watchtogetherbackend.service.fastdfs.impl;

import com.watchtogether.watchtogetherbackend.service.fastdfs.FastDFSService;
import com.watchtogether.watchtogetherbackend.utils.FastDFSClient;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FastDFSServiceImpl implements FastDFSService {
    @Resource
    private FastDFSClient fastDFSClient;

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
        return "http://192.168.11.130:8080/" + urlTrail;
    }
}
