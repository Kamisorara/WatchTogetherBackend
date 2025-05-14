package com.wt.service.wt.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wt.common.utils.RedisCache;
import com.wt.dao.mapper.UserMapper;
import com.wt.entity.resp.UserInfoResp;
import com.wt.entity.wt.MovieSelectMessage;
import com.wt.service.wt.RoomService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class RoomServiceImpl implements RoomService {

    @Resource
    private RedisCache redisCache;
    @Resource
    private UserMapper userMapper;

    // 房间前缀
    private static final String ROOM_PREFIX = "room_";

    // 房间电影状态
    private static final String ROOM_MOVIE_KEY_PREFIX = "room:movie:";

    // 房间房主键前缀
    private static final String ROOM_OWNER_PREFIX = "room:owner:";

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
                String roomKey = ROOM_PREFIX + roomCode;
                // 删除房间
                redisCache.deleteObject(roomKey);

                // 删除关联的电影状态
                String movieKey = ROOM_MOVIE_KEY_PREFIX + roomCode;
                redisCache.deleteObject(movieKey);

                // 删除房主信息
                String ownerKey = ROOM_OWNER_PREFIX + roomCode;
                redisCache.deleteObject(ownerKey);

                log.info("房间 {} 及其关联数据已完全删除", roomCode);
            }
        } catch (Exception e) {
            log.error("删除房间数据失败", e);
            throw new RuntimeException("无法删除房间");
        }
    }

    @Override
    public void saveRoomMovieState(String roomCode, MovieSelectMessage movieState) {
        try {
            String key = ROOM_MOVIE_KEY_PREFIX + roomCode;
            // 将电影状态对象转换为 JSON串
            String movieJson = new ObjectMapper().writeValueAsString(movieState);
            redisCache.setCacheObject(key, movieJson);

        } catch (Exception e) {
            log.error("保存房间电影状态失败", e);
        }
    }

    @Override
    public MovieSelectMessage getRoomMovieState(String roomCode) {
        try {
            String key = ROOM_MOVIE_KEY_PREFIX + roomCode;
            String movieJson = redisCache.getCacheObject(key);
            if (movieJson != null) {
                return new ObjectMapper().readValue(movieJson, MovieSelectMessage.class);
            }
        } catch (Exception e) {
            log.error("获取房间电影状态失败", e);
        }
        return null;
    }

    @Override
    public String getRoomOwner(String roomCode) {
        String key = ROOM_OWNER_PREFIX + roomCode;
        return redisCache.getCacheObject(key);
    }

    @Override
    public void setRoomOwner(String roomCode, String userId) {
        String key = ROOM_OWNER_PREFIX + roomCode;
        redisCache.setCacheObject(key, userId);
        log.info("房间 {} 设置房主: {}", roomCode, userId);
    }

    @Override
    public boolean isRoomOwner(String roomCode, String userId) {
        String ownerId = getRoomOwner(roomCode);
        return userId != null && userId.equals(ownerId);
    }

    @Override
    public String selectNewRoomOwner(String roomCode) {
        Set<String> users = getUserIdInRoom(roomCode);
        if (users == null || users.isEmpty()) {
            return null;
        }

        // 随机选择一个用户作为新房主
        String[] userArray = users.toArray(new String[0]);
        String newOwner = userArray[new Random().nextInt(userArray.length)];

        // 设置新房主
        setRoomOwner(roomCode, newOwner);
        log.info("房间 {} 选择新房主: {}", roomCode, newOwner);

        return newOwner;
    }
}
