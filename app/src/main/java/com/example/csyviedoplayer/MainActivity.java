package com.example.csyviedoplayer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.csyviedoplayer.audio.AudioActivity;
import com.example.csyviedoplayer.utils.MediaUtils;
import com.example.csyviedoplayer.video.SimplePlayer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        ImageView videoBtn = findViewById(R.id.video_player);
        Button audioBtn = findViewById(R.id.audio_player);

        String source = "https://asleep-1254281736.cos.ap-beijing.myqcloud.com/shipin/shuimianriji2.mp4";

        MediaUtils.createVideoThumbnail(videoBtn, source, MainActivity.this);

        Integer ringDuring = MediaUtils.getRingDuring(source);

        long hour = ringDuring / 3600; //时，取商
        long mm = ringDuring / 60; //分，取商
        long ss = ringDuring % 60; //秒，取余数
        Log.e("TIME", ringDuring + "");
        Log.e("TIME", hour + "时:" + mm + "分:" + ss + "秒");//时长

        videoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SimplePlayer.class));
            }
        });
        audioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AudioActivity.class));

            }
        });
    }
}