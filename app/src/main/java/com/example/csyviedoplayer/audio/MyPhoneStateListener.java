package com.example.csyviedoplayer.audio;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

/**
 * Copyright (c) 2021
 * 正岸健康
 * author: whs
 * created on: 2021/4/13 16:29
 * description: 电话监听
 */
public class MyPhoneStateListener extends PhoneStateListener {
    private MyAudioManager mMyAudioManager;
    /**记录来电时，是否在播放状态,在电话空闲时恢复*/
    private boolean mAudioPlayingWhenCallRinging = false;

    MyPhoneStateListener(MyAudioManager mMyAudioManager) {
        this.mMyAudioManager = mMyAudioManager;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);
        if (mMyAudioManager == null) {
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if(mMyAudioManager.isPlaying()){
                    mAudioPlayingWhenCallRinging = true;
                    mMyAudioManager.pause(false);
                }else{
                    mAudioPlayingWhenCallRinging = false;
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //打完电话
                if (mAudioPlayingWhenCallRinging){
                    mMyAudioManager.reStart();
                }
                break;
            default:
                break;
        }
    }
}