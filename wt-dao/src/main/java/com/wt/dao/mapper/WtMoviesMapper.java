package com.wt.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wt.entity.wt.WtMovies;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WtMoviesMapper extends BaseMapper<WtMovies> {
    // 获取电影列表
    List<WtMovies> getMovieList();

    // 根据上传者ID获取电影列表
    List<WtMovies> getMovieListByUploader(@Param("uploaderId") Long uploaderId);

    // 根据ID获取电影
    WtMovies selectMovieById(@Param("id") Long id);
}
