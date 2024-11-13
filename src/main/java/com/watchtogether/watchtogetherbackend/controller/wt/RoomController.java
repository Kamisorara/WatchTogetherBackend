package com.watchtogether.watchtogetherbackend.controller.wt;

import com.watchtogether.watchtogetherbackend.entity.response.RestBean;
import com.watchtogether.watchtogetherbackend.entity.wt.VideoControlMessage;
import com.watchtogether.watchtogetherbackend.service.sys.UserService;
import com.watchtogether.watchtogetherbackend.service.wt.RoomService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/room")
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
            return RestBean.success(userId + "加入" + roomCode + "房间");
        }
    }

    /**
     * 视频控制
     */
    @MessageMapping("/video-control/{roomCode}")
    @SendTo("/topic/video-sync/{roomCode}")
    public VideoControlMessage handleVideoControl(VideoControlMessage message) throws Exception {
//        String userId = userService.getUserIdFromToken(request).toString();
//        if (!roomService.getUserInRoom(roomCode).contains(userId)) {
//            throw new Exception(userId + "没有授权");
//        }
        return message;
    }

}
