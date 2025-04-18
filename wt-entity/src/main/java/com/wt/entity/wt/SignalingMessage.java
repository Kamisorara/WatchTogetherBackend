package com.wt.entity.wt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 信令消息封装
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignalingMessage {
    private String type;        // 消息类型: "join-room", "leave-room", "offer", "answer", "ice-candidate"
    private String senderId;    // 发送者ID
    private String targetUserId; // 接收者ID（可选）
    private Object sdp;         // 会话描述（用于offer/answer）
    private Object candidate;   // ICE候选（用于网络连接）
    private String userId;      // 用户ID（用于加入/离开房间）

}
