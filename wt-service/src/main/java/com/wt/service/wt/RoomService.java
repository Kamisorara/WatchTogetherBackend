package com.wt.service.wt;


import com.wt.entity.resp.UserInfoResp;
import com.wt.entity.wt.MovieSelectMessage;

import java.util.List;
import java.util.Set;

public interface RoomService {

    // 创建房间
    String createRoom(String userId);

    // 加入房间
    void addUserToRoom(String roomCode, String userId);

    // 检查对应房间是否存在
    Boolean roomExists(String roomCode);

    // 获取对应房间中的用户Id
    Set<String> getUserIdInRoom(String roomCode);

    // 获取房间内用户详情(排除自己)
    List<UserInfoResp> getUserDetailsInRoom(Set<String> userIdSet, String personalId);

    // 用户断开时移除房间
    void removeUserFromRoom(String roomCode, String userId);

    // 判断房间是否为空
    Boolean isEmptyRoom(String roomCode);

    // 删除房间
    void removeRoom(String roomCode);

    // 保存当前房间内电影的状态(电影名称，播放进度，播放状态)
    void saveRoomMovieState(String roomCode, MovieSelectMessage movieState);

    // 获取当前房间内电影的状态
    MovieSelectMessage getRoomMovieState(String roomCode);
}
