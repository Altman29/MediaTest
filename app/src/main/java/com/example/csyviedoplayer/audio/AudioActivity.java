package com.example.csyviedoplayer.audio;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.csyviedoplayer.R;

public class AudioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);


        playAudio();
    }

    private void playAudio() {
        String audioUrl = "https://media.asleephealth.com/yinpin/06.mp3";

        MyAudioManager myAudioManager = new MyAudioManager(AudioActivity.this);

        myAudioManager.start(audioUrl);
        myAudioManager.setAudioPlayerListener(new AudioPlayerEvent() {
            @Override
            public void onBufferingUpdate(int percent) {

            }

            @Override
            public void onBuffering(boolean isBuffering) {

            }

            @Override
            public void onPlayProgress(long duration, long currPosition) {

            }

            @Override
            public void onClockTime(long duration) {

            }

            @Override
            public void onStatusChange(AudioPlayEnum mStatus, int currPlayPotion) {

            }
        });
    }
}