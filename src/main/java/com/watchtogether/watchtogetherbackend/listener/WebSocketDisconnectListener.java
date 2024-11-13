package com.watchtogether.watchtogetherbackend.listener;

import com.watchtogether.watchtogetherbackend.service.wt.RoomService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
public class WebSocketDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {
    @Resource
    private RoomService roomService;

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Map<String, String> userInfo = WebSocketConnectListener.getSessionInfo(sessionId);
        if (userInfo != null) {
            String userId = userInfo.get("userId");
            String roomCode = userInfo.get("roomCode");
            // 移除用户
            if (!StringUtils.isEmpty(userId)) {
                if (!StringUtils.isEmpty(roomCode)) {
                    roomService.removeUserFromRoom(roomCode, userId);
                    System.out.println(userId + "已移出房间");
                }
                // 房间没人则从redis中移除房间
                if (roomService.isEmptyRoom(roomCode)) {
                    roomService.removeRoom(roomCode);
                    System.out.println(roomCode + "房间已删除");
                }
            }
        }
    }
}