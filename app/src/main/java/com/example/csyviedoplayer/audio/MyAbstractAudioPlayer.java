package com.example.csyviedoplayer.audio;

import java.util.List;

/**
 * Copyright (c) 2021
 * 正岸健康
 * author: whs
 * created on: 2021/4/13 16:29
 * description: 抽象类
 */
public abstract class MyAbstractAudioPlayer {
    AudioPlayerEvent mAudioPlayerEvent;
    /**
     * 暂停之后调用，接着上一次播放
     */
    public abstract void reStart();
    /**
     * 带资源播放，播放当前资源
     * @param path path
     */
    public abstract void start(String path);

    /**
     * 将整个列表添加到播放列表，如果是未播放状态，则播放第一个
     * @param pathList 资源列表
     */
    public abstract void start(List<String> pathList);

    /**
     * 更改播放列表
     */
    public abstract void modifyPlayList(List<String> pathList);

    /**
     * 播放事件监听器
     * @param event 时间
     */
    public abstract void setAudioPlayerListener(AudioPlayerEvent event);

    /**
     * 暂停
     */
    public abstract void pause(boolean noAutoPause);

    /**
     * 暂停缓存
     * @param path 路径
     */
    public abstract void stopCacheAndShutdown(String path);

    /**
     * 缓存下一首
     */
    public abstract void cacheNext();

    /**
     * 下一首
     */
    public abstract void nextPlay();

    /**
     * 上一首
     */
    public abstract void prevPlay();

    /**
     * 是否正在播放
     * @return boolean
     */
    public abstract boolean isPlaying();

    /**
     * 当前播放状态
     * @return AudioPlayEnum
     */
    public abstract AudioPlayEnum getPlayerStatus();

    /**
     * 调整进度
     * @param time 时间
     */
    public abstract void seekTo(long time);

    /**
     * 拖动进度条，通知（防止拖动时Timmer跑进度条）
     */
    public abstract void seekStart();

    /**
     * 释放播放器
     */
    public abstract void release();

    /**
     * 获取当前播放的位置
     * @return 获取当前播放的位置
     */
    public abstract long getCurrentPosition();

    /**
     * 获取当前播放列表位置
     * @return 获取当前播放列表位置
     */
    public abstract int getCurrentPlayPosition();

    /**
     * 获取视频总时长
     * @return long
     */
    public abstract long getDuration();

    /**
     * 获取缓冲百分比
     * @return int
     */
    public abstract int getBufferedPercentage();
    /**
     * 设置列表是否循环播放
     * @param isLooping 循环
     */
    public abstract void setListLooping(boolean isLooping);

    /**
     * 设置定时
     * @param time
     */
    public abstract void setCountDownTime(long time);


    /**
     * 设置是否单曲循环
     * @param isLooping 循环
     */
    public abstract void setSingleLooping(boolean isLooping);
}
