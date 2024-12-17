package com.watchtogether.watchtogetherbackend.service.wt;

import com.watchtogether.watchtogetherbackend.entity.response.UserInfoResp;

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
}
