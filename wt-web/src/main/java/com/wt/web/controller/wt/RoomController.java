package com.wt.web.controller.wt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wt.common.annotaion.RateLimit;
import com.wt.entity.resp.RestBean;
import com.wt.entity.system.SysUser;
import com.wt.entity.wt.*;
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
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 房间内操作相关Controller
 */
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
     * 返回系统中所有可用的电影信息列表
     *
     * @return 包含电影列表的RestBean响应，成功时包含电影列表数据，失败时包含错误信息
     */
    @GetMapping("/get-movie-list")
    @RateLimit(limit = 5, message = "访问过于频繁")
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
     * 上传电影文件
     * 接收用户上传的电影文件并保存到系统中，记录相关元数据
     * 仅限已登录用户使用，且有访问频率限制
     *
     * @param file        上传的电影文件，MultipartFile格式
     * @param title       电影标题，不能为空
     * @param description 电影描述信息
     * @param request     HTTP请求对象，用于获取当前用户信息
     * @return 包含上传结果的RestBean响应，成功时包含电影信息，失败时包含错误信息
     */
    @PostMapping("/movie-upload")
    @RateLimit(key = "#request.getHeader('Authorization') + ':' + #request.getRequestURI()", limit = 5, message = "访问过于频繁")
    public RestBean uploadMovie(@RequestParam("file") MultipartFile file,
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
     * 创建观影房间
     * 为当前用户创建一个新的观影房间，并将用户设置为房主
     * 有访问频率限制，防止恶意调用
     *
     * @param request HTTP请求对象，用于获取当前用户信息
     * @return 包含房间代码的RestBean响应，成功时返回新创建房间的唯一代码
     * @throws Exception 创建房间过程中可能抛出的异常
     */
    @PostMapping("/create")
    @RateLimit(key = "#request.getHeader('Authorization') + ':' + #request.getRequestURI()", limit = 5, message = "访问过于频繁")
    public RestBean createRoom(HttpServletRequest request) throws Exception {
        String userId = userService.getUserIdFromServerletRequest(request).toString();
        String roomCode = roomService.createRoom(userId);

        // 创建房间时设置房主
        roomService.setRoomOwner(roomCode, userId);

        log.info("id:{}用户创建{}房间并成为房主", userId, roomCode);
        return RestBean.success(roomCode);
    }

    /**
     * 加入观影房间
     * 支持JSON格式和表单参数两种提交方式，用于 兼容屎山前端
     * 检查房间是否存在，将用户添加到指定房间，并返回房间当前状态
     *
     * @param request    HTTP请求对象，用于获取当前用户信息
     * @param requestMap JSON请求体中的参数映射，包含roomCode参数
     * @return 包含加入结果的RestBean响应，成功时包含房间状态信息，失败时包含错误信息
     */
    @PostMapping("/join")
    @RateLimit(limit = 5, message = "访问过于频繁")
    public RestBean joinRoom(HttpServletRequest request,
                             @RequestBody(required = false) Map<String, String> requestMap) throws Exception {
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
            boolean isOwner = roomService.isRoomOwner(roomCode, userId.toString());

            log.info("id:{}用户加入房间{}", userId, roomCode);

            // 获取当前房间电影状态
            MovieSelectMessage currentMovie = roomService.getRoomMovieState(roomCode);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("message", userId + "加入" + roomCode + "房间");
            responseMap.put("currentMovie", currentMovie); // 即使是null也不会有问题
            responseMap.put("isCreator", isOwner);

            return RestBean.success(responseMap);
        }
    }

    /**
     * 获取房间房主ID
     * 根据房间代码查询并返回房间创建者的用户ID
     *
     * @param request    HTTP请求对象
     * @param requestMap JSON请求体中的参数映射，包含roomCode参数
     * @return 包含房主ID的RestBean响应，成功时返回房主用户ID，失败时包含错误信息
     */
    @GetMapping("/room-owner")
    @RateLimit(limit = 5, message = "访问过于频繁")
    public RestBean getRoomOwner(HttpServletRequest request,
                                 @RequestBody(required = false) Map<String, String> requestMap) {
        if (requestMap != null) {
            String roomCode = requestMap.get("roomCode");
            if (!roomService.roomExists(roomCode)) {
                return RestBean.error(400, "房间不存在");
            }
            String ownerId = roomService.getRoomOwner(roomCode);
            return RestBean.success(ownerId);
        }
        return RestBean.error(400, "房间号不能为空");
    }

    /**
     * 获取房间内所有用户信息
     * 返回指定房间中所有在线用户的详细信息
     *
     * @param request HTTP请求对象，包含roomCode参数
     * @return 包含房间用户列表的RestBean响应，成功时返回用户详细信息列表，失败时包含错误信息
     */
    @GetMapping("/get-room-user")
    @RateLimit(limit = 5, message = "访问过于频繁")
    public RestBean getRoomUser(HttpServletRequest request) throws Exception {
        String roomCode = request.getParameter("roomCode");
        if (!roomService.roomExists(roomCode)) {
            return RestBean.error(400, "房间不存在，请输入正确的房间号");
        } else {
            Long personalId = userService.getUserIdFromServerletRequest(request);
            Set<String> userIdInRoom = roomService.getUserIdInRoom(roomCode);
            List<SysUser> userDetailsInRoom = roomService.getUserDetailsInRoom(userIdInRoom, String.valueOf(personalId));
            return RestBean.success(userDetailsInRoom);
        }
    }

    /**
     * 视频控制message处理
     * WebSocket端点，处理房间内视频播放控制消息（如播放、暂停、跳转等）
     * 接收客户端发送的控制指令并广播给房间内所有用户
     *
     * @param message 视频控制消息对象，包含控制类型和参数
     * @return 广播给房间内所有用户的视频控制消息
     */
    @MessageMapping("/video-control/{roomCode}")
    @SendTo("/topic/video-sync/{roomCode}")
    public VideoControlMessage handleVideoControl(@Payload VideoControlMessage message) throws Exception {
        return message;
    }

    /**
     * 处理电影选择message
     * WebSocket端点，处理房间内电影选择变更
     * 接收房主选择的电影信息并广播给房间内所有用户，同时保存房间当前电影状态
     *
     * @param roomCode 房间代码，用于标识接收消息的房间
     * @param message  电影选择消息对象，包含电影ID、标题和视频URL
     * @return 广播给房间内所有用户的电影选择消息
     */
    @MessageMapping("/movie-select/{roomCode}")
    @SendTo("/topic/movie-select/{roomCode}")
    public MovieSelectMessage handleMovieSelection(@DestinationVariable("roomCode") String roomCode,
                                                   @Payload MovieSelectMessage message) {
        log.info("房间 {} 选择了电影: {}", roomCode, message.getTitle());

        if (message.getMovieId() != null) {
            try {
                WtMovies movie = movieService.getMovieById(message.getMovieId());
                if (movie != null && (message.getTitle() == null || message.getVideoUrl() == null)) {
                    message.setTitle(movie.getTitle());
                    message.setVideoUrl(movie.getVideoUrl());
                }
            } catch (Exception e) {
                log.error("获取电影详情失败", e);
            }
        }

        // 保存当前房间的电影状态
        roomService.saveRoomMovieState(roomCode, message);

        return message;
    }

    /**
     * 处理音频数据传输 (后期Electron版本使用)
     * WebSocket端点，处理房间内语音聊天的音频数据
     * 接收Base64编码的音频数据，解码后转发给房间内其他用户
     *
     * @param roomCode 房间代码，用于标识接收消息的房间
     * @param message  包含Base64编码音频数据的JSON字符串
     * @return 解码后的音频消息对象，用于广播给房间内其他用户
     */
    @MessageMapping("/audio/{roomCode}")
    @SendTo("/topic/audio-sync/{roomCode}")
    public AudioMessage handleAudio(@DestinationVariable("roomCode") String roomCode,
                                    String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            AudioMessage audioMessage = objectMapper.readValue(message, AudioMessage.class);

            byte[] decodedAudio = null;
            String base64String = audioMessage.getAudioData();

            if (base64String != null && !base64String.isEmpty()) {
                try {
                    decodedAudio = Base64.decodeBase64(base64String);
                } catch (IllegalArgumentException e) {
                    log.error("base64音频数据发生错误: {}", base64String, e);
                    log.error("Base64音频数据串: {}", StringUtils.newStringUtf8(base64String.getBytes()));
                    return null;
                }
            } else {
                log.warn("音频数据为空: {}", message);
                return null;
            }

            if (decodedAudio != null) {
                String audioString = new String(decodedAudio, StandardCharsets.UTF_8);
                audioMessage.setAudioData(audioString);
                log.info("接收到的音频数据长度: {}", decodedAudio.length);
            } else {
                return null;
            }

            return audioMessage;
        } catch (JsonProcessingException e) {
            log.error("处理音频时发生错误", e);
            return null;
        }
    }

    /**
     * 中继WebRTC信令消息
     * WebSocket端点，处理房间内WebRTC连接建立所需的信令交换
     * 接收客户端发送的信令消息并转发给房间内其他用户
     *
     * @param roomCode 房间代码，用于标识接收消息的房间
     * @param message  信令消息对象，包含信令类型和数据
     * @return 转发给房间内其他用户的信令消息
     */
    @MessageMapping("/rtc-signaling/{roomCode}")
    @SendTo("/topic/rtc-signaling/{roomCode}")
    public SignalingMessage relaySignalingMessage(@DestinationVariable("roomCode") String roomCode,
                                                  @Payload SignalingMessage message) {
        log.info("房间 {} 收到信令: {}", roomCode, message.getType());
        return message;
    }

}
