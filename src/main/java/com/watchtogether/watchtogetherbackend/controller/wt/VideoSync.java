package com.watchtogether.watchtogetherbackend.controller.wt;


import com.watchtogether.watchtogetherbackend.entity.wt.VideoControlMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class VideoSync {
    @MessageMapping("/video-control") // 处理客户端发送的控制消息
    @SendTo("/topic/video-sync") // 广播消息到所有订阅该频道的客户端
    public VideoControlMessage handleVideoControl(VideoControlMessage message) {
        return message; // 直接返回消息并广播给其他客户端
    }
}
