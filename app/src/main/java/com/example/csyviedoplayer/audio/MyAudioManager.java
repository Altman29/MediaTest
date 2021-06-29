package com.example.csyviedoplayer.audio;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Copyright (c) 2021
 * 正岸健康
 * author: whs
 * created on: 2021/4/13 16:31
 * description: 音频管理
 */
public class MyAudioManager extends MyAbstractAudioPlayer {

    /**
     * 定时器检测播放进度时间间隔
     */
    private final int TIMER_PROGRESS_INTERVAL = 500;
    /**
     * 最大连续播放错误数
     */
    private static final int MAX_CONTINUE_ERROR_NUM = 3;

    private IjkMediaPlayer mMediaPlayer;
    private Context mContext;
    private Context appContext;

    private Disposable disposable;
    /**
     * 播放错误次数，连续三次错误，则不进行下一次播放了
     */
    private int mErrorPlayNum = 0;
    /**
     * 播放路径资源存储
     */
    private List<String> mDataSourceList = new ArrayList<>();
    /**
     * 播放位置存储
     */
    private int currPlayPotion = -1;
    /**
     * 是否列表循环播放
     */
    private boolean mListLooping = false;
    /**
     * 是否单曲循环
     */
    private boolean mSingleLooping = false;
    /**
     * 缓存百分比
     */
    private int mBufferedPercent;
    /**
     * 是否正在拖动进度条
     */
    private boolean mSeekTouch = false;
    /**
     * 监听播放器进度Timer以及Task
     */
    private Timer mProgressTimer;
    private TimerTask mProgressTask;
    private final Object mProgressLock = new Object();
    /**
     * 进度线程活动
     */
    private boolean mAudioStateAlive = true;
    /**
     * 音频缓存
     */
    private HttpProxyCacheServer mCacheServer;
    /**
     * 记录上一次播放进度，用于判断是否正在缓冲
     */
    private long prevPlayPosition;
    /**
     * 电话管理者对象
     */
    private TelephonyManager mTelephonyManager;
    /**
     * 电话状态监听者
     */
    private MyPhoneStateListener myPhoneStateListener;
    /**
     * 用于发送是否在缓冲的消息Handler
     */
    private Handler mBufferHandler = new Handler(Looper.getMainLooper());

    /**
     * 当前播放器状态
     * */
    private AudioPlayEnum mPlayerStatus = AudioPlayEnum.PLAYER_FREE;

    /**
     * 手动暂停
     */
    private boolean noAutoPause = true;

    public MyAudioManager(Context context) {
        mContext = context;
        appContext = context.getApplicationContext();
        initPlayer();
    }

    private void initPlayer() {
        //初始化
        mMediaPlayer = new IjkMediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //缓存文件的路径
        //mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_file_path", path);
        //添加监听
        mMediaPlayer.setOnErrorListener(onErrorListener);
        mMediaPlayer.setOnCompletionListener(onCompletionListener);
        mMediaPlayer.setOnInfoListener(onInfoListener);
        mMediaPlayer.setOnBufferingUpdateListener(onBufferingUpdateListener);
        mMediaPlayer.setOnPreparedListener(onPreparedListener);
        //来电监听
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        myPhoneStateListener = new MyPhoneStateListener(this);
        mTelephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * 播放
     */
    private void prepareAsync(int position, String path) {
        try {
            mMediaPlayer.reset();
            sendPlayerStatus(AudioPlayEnum.PLAYER_PREPARING);
            mCacheServer = AudioCacheManager.getProxy(appContext);

            AudioCacheManager.getInstance(appContext).registerCacheListener(path, new CacheListener() {
                @Override
                public void onCacheAvailable(File file, String s, int i) {
                    mBufferedPercent = i;
                    if (mAudioPlayerEvent != null) {
                        mAudioPlayerEvent.onBufferingUpdate(mBufferedPercent);
                    }
                }
            });
            String proxyPath = AudioCacheManager.getInstance(appContext).getProxyUrl(path);
            if (mCacheServer.isCached(path)
                    || proxyPath.startsWith("file://")
                    || proxyPath.startsWith("/storage/emulated/0/")) {
                mBufferedPercent = 100;
                if (mAudioPlayerEvent != null) {
                    sendBufferingHandler(true, false);
                }
                //不要在这里发进度，上层调用不到duration，设置不了max
            } else {
                mBufferedPercent = 0;
                if (mAudioPlayerEvent != null) {
                    sendBufferingHandler(false, true);
                }
            }
            //缓存下一首
            cacheNext();

            mMediaPlayer.setDataSource(proxyPath);
            mMediaPlayer.prepareAsync();
            currPlayPotion = position;
        } catch (Exception e) {
            e.printStackTrace();
            onErrorPlay();
        }
    }

    @Override
    public void reStart() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            sendPlayerStatus(AudioPlayEnum.PLAYER_PLAYING);
            mMediaPlayer.start();
            synchronized (mProgressLock) {
                mProgressLock.notifyAll();
            }
            countDownTime(clockStart,clockTime);
        }
    }

    @Override
    public void start(String path) {
        if(!TextUtils.isEmpty(path)){
            List<String> pathList = new ArrayList<>();
            pathList.add(path);
            start(pathList);
        }
    }

    @Override
    public void start(List<String> pathList) {
        if (pathList == null || pathList.isEmpty()) {
            return;
        }
        mDataSourceList.clear();
        mDataSourceList.addAll(pathList);
        prepareAsync(0, mDataSourceList.get(0));

        countDownTime(clockStart,clockTime);
    }

    @Override
    public void modifyPlayList(List<String> pathList) {
        if (pathList == null || pathList.isEmpty()) {
            return;
        }
        mDataSourceList.clear();
        mDataSourceList.addAll(pathList);
        currPlayPotion = -1;
    }

    @Override
    public void setAudioPlayerListener(AudioPlayerEvent event) {
        this.mAudioPlayerEvent = event;
    }

    @Override
    public void pause(boolean noAutoPause) {
        try {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                sendBufferingHandler(true, false);
                sendPlayerStatus(AudioPlayEnum.PLAYER_PAUSE);
                mMediaPlayer.pause();
                stopCountDown();
                this.noAutoPause = noAutoPause;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopCacheAndShutdown(String path) {
        if (mCacheServer != null && !TextUtils.isEmpty(path)) {
           // mCacheServer.stopCacheAndShutdown(path);
            mCacheServer.shutdown();

        }
    }

    @Override
    public void cacheNext() {
        if (mDataSourceList.isEmpty()) {
            return;
        }
        if (currPlayPotion >= 0) {
            if (mDataSourceList.size() > currPlayPotion + 1) {
                AudioCacheManager.getInstance(appContext).registerCacheListener(mDataSourceList.get(currPlayPotion + 1), (CacheListener) (file, s, i) -> {
                });
            }
        }
    }

    @Override
    public void nextPlay() {
        if (mDataSourceList.isEmpty()) {
            return;
        }
        if (currPlayPotion < 0) {
            prepareAsync(0, mDataSourceList.get(0));
        } else {
            if (mDataSourceList.size() > currPlayPotion + 1) {
                prepareAsync(currPlayPotion + 1, mDataSourceList.get(currPlayPotion + 1));
            } else {
                if (mListLooping) {
                    prepareAsync(0, mDataSourceList.get(0));
                }
            }
        }
    }



    @Override
    public void prevPlay() {
        if (mDataSourceList.isEmpty() || currPlayPotion < 0) {
            return;
        }
        if (currPlayPotion == 0) {
            if (mListLooping) {
                prepareAsync(mDataSourceList.size() - 1, mDataSourceList.get(mDataSourceList.size() - 1));
            }
        } else {
            prepareAsync(currPlayPotion - 1, mDataSourceList.get(currPlayPotion - 1));
        }
    }

    @Override
    public boolean isPlaying() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public AudioPlayEnum getPlayerStatus() {
        return mPlayerStatus;
    }

    @Override
    public void seekTo(long time) {
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.seekTo(time);
                mSeekTouch = false;
                if (mMediaPlayer.isPlaying()) {
                    synchronized (mProgressLock) {
                        mProgressLock.notifyAll();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void seekStart() {
        mSeekTouch = true;
    }

    @Override
    public void release() {
        mTelephonyManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        mBufferHandler.removeCallbacksAndMessages(null);
        stopCountDown();
        destroyTimer();
        if (mCacheServer != null) {
            mCacheServer.shutdown();
            AudioCacheManager.getInstance(appContext).unregisterCacheListener();
            AudioCacheManager.getInstance(appContext).release();
        }
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()){
                mMediaPlayer.stop();
            }
            mAudioStateAlive = false;
            currPlayPotion = -1;
            mMediaPlayer.release();
        }
    }

    @Override
    public long getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public int getCurrentPlayPosition() {
        return currPlayPotion;
    }

    @Override
    public long getDuration() {
        return mMediaPlayer.getDuration();
    }

    @Override
    public int getBufferedPercentage() {
        return mBufferedPercent;
    }

    @Override
    public void setListLooping(boolean isLooping) {
        mListLooping = isLooping;
    }

    @Override
    public void setCountDownTime(long time) {
        clockTime = time;
        clockStart = 0;

        if (mAudioPlayerEvent != null){
            if (time < 0){
                mAudioPlayerEvent.onClockTime(-1);
            }else{
                mAudioPlayerEvent.onClockTime(time * 60 );
            }
        }

        if (!noAutoPause){
            reStart();
        }
        if (mMediaPlayer.isPlaying()){
            countDownTime(0,time);
        }

    }
    private long clockStart;
    private long clockTime;
    /**
     * 定时停止
     */
    private void countDownTime(long start, long endTime){
        stopCountDown();
        if (endTime < 0){
            mAudioPlayerEvent.onClockTime(-1);
            return;
        }
        clockStart = endTime;
        if (endTime <= start/60){
            //重新计时
            endTime = 15;
            clockTime = 15;
            start = 0;
        }
        long finalEndTime = endTime;
        disposable = Observable.intervalRange(start,endTime * 60 + 1,0,1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    mAudioPlayerEvent.onClockTime(finalEndTime * 60 - aLong);
                    clockStart = aLong;
                    if (aLong >= finalEndTime * 60){
                        pause(false);
                    }
                });
    }

    /**
     * 销毁定时器
     */
    private void stopCountDown(){
        if (disposable != null){
            if (!disposable.isDisposed()){
                disposable.dispose();
                disposable = null;
            }
        }
    }

    @Override
    public void setSingleLooping(boolean isLooping) {
        mSingleLooping = isLooping;
    }

    /**
     * 销毁Tmmer
     */
    private void destroyTimer() {
        if (mProgressTimer != null) {
            mProgressTimer.cancel();
            mProgressTimer = null;
        }
    }

    /**
     * 错误时，自动播放下一首
     */
    private void onErrorPlay() {
        sendPlayerStatus(AudioPlayEnum.PLAYER_ERROR);
        destroyTimer();
        if (mErrorPlayNum < MAX_CONTINUE_ERROR_NUM) {
            mErrorPlayNum++;
            if (mSingleLooping) {
                //单曲循环
                prepareAsync(currPlayPotion, mDataSourceList.get(currPlayPotion));
            } else if (mListLooping || mDataSourceList.size() < currPlayPotion + 1) {
                //列表循环
                nextPlay();
            }
        }
    }

    /**
     * 延时发送是否在缓冲的状态，防止假缓冲
     */
    private void sendBufferingHandler(boolean sendNow, final boolean isBuffering) {
        if (mAudioPlayerEvent != null) {
            mAudioPlayerEvent.onBuffering(isBuffering);
        }
        /*mBufferHandler.removeCallbacksAndMessages(null);
        mBufferHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mAudioPlayerEvent != null) {
                    mAudioPlayerEvent.onBuffering(isBuffering);
                }
            }
        }, sendNow ? 0 : (long) (2.5 * TIMER_PROGRESS_INTERVAL));*/
    }

    /**
     * 设置当前播放状态
     */
    private void sendPlayerStatus(AudioPlayEnum mStatus){
        mPlayerStatus = mStatus;
        if(mAudioPlayerEvent != null){
            mAudioPlayerEvent.onStatusChange(mPlayerStatus,currPlayPotion);
        }
    }

    /**
     * 定时器检测播放进度
     */
    private void playProgressListener() {
        if (mProgressTimer == null) {
            mProgressTimer = new Timer();
        }
        if (mProgressTask != null) {
            mProgressTask.cancel();
            mProgressTask = null;
        }
        mProgressTask = new TimerTask() {
            @Override
            public void run() {

                while (mAudioStateAlive) {
                    try {
                        Thread.sleep(TIMER_PROGRESS_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mMediaPlayer == null
                            || !mMediaPlayer.isPlaying()
                            || mSeekTouch) {
                        synchronized (mProgressLock) {
                            try {
                                mProgressLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();

                            }
                        }
                    }
                    if (mAudioPlayerEvent != null
                            && !mSeekTouch
                            && mMediaPlayer.isPlaying()
                            && mMediaPlayer != null) {
                        long currPosition = mMediaPlayer.getCurrentPosition();
                        //播放之前的缓冲在onPrepare时已经缓冲完了，所以这里要排除进度为0
                        if (currPosition != 0 && prevPlayPosition >= currPosition) {
                            sendBufferingHandler(false, true);
                        } else {
                            sendBufferingHandler(true, false);
                        }
                        prevPlayPosition = currPosition;
                        mAudioPlayerEvent.onPlayProgress(mMediaPlayer.getDuration(), currPosition);
                    }
                }
            }
        };
        mProgressTimer. schedule(mProgressTask, 0);
    }

    private IMediaPlayer.OnErrorListener onErrorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer iMediaPlayer, int frameworkErr, int implErr) {
            sendBufferingHandler(true, false);
            onErrorPlay();
            return true;
        }
    };
    private IMediaPlayer.OnInfoListener onInfoListener = new IMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int what, int extra) {
            return true;
        }
    };

    private IMediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int percent) {
        }
    };

    private IMediaPlayer.OnPreparedListener onPreparedListener = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer iMediaPlayer) {
            if (mAudioPlayerEvent != null) {
                synchronized (mProgressLock) {
                    mProgressLock.notifyAll();
                }
                mErrorPlayNum = 0;
                //准备完毕后发送一次缓存（有可能已经缓存完毕）
                mAudioPlayerEvent.onBufferingUpdate(mBufferedPercent);
                //当前正播放，播放器已经预缓存完毕，开始播放了
                sendBufferingHandler(true, false);
                //记录并发送当前播放状态
                sendPlayerStatus(AudioPlayEnum.PLAYER_PLAYING);
                //开始监听播放进度
                playProgressListener();
            }
        }
    };
    private IMediaPlayer.OnCompletionListener onCompletionListener = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer iMediaPlayer) {
            //播放完毕
            if (mAudioPlayerEvent != null) {
                prevPlayPosition = 0;
                sendPlayerStatus(AudioPlayEnum.PLAYER_COMPLETE);
                destroyTimer();
                if (mSingleLooping) {
                    //单曲循环
                    prepareAsync(currPlayPotion, mDataSourceList.get(currPlayPotion));
                } else if (mListLooping || mDataSourceList.size() < currPlayPotion + 1) {
                    //列表循环
                    nextPlay();
                }

            }
        }
    };
}
