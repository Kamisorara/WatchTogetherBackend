package com.watchtogether.watchtogetherbackend.entity.wt;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class VideoControlMessage {
    // Getter 和 Setter 方法
    private String action; // 控制动作，例如 "play" 或 "pause"
    private double time;   // 视频的当前时间（秒）

    // 无参构造函数（用于序列化/反序列化）
    public VideoControlMessage() {
    }

    // 带参构造函数，用于创建消息对象
    public VideoControlMessage(String action, double time) {
        this.action = action;
        this.time = time;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTime(double time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "VideoControlMessage{" +
                "action='" + action + '\'' +
                ", time=" + time +
                '}';
    }
}
