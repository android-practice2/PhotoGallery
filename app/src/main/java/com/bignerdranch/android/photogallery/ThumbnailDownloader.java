package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private boolean mHasQuit = false;
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int PRELOAD = 1;
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ConcurrentHashMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    private PreloadListener mPreloadListener;

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        this.mResponseHandler = responseHandler;

    }

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail, String url);
    }
    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> thumbnailDownloadListener) {
        mThumbnailDownloadListener = thumbnailDownloadListener;
    }

    public interface PreloadListener{
        void onPreloaded(String url, Bitmap bitmap);
    }

    public void setPreloadListener(PreloadListener preloadListener) {
        mPreloadListener = preloadListener;
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {

                if (MESSAGE_DOWNLOAD == msg.what) {
                    T obj = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(obj));
                    handleRequest(obj);
                } else if (PRELOAD == msg.what) {
                    String url=(String) msg.obj;
                    Log.i(TAG, "Preload a image for URL: " + url);
                    byte[] urlBytes = new byte[0];
                    try {
                        urlBytes = new FlickrFetchr().getUrlBytes(url);
                    } catch (IOException e) {
                        Log.e(TAG, "Error downloading image", e);
                    }
                    Bitmap bitmap = BitmapFactory.decodeByteArray(urlBytes, 0, urlBytes.length);
                    mPreloadListener.onPreloaded(url, bitmap);

                }

            }
        };
    }

    private void handleRequest(T target) {
        String url = mRequestMap.get(target);
        if (url == null) {
            return;
        }

        try {
            byte[] urlBytes = new FlickrFetchr().getUrlBytes(url);
            Bitmap bitmap = BitmapFactory.decodeByteArray(urlBytes, 0, urlBytes.length);
            mResponseHandler.post(new Runnable() {//main thread will execute this runnable
                @Override
                public void run() {
                    if (!url.equals(mRequestMap.get(target)) || mHasQuit) {
                        return;
                    }

                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap, url);

                }
            });

            Log.i(TAG, "Bitmap created");
        } catch (IOException e) {
            Log.e(TAG, "Error downloading image", e);
        }

    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();

    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);
        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            Message message = mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target);
            message.sendToTarget();
        }

    }

    public void preLoad(String url) {
        mRequestHandler.obtainMessage(PRELOAD, url).sendToTarget();

    }
}
