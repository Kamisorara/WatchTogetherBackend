package com.watchtogether.watchtogetherbackend.service.wt.impl;

import com.watchtogether.watchtogetherbackend.entity.response.UserInfoResp;
import com.watchtogether.watchtogetherbackend.mapper.sys.UserMapper;
import com.watchtogether.watchtogetherbackend.service.wt.RoomService;
import com.watchtogether.watchtogetherbackend.utils.RedisCache;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoomServiceImpl implements RoomService {

    @Resource
    private RedisCache redisCache;
    @Resource
    private UserMapper userMapper;

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
    public void addUserToRoom(String roomCode, String userId) {
        try {
            String roomKey = ROOM_PREFIX + roomCode;
            Set<String> userSet = redisCache.getCacheSet(roomKey);
            if (userSet != null) {
//                System.out.println(userId);
                userSet.add(userId);
                redisCache.setCacheSet(roomKey, userSet);
            } else {
                throw new RuntimeException("房间不存在或数据不完整");
            }
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
    public Set<String> getUserIdInRoom(String roomCode) {
        String roomKey = ROOM_PREFIX + roomCode;
        return redisCache.getCacheSet(roomKey);
    }

    @Override
    public List<UserInfoResp> getUserDetailsInRoom(Set<String> userIdSet, String personalId) {
        List<UserInfoResp> result = new ArrayList<>();
        // 排除自己Id
        userIdSet.remove(personalId);
        for (String userId : userIdSet) {
            UserInfoResp userDetailInfo = userMapper.getUserInfoById(Long.valueOf(userId));
            result.add(userDetailInfo);
        }
        return result;
    }


    @Override
    public void removeUserFromRoom(String roomCode, String userId) {
        try {
            String roomKey = ROOM_PREFIX + roomCode;
            // 获取绑定的 Redis Set 操作
            BoundSetOperations<String, String> setOps = redisCache.redisTemplate.boundSetOps(roomKey);
            if (setOps.isMember(userId)) {
                // 删除删除用户
                setOps.remove(userId);
            } else {
                throw new RuntimeException(userId + "不在房间中");
            }
        } catch (Exception e) {
            throw new RuntimeException(userId + "离开房间失败", e);
        }
    }

    @Override
    public Boolean isEmptyRoom(String roomCode) {
        String roomKey = ROOM_PREFIX + roomCode;
        return redisCache.getCacheSet(roomKey).isEmpty();
    }

    @Override
    public void removeRoom(String roomCode) {
        try {
            if (isEmptyRoom(roomCode)) {
                redisCache.deleteObject(roomCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("无法删除房间");
        }
    }
}
