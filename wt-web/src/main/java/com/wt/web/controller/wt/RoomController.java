package com.wt.web.controller.wt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wt.entity.resp.RestBean;
import com.wt.entity.resp.UserInfoResp;
import com.wt.entity.wt.AudioMessage;
import com.wt.entity.wt.SignalingMessage;
import com.wt.entity.wt.VideoControlMessage;
import com.wt.entity.wt.WtMovies;
import com.wt.service.system.UserService;
import com.wt.service.wt.MovieService;
import com.wt.service.wt.RoomService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.codec.binary.StringUtils;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/room")
@Slf4j
public class RoomController {
    @Resource
    private RoomService roomService;
    @Resource
    private UserService userService;
    @Resource
    private MovieService movieService;

    /**
     * 获取电影列表
     */
    @GetMapping("/get-movie-list")
    public RestBean getMovieList() {
        try {
            List<WtMovies> movieList = movieService.getMovieList();
            return RestBean.success(movieList);
        } catch (Exception e) {
            log.error("获取电影列表失败", e);
            return RestBean.error(500, "获取电影列表失败");
        }
    }

    /**
     * 上传电影
     */
    @PostMapping("/movie-upload")
    public RestBean uploadMovie(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            HttpServletRequest request) {
        try {
            // 获取当前用户ID
            Long userId = userService.getUserIdFromServerletRequest(request);
            // 上传电影
            WtMovies movie = movieService.uploadMovie(file, title, description, userId);
            return RestBean.success(movie);
        } catch (Exception e) {
            log.error("上传电影失败", e);
            return RestBean.error(500, "上传电影失败: " + e.getMessage());
        }
    }

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
     * 加入房间 为了兼容屎山
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/join")
    public RestBean joinRoom(HttpServletRequest request, @RequestBody(required = false) Map<String, String> requestMap) throws Exception {
        String roomCode;
        // 优先使用JSON请求中的数据
        if (requestMap != null) {
            roomCode = requestMap.get("roomCode");
        } else {
            // 其次使用请求中的数据
            roomCode = request.getParameter("roomCode");
        }
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

    /**
     * 中继信令消息
     *
     * @param roomCode 房间号
     * @param message  信令消息
     * @return 信令消息
     */
    @MessageMapping("/rtc-signaling/{roomCode}")
    @SendTo("/topic/rtc-signaling/{roomCode}")
    public SignalingMessage relaySignalingMessage(@DestinationVariable("roomCode") String roomCode, // 明确指定参数名
                                                  SignalingMessage message) {
        // 可以在这里记录日志，如记录哪个房间收到了什么类型的信令
        System.out.println("房间 " + roomCode + " 收到信令: " + message.getType());
        return message;
    }


}
