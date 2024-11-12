package com.watchtogether.watchtogetherbackend.service.wt.impl;

import com.watchtogether.watchtogetherbackend.service.wt.RoomService;
import com.watchtogether.watchtogetherbackend.utils.RedisCache;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class RoomServiceImpl implements RoomService {

    @Resource
    private RedisCache redisCache;

    private static final String ROOM_PREFIX = "room_";

    @Override
    public String createRoom(String userId) {
        try {

            String roomCode = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6).toUpperCase();
            // 取前六位uuid作为roomKey
            String roomKey = ROOM_PREFIX + roomCode;
            Set<String> set = new HashSet<>();
            set.add(userId);
            redisCache.setCacheSet(roomKey, set);
            return roomCode;
        } catch (Exception e) {
            throw new RuntimeException("房间创建失败");
        }
    }

    @Override
    public Boolean addUserToRoom(String roomCode, String userId) {
        try {
            String roomKey = ROOM_PREFIX + roomCode;
            redisCache.getCacheSet(roomKey).add(userId);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(userId + "加入房间失败");
        }
    }

    @Override
    public Boolean roomExists(String roomCode) {
        String roomKey = ROOM_PREFIX + roomCode;
        return !redisCache.keys(roomKey).isEmpty();
    }

    @Override
    public Set<String> getUserInRoom(String roomCode) {
        String roomKey = ROOM_PREFIX + roomCode;
        return redisCache.getCacheSet(roomKey);
    }

    @Override
    public Boolean removeUserFromRoom(String roomCode, String userId) {
        try {
            String roomKey = ROOM_PREFIX + roomCode;
            redisCache.getCacheSet(roomKey).remove(userId);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(userId + "离开房间失败");
        }
    }
}
