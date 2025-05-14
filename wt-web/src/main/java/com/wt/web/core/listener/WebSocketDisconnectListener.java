package com.wt.web.core.listener;

import com.wt.service.wt.RoomService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket断开连接监听器
 */
@Component
@Slf4j
public class WebSocketDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {
    @Resource
    private RoomService roomService;
    @Resource
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Map<String, String> userInfo = WebSocketConnectListener.getSessionInfo(sessionId);
        if (userInfo != null) {
            String userId = userInfo.get("userId");
            String roomCode = userInfo.get("roomCode");

            if (!StringUtils.isEmpty(userId) && !StringUtils.isEmpty(roomCode)) {
                // 检查断开连接的用户是否是房主
                boolean isOwner = roomService.isRoomOwner(roomCode, userId);

                // 移除用户
                roomService.removeUserFromRoom(roomCode, userId);

                // 广播用户变动
                messagingTemplate.convertAndSend("/topic/room/" + roomCode, Map.of("type", "USER_CHANGE"));
                log.info("id:{}用户离开房间", userId);

                // 如果是房主断开连接并且房间还有其他用户
                if (isOwner && !roomService.isEmptyRoom(roomCode)) {
                    // 选择新房主
                    String newOwner = roomService.selectNewRoomOwner(roomCode);

                    if (newOwner != null) {
                        log.info("房间 {} 的房主 {} 断开连接，选择新房主: {}", roomCode, userId, newOwner);

                        // 通知所有用户房主已更换
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("type", "OWNER_CHANGED");
                        notification.put("newOwnerId", newOwner);
                        messagingTemplate.convertAndSend("/topic/room-status/" + roomCode, notification);

                        // 单独通知新房主
                        Map<String, Object> ownerStatus = new HashMap<>();
                        ownerStatus.put("isCreator", true);
                        messagingTemplate.convertAndSendToUser(newOwner, "/queue/room-status/" + roomCode, ownerStatus);
                    }
                }

                // 房间没人则从redis中移除房间
                if (roomService.isEmptyRoom(roomCode)) {
                    roomService.removeRoom(roomCode);
                    log.info("room:{}房间删除", roomCode);
                }
            }
        }
    }
}