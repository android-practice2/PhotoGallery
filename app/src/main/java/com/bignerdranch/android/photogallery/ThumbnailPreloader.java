package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.IOException;


public class ThumbnailPreloader extends HandlerThread
{

    private static final String TAG = "ThumbnailPreloader";
    private static final int PRELOAD = 1;

    private Handler mRequestHandler;
//    private PreloadListener mPreloadListener;
    private boolean mQuit;
    private Fragment mFragment;
    private LruCache<String, Drawable> mImageCache;

    public ThumbnailPreloader(Fragment fragment) {
        super(TAG);
        this.mFragment =fragment;

    }

//    public interface PreloadListener{
//        void onPreloaded(String url, Bitmap bitmap);
//    }
//
//    public void setPreloadListener(PreloadListener preloadListener) {
//        mPreloadListener = preloadListener;
//    }

    public void onPreloaded(String url, Bitmap bitmap) {
        if (mFragment. isAdded()) {
            Drawable drawable = new BitmapDrawable(mFragment.getResources(), bitmap);
            mImageCache.put(url, drawable);
        }
    }

    public void setImageCache(LruCache<String, Drawable> imageCache) {
        mImageCache = imageCache;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();

        mRequestHandler=new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (mQuit) {
                    return;
                }

                String url=(String) msg.obj;
                Log.i(TAG, "Preload a image for URL: " + url);
                byte[] urlBytes = new byte[0];
                try {
                    urlBytes = new FlickrFetchr().getUrlBytes(url);
                } catch (IOException e) {
                    Log.e(TAG, "Error downloading image", e);
                }
                Bitmap bitmap = BitmapFactory.decodeByteArray(urlBytes, 0, urlBytes.length);
//                mPreloadListener.onPreloaded(url, bitmap);
                onPreloaded(url, bitmap);

            }
        };
    }

    @Override
    public boolean quit() {
        mQuit = true;

        return super.quit();
    }

    public void preLoad(String url) {
        mRequestHandler.obtainMessage(PRELOAD, url).sendToTarget();

    }
}
