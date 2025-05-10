package com.wt.service.wt;

import com.wt.entity.wt.WtMovies;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MovieService {
    // 获取电影列表
    List<WtMovies> getMovieList();

    // 上传电影
    WtMovies uploadMovie(MultipartFile file, String title, String description, Long uploaderId) throws Exception;

    // 根据ID获取电影
    WtMovies getMovieById(Long movieId);
}
