package com.watchtogether.watchtogetherbackend.listener;

import com.watchtogether.watchtogetherbackend.service.sys.UserService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
@Slf4j
public class WebSocketConnectListener implements ApplicationListener<SessionConnectedEvent> {
    @Resource
    private UserService userService;
    @Resource
    private SimpMessagingTemplate messagingTemplate;

    // 使用一个静态Map来存储sessionId与userId和roomCode的关联
    private static final Map<String, Map<String, String>> sessionInfoMap = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
//        System.out.println(headerAccessor);
        // 获取原始连接消息的 headers
        Message<?> connectMessage = (Message<?>) headerAccessor.getHeader("simpConnectMessage");

        // 获取Session id 和 请求头中的token roomCode等信息
        String sessionId = headerAccessor.getSessionId();

        if (connectMessage != null) {
            Map<String, Object> connectHeaders = connectMessage.getHeaders();
            Map<String, List<String>> nativeHeaders = (Map<String, List<String>>) connectHeaders.get("nativeHeaders");
            if (nativeHeaders != null && nativeHeaders.containsKey("token")) {
                String token = nativeHeaders.get("token").get(0);
                String roomCode = nativeHeaders.get("roomCode").get(0);
//                System.out.println(token);
//                System.out.println(roomCode);
                try {
                    String userId = userService.getUserIdFromToken(token).toString();
                    if (!StringUtils.isEmpty(userId) && !StringUtils.isEmpty(roomCode)) {
                        Map<String, String> userInfo = new ConcurrentHashMap<>();
                        userInfo.put("userId", userId);
                        userInfo.put("roomCode", roomCode);
                        sessionInfoMap.put(sessionId, userInfo);

                        // 广播用户变动
                        messagingTemplate.convertAndSend("/topic/room/" + roomCode, Map.of("type", "USER_CHANGE"));

                        log.info("id:{}用户加入房间{}", userId, roomCode);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("加入房间时发生错误");
                }
            }
        }
    }

    // 提供一个静态方法给WebSocketDisconnectListener访问Session信息
    public static Map<String, String> getSessionInfo(String sessionId) {
        return sessionInfoMap.get(sessionId);
    }
}
