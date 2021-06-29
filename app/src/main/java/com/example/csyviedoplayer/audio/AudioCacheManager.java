package com.example.csyviedoplayer.audio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.file.Md5FileNameGenerator;

import java.io.File;

/**
 * Copyright (c) 2021
 * 正岸健康
 * author: whs
 * created on: 2021/4/13 16:30
 * description: AndroidVideoCache缓存
 */
public class AudioCacheManager {
    /**
     * 最大缓存容量
     */
    private static final long DEFAULT_MAX_SIZE = 200 * 1024 * 1024;
    /**
     *  最大缓存数量
     */
    private static final int DEFAULT_MAX_FILE_COUNT = 20;
    /**
     * SD卡APP保存文件名
     */
    private static final String SD_SAVE_PATH = "appName";
    @SuppressLint("SdCardPath")
    private static final String APP_SAVE_PATH = "/data/data/";

    private static final String SAVE_AUDIO_PATH = "audio_cache";


    private static AudioCacheManager mInstance;
    private static HttpProxyCacheServer mCacheServer;
    private CacheListener mCacheListener;

    public static AudioCacheManager getInstance(Context context) {
        Context applicationContext = context.getApplicationContext();
        if (null == mInstance) {
            synchronized (AudioCacheManager.class) {
                if (null == mInstance) {
                    mInstance = new AudioCacheManager(applicationContext);
                }
            }
        }
        return mInstance;
    }

    private AudioCacheManager(Context context) {
        File cacheDir = getCacheDirectory(context);
        Md5FileNameGenerator md5FileNameGenerator = new Md5FileNameGenerator();
        HttpProxyCacheServer.Builder builder = new HttpProxyCacheServer.Builder(context)
                .maxCacheFilesCount(DEFAULT_MAX_FILE_COUNT).cacheDirectory(cacheDir).fileNameGenerator(md5FileNameGenerator);
        mCacheServer = builder.build();
    }

    static HttpProxyCacheServer getProxy(Context context) {
        return mCacheServer == null ? (mCacheServer = newProxy(context)) : mCacheServer;
    }

    private static HttpProxyCacheServer newProxy(Context context) {
        return new HttpProxyCacheServer.Builder(context)
                .maxCacheSize(DEFAULT_MAX_SIZE)
                .build();
    }

    private File getCacheDirectory(Context context) {
        File cacheParentDir = getCacheParentDirectory(context);
        File cacheDir = new File(cacheParentDir, SAVE_AUDIO_PATH);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDir;
    }
    private File getCacheParentDirectory(Context context) {
        File appCacheDir = null;
        String externalStorageState;
        try {
            externalStorageState = Environment.getExternalStorageState();
        } catch (NullPointerException e) {
            externalStorageState = "";
        }

        //暂不考虑放到sd卡下
//        if (MEDIA_MOUNTED.equals(externalStorageState)) {
//            appCacheDir = getExternalCacheDir(context);
//        }
        if (appCacheDir == null) {
            appCacheDir = context.getCacheDir();
        }
        if (appCacheDir == null) {
            String cacheDirPath = APP_SAVE_PATH
                    + context.getPackageName()
                    + File.separator+SAVE_AUDIO_PATH+File.separator;
            appCacheDir = new File(cacheDirPath);
        }
        return appCacheDir;
    }

    private static File getExternalCacheDir(Context context) {
        String pathPrix = Environment.getExternalStorageDirectory() + "/";
        File file = new File(pathPrix + SD_SAVE_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }


    String getProxyUrl(String url) {
        return mCacheServer.getProxyUrl(url);
    }

    void registerCacheListener(String url, CacheListener listener) {
        mCacheListener = listener;
        mCacheServer.registerCacheListener(listener, url);
    }

    void unregisterCacheListener() {
        if (mCacheListener != null){
            mCacheServer.unregisterCacheListener(mCacheListener);
        }

    }

    void release(){
        mInstance = null;
    }
}
