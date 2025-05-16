package com.wt.service.wt.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wt.common.utils.RedisCache;
import com.wt.dao.mapper.UserMapper;
import com.wt.entity.system.SysUser;
import com.wt.entity.wt.MovieSelectMessage;
import com.wt.service.wt.RoomService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 房间服务实现类
 * 基于Redis实现在线观影房间的创建、管理和状态维护
 * 处理用户进出、房间状态更新和房主权限控制
 */
@Service
@Slf4j
public class RoomServiceImpl implements RoomService {

    @Resource
    private RedisCache redisCache;

    @Resource
    private UserMapper userMapper;

    /**
     * Redis中房间成员集合的键名前缀
     */
    private static final String ROOM_PREFIX = "room_";

    /**
     * Redis中房间电影状态的键名前缀
     */
    private static final String ROOM_MOVIE_KEY_PREFIX = "room:movie:";

    /**
     * Redis中房间房主信息的键名前缀
     */
    private static final String ROOM_OWNER_PREFIX = "room:owner:";

    /**
     * 创建新的观影房间
     * 生成唯一6位大写字母房间码，将创建用户添加到房间，并存储到Redis
     *
     * @param userId 创建房间的用户ID
     * @return 生成的6位大写字母房间码
     * @throws RuntimeException 房间创建失败时抛出
     */
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

    /**
     * 将用户添加到指定房间
     * 验证房间存在性并将用户ID添加到Redis中的房间成员集合
     *
     * @param roomCode 房间码
     * @param userId   要添加的用户ID
     * @throws RuntimeException 房间不存在或添加失败时抛出
     */
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

    /**
     * 检查指定房间是否存在
     * 根据房间码查询Redis中是否有对应的房间记录
     *
     * @param roomCode 房间码
     * @return 房间存在返回true，否则返回false
     */
    @Override
    public Boolean roomExists(String roomCode) {
        String roomKey = ROOM_PREFIX + roomCode;
        return !redisCache.keys(roomKey).isEmpty();
    }

    /**
     * 获取指定房间中的所有用户ID集合
     * 从Redis中获取房间所有成员ID
     *
     * @param roomCode 房间码
     * @return 房间内用户ID的Set集合
     */
    @Override
    public Set<String> getUserIdInRoom(String roomCode) {
        String roomKey = ROOM_PREFIX + roomCode;
        return redisCache.getCacheSet(roomKey);
    }

    /**
     * 获取房间内用户详细信息
     * 根据用户ID集合查询用户详情，并排除调用者自己
     *
     * @param userIdSet  用户ID集合
     * @param personalId 当前用户ID，将被排除
     * @return 房间内其他用户的详细信息列表
     */
    @Override
    public List<SysUser> getUserDetailsInRoom(Set<String> userIdSet, String personalId) {
        List<SysUser> result = new ArrayList<>();
        // 排除自己Id
        userIdSet.remove(personalId);
        for (String userId : userIdSet) {
            SysUser userDetailInfo = userMapper.getUserInfoById(Long.valueOf(userId));
            result.add(userDetailInfo);
        }
        return result;
    }

    /**
     * 将用户从房间中移除
     * 当用户断开连接或主动离开时，从房间成员集合中删除
     *
     * @param roomCode 房间码
     * @param userId   需要移除的用户ID
     * @throws RuntimeException 用户不在房间中或移除失败时抛出
     */
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

    /**
     * 判断房间是否为空
     * 检查房间内是否还有用户存在
     *
     * @param roomCode 房间码
     * @return 房间为空返回true，否则返回false
     */
    @Override
    public Boolean isEmptyRoom(String roomCode) {
        String roomKey = ROOM_PREFIX + roomCode;
        return redisCache.getCacheSet(roomKey).isEmpty();
    }

    /**
     * 删除空房间
     * 清除Redis中的房间数据及相关状态信息，包括成员集合、电影状态和房主信息
     *
     * @param roomCode 房间码
     */
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

    /**
     * 保存房间内电影播放状态
     * 将电影播放信息序列化为JSON并存储到Redis
     *
     * @param roomCode   房间码
     * @param movieState 电影播放状态信息对象
     */
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

    /**
     * 获取房间内电影播放状态
     * 从Redis读取并反序列化电影播放信息
     *
     * @param roomCode 房间码
     * @return 电影播放状态信息对象，如不存在则返回null
     */
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

    /**
     * 获取房间房主ID
     * 从Redis中读取指定房间的房主信息
     *
     * @param roomCode 房间码
     * @return 房主用户ID，如不存在则返回null
     */
    @Override
    public String getRoomOwner(String roomCode) {
        String key = ROOM_OWNER_PREFIX + roomCode;
        return redisCache.getCacheObject(key);
    }

    /**
     * 设置房间房主
     * 将指定用户设置为房间控制者并存储到Redis
     *
     * @param roomCode 房间码
     * @param userId   要设置为房主的用户ID
     */
    @Override
    public void setRoomOwner(String roomCode, String userId) {
        String key = ROOM_OWNER_PREFIX + roomCode;
        redisCache.setCacheObject(key, userId);
        log.info("房间 {} 设置房主: {}", roomCode, userId);
    }

    /**
     * 判断用户是否是房间房主
     * 验证指定用户ID与当前房主ID是否一致
     *
     * @param roomCode 房间码
     * @param userId   需要验证的用户ID
     * @return 是房主返回true，否则返回false
     */
    @Override
    public boolean isRoomOwner(String roomCode, String userId) {
        String ownerId = getRoomOwner(roomCode);
        return userId != null && userId.equals(ownerId);
    }

    /**
     * 房主离开时随机选择新房主
     * 从房间剩余用户中随机指定一名用户为新房主
     *
     * @param roomCode 房间码
     * @return 新房主的用户ID，如房间为空则返回null
     */
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
