package com.watchtogether.watchtogetherbackend.entity.wt;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 音频数据封装
 */
@Data
@Getter
@Setter
public class AudioMessage {
    private String audioData; // Keep as String
    private String senderId;

    public AudioMessage() {
    }

    public AudioMessage(String audioData, String senderId) {
        this.audioData = audioData;
        this.senderId = senderId;
    }
}