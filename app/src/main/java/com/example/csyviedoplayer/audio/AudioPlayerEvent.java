package com.example.csyviedoplayer.audio;

/**
 * Copyright (c) 2021
 * 正岸健康
 * author: whs
 * created on: 2021/4/13 16:28
 * description: 回调接口
 */
public interface AudioPlayerEvent {
    /**
     * 缓存更新
     * */
    void onBufferingUpdate(int percent);
    /**
     * 是否正在缓冲*/
    void onBuffering(boolean isBuffering);
    /**
     * 播放进度*/
    void onPlayProgress(long duration,long currPosition);

    /**
     * 定时进度
     */
    void onClockTime(long duration);

    /**
     * 播放状态改变
     * @param mStatus 状态
     */
    void onStatusChange(AudioPlayEnum mStatus,int currPlayPotion);
}
