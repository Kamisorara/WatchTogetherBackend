package com.watchtogether.watchtogetherbackend.service.fastdfs;

import org.springframework.web.multipart.MultipartFile;

public interface FastDFSService {
    // 长传文件
    String uploadImg(MultipartFile file) throws Exception;
}
