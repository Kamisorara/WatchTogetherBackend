package com.wt.service.wt;


import com.wt.entity.system.SysUser;
import com.wt.entity.wt.MovieSelectMessage;

import java.util.List;
import java.util.Set;

/**
 * 房间Service
 * 提供在线观影房间的创建、管理和用户交互功能
 * 管理用户进出房间、房间内电影播放状态以及房主权限
 */
public interface RoomService {

    /**
     * 创建新的观影房间
     * 生成唯一房间码并将创建用户添加到房间中
     *
     * @param userId 创建房间的用户ID
     * @return 生成的6位大写字母房间码
     * @throws RuntimeException 房间创建失败时抛出
     */
    String createRoom(String userId);

    /**
     * 将用户添加到指定房间
     * 验证房间存在性并将用户ID添加到房间成员集合中
     *
     * @param roomCode 房间码
     * @param userId   要添加的用户ID
     * @throws RuntimeException 房间不存在或添加失败时抛出
     */
    void addUserToRoom(String roomCode, String userId);

    /**
     * 检查指定房间是否存在
     * 根据房间码查询Redis中是否有对应的房间记录
     *
     * @param roomCode 房间码
     * @return 房间存在返回true，否则返回false
     */
    Boolean roomExists(String roomCode);

    /**
     * 获取指定房间中的所有用户ID集合
     * 从Redis中获取房间所有成员ID
     *
     * @param roomCode 房间码
     * @return 房间内用户ID的Set集合
     */
    Set<String> getUserIdInRoom(String roomCode);

    /**
     * 获取房间内用户详细信息
     * 根据用户ID集合查询用户详情，并排除调用者自己
     *
     * @param userIdSet  用户ID集合
     * @param personalId 当前用户ID，将被排除
     * @return 房间内其他用户的详细信息列表
     */
    List<SysUser> getUserDetailsInRoom(Set<String> userIdSet, String personalId);

    /**
     * 将用户从房间中移除
     * 当用户断开连接或主动离开时调用
     *
     * @param roomCode 房间码
     * @param userId   需要移除的用户ID
     * @throws RuntimeException 用户不在房间中或移除失败时抛出
     */
    void removeUserFromRoom(String roomCode, String userId);

    /**
     * 判断房间是否为空
     * 检查房间内是否还有用户存在
     *
     * @param roomCode 房间码
     * @return 房间为空返回true，否则返回false
     */
    Boolean isEmptyRoom(String roomCode);

    /**
     * 删除空房间
     * 清除Redis中的房间数据及相关状态信息
     *
     * @param roomCode 房间码
     */
    void removeRoom(String roomCode);

    /**
     * 保存房间内电影播放状态
     * 记录当前播放的电影信息、播放进度和状态
     *
     * @param roomCode   房间码
     * @param movieState 电影播放状态信息对象
     */
    void saveRoomMovieState(String roomCode, MovieSelectMessage movieState);

    /**
     * 获取房间内电影播放状态
     * 读取当前房间的电影播放信息
     *
     * @param roomCode 房间码
     * @return 电影播放状态信息对象，如不存在则返回null
     */
    MovieSelectMessage getRoomMovieState(String roomCode);

    /**
     * 获取房间房主ID
     * 查询指定房间的当前房主用户ID
     *
     * @param roomCode 房间码
     * @return 房主用户ID，如不存在则返回null
     */
    String getRoomOwner(String roomCode);

    /**
     * 设置房间房主
     * 为指定房间指派新的房主用户
     *
     * @param roomCode 房间码
     * @param userId   要设置为房主的用户ID
     */
    void setRoomOwner(String roomCode, String userId);

    /**
     * 判断用户是否是房间房主
     * 验证指定用户是否拥有房间控制权限
     *
     * @param roomCode 房间码
     * @param userId   需要验证的用户ID
     * @return 是房主返回true，否则返回false
     */
    boolean isRoomOwner(String roomCode, String userId);

    /**
     * 房主离开时随机选择新房主
     * 从房间剩余用户中随机指定一名用户为新房主
     *
     * @param roomCode 房间码
     * @return 新房主的用户ID，如房间为空则返回null
     */
    String selectNewRoomOwner(String roomCode);
}
