package com.wt.entity.wt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket视频选择消息
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieSelectMessage {
    private Long movieId;
    private String title;
    private String videoUrl;
}