package com.wt.service.wt;

import com.wt.entity.wt.WtMovies;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 电影Service接口
 * 提供电影资源的查询、上传和获取功能
 * 管理系统中的电影资源数据
 */
public interface MovieService {

    /**
     * 获取电影列表
     * 查询系统中所有可用的电影资源
     *
     * @return 电影对象列表
     */
    List<WtMovies> getMovieList();

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
    WtMovies uploadMovie(MultipartFile file, String title, String description, Long uploaderId) throws Exception;

    /**
     * 根据ID获取电影详情
     * 查询指定ID的电影资源完整信息
     *
     * @param movieId 电影资源ID
     * @return 电影对象，如不存在则可能返回null
     */
    WtMovies getMovieById(Long movieId);
}
