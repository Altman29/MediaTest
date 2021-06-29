package com.example.csyviedoplayer.audio;

/**
 * Copyright (c) 2021
 * 正岸健康
 * author: whs
 * created on: 2021/4/13 16:28
 * description: 枚举
 */
public enum AudioPlayEnum {
    /**播放空闲*/
    PLAYER_FREE,
    /**预缓冲，准备播放中*/
    PLAYER_PREPARING,
    /**播放中*/
    PLAYER_PLAYING,
    /**播放完毕*/
    PLAYER_COMPLETE,
    /**播放暂停*/
    PLAYER_PAUSE,
    /**播放错误*/
    PLAYER_ERROR
}
