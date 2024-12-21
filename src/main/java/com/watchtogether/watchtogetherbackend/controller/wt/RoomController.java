package com.watchtogether.watchtogetherbackend.controller.wt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.watchtogether.watchtogetherbackend.entity.response.RestBean;
import com.watchtogether.watchtogetherbackend.entity.response.UserInfoResp;
import com.watchtogether.watchtogetherbackend.entity.wt.AudioMessage;
import com.watchtogether.watchtogetherbackend.entity.wt.VideoControlMessage;
import com.watchtogether.watchtogetherbackend.service.sys.UserService;
import com.watchtogether.watchtogetherbackend.service.wt.RoomService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.codec.binary.StringUtils;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/room")
@Slf4j
public class RoomController {
    @Resource
    private RoomService roomService;
    @Resource
    private UserService userService;

    /**
     * 创建房间
     *
     * @return
     */
    @PostMapping("/create")
    public RestBean createRoom(HttpServletRequest request) throws Exception {
        String userId = userService.getUserIdFromServerletRequest(request).toString();
        String roomCode = roomService.createRoom(userId);
        log.info("id:{}用户创建{}房间", userId, roomCode);
        return RestBean.success(roomCode);
    }

    /**
     * 加入房间
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/join")
    public RestBean joinRoom(HttpServletRequest request) throws Exception {
        String roomCode = request.getParameter("roomCode");
        if (!roomService.roomExists(roomCode)) {
            return RestBean.error(400, "房间不存在，请输入正确的房间号");
        } else {
            Long userId = userService.getUserIdFromServerletRequest(request);
            roomService.addUserToRoom(roomCode, userId.toString());
            log.info("id:{}用户加入房间{}", userId, roomCode);
            return RestBean.success(userId + "加入" + roomCode + "房间");
        }
    }

    /**
     * 获取房间内用户
     */
    @GetMapping("/get-room-user")
    public RestBean getRoomUser(HttpServletRequest request) throws Exception {
        String roomCode = request.getParameter("roomCode");
        if (!roomService.roomExists(roomCode)) {
            return RestBean.error(400, "房间不存在，请输入正确的房间号");
        } else {
            Long personalId = userService.getUserIdFromServerletRequest(request);
            Set<String> userIdInRoom = roomService.getUserIdInRoom(roomCode);
            List<UserInfoResp> userDetailsInRoom = roomService.getUserDetailsInRoom(userIdInRoom, String.valueOf(personalId));
            return RestBean.success(userDetailsInRoom);
        }
    }

    /**
     * 视频控制
     */
    @MessageMapping("/video-control/{roomCode}")
    @SendTo("/topic/video-sync/{roomCode}")
    public VideoControlMessage handleVideoControl(VideoControlMessage message) throws Exception {
        return message;
    }

    /**
     * 处理音频数据
     */
    @MessageMapping("/audio/{roomCode}")
    @SendTo("/topic/audio-sync/{roomCode}")
    public AudioMessage handleAudio(@DestinationVariable String roomCode, String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            AudioMessage audioMessage = objectMapper.readValue(message, AudioMessage.class);

            byte[] decodedAudio = null;
            String base64String = audioMessage.getAudioData();

            if (base64String != null && !base64String.isEmpty()) {
                try {
                    decodedAudio = Base64.decodeBase64(base64String);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid Base64 string: {}", base64String, e);
                    log.error("Base64 String bytes: {}", StringUtils.newStringUtf8(base64String.getBytes()));
                    return null;
                }
            } else {
                log.warn("audioData is null or empty for message: {}", message);
                return null;
            }

            if (decodedAudio != null) {
                String audioString = new String(decodedAudio, StandardCharsets.UTF_8);
                audioMessage.setAudioData(audioString);
                log.info("Received audio data length: {}", decodedAudio.length);
            } else {
                return null;
            }

            return audioMessage;
        } catch (JsonProcessingException e) {
            log.error("Error processing audio message", e);
            return null;
        }
    }

}
