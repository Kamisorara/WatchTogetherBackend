package com.wt.service.wt.impl;

import com.wt.dao.mapper.WtMoviesMapper;
import com.wt.entity.wt.WtMovies;
import com.wt.service.minio.MinioService;
import com.wt.service.wt.MovieService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

/**
 * 电影Service实现类
 * 管理系统中的电影资源数据
 */
@Service
@Slf4j
public class MovieServiceImpl implements MovieService {
    @Resource
    private WtMoviesMapper wtMoviesMapper;

    @Resource
    private MinioService minioService;

    /**
     * 获取电影列表
     * 查询系统中所有可用的电影资源
     *
     * @return 电影对象列表
     */
    @Override
    public List<WtMovies> getMovieList() {
        return wtMoviesMapper.getMovieList();
    }

    /**
     * 上传电影资源
     * 处理电影文件上传并创建相关元数据记录
     *
     * @param file        上传的电影文件
     * @param title       电影标题
     * @param description 电影描述
     * @param uploaderId  上传者用户ID
     * @return 创建的电影对象，包含生成的ID和元数据
     */
    @Override
    public WtMovies uploadMovie(MultipartFile file, String title, String description, Long uploaderId) throws Exception {
        try {
            log.info("开始上传电影视频文件: {}, 大小: {}MB", file.getOriginalFilename(), file.getSize() / (1024 * 1024));

            // 使用专门的视频上传方法
            String videoUrl = minioService.uploadVideo(file);
            log.info("视频上传成功，视频URL: {}", videoUrl);

            // 创建电影记录
            WtMovies movie = new WtMovies();
            movie.setTitle(title);
            movie.setDescription(description);
            movie.setVideoUrl(videoUrl);
            movie.setUploaderId(uploaderId);
            movie.setCreateTime(new Date());
            movie.setMimeType(file.getContentType());

            // 保存到数据库
            wtMoviesMapper.insert(movie);
            log.info("电影信息已保存到数据库，ID: {}", movie.getId());

            return movie;
        } catch (Exception e) {
            log.error("上传电影失败", e);
            throw e;
        }
    }

    /**
     * 根据ID获取电影详情
     * 查询指定ID的电影资源完整信息
     *
     * @param movieId 电影资源ID
     * @return 电影对象，如不存在则可能返回null
     */
    @Override
    public WtMovies getMovieById(Long movieId) {
        return wtMoviesMapper.selectMovieById(movieId);
    }
}
