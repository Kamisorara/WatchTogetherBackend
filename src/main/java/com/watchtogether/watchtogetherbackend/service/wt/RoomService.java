package com.watchtogether.watchtogetherbackend.service.wt;

import java.util.Set;

public interface RoomService {

    // 创建房间
    String createRoom(String userId);

    // 加入房间
    Boolean addUserToRoom(String roomCode, String userId);

    // 检查对应房间是否存在
    Boolean roomExists(String roomCode);

    // 获取对应房间中的用户Id
    Set<String> getUserInRoom(String roomCode);

    // 用户断开时移除房间
    Boolean removeUserFromRoom(String roomCode, String userId);
}
