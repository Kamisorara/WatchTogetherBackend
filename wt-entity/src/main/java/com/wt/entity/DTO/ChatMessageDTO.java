package com.wt.entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息数据传输DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private String id;           // 消息唯一ID
    private String userId;       // 发送用户ID
    private String userName;     // 发送用户名称
    private String userAvatar;   // 发送用户头像URL
    private String content;      // 消息内容
    private String timestamp;    // 消息时间戳字符串
}