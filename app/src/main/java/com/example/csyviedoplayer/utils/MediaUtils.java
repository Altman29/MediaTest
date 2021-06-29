package com.example.csyviedoplayer.utils;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.csyviedoplayer.R;

import java.util.HashMap;

/**
 * Copyright©  2021
 * 正岸健康
 * author: csy
 * created on: 6/29/21 2:15 PM
 * description:
 */
public class MediaUtils {
    //网络
    public static void createVideoThumbnail(ImageView imageView, String url, Context context) {
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(context)
                .setDefaultRequestOptions(
                        new RequestOptions()
                                .frame(0)
                                .centerCrop()
                                .error(R.mipmap.placeholder)//可以忽略
                        // .placeholder(R.mipmap.xxx2)//可以忽略
                )
                .load(url)
                .into(imageView);
    }

    public static int getRingDuring(String mUri) {
        Integer duration = null;
        android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
        try {
            if (mUri != null) {
                HashMap<String, String> headers = null;
                if (headers == null) {
                    headers = new HashMap<String, String>();
                    headers.put("User-Agent", "Mozilla/5.0 (Linux; U; Android 4.4.2; zh-CN; MW-KW-001 Build/JRO03C) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 UCBrowser/1.0.0.001 U4/0.8.0 Mobile Safari/533.1");
                }
                mmr.setDataSource(mUri, headers);
            }

            duration = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000;
        } catch (Exception ex) {
        } finally {
            mmr.release();
        }
        return duration;
    }
}