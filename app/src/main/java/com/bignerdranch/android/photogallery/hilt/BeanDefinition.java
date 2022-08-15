package com.bignerdranch.android.photogallery.hilt;

import android.content.Context;

import androidx.room.Room;

import com.bignerdranch.android.photogallery.FlickrFetchr;
import com.bignerdranch.android.photogallery.db_caching.AppDatabase;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.components.FragmentComponent;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn({SingletonComponent.class})
public class BeanDefinition {


    @Provides
    public static AppDatabase sAppDatabase(@ApplicationContext Context context) {
       return Room.databaseBuilder(context, AppDatabase.class, "PhotoGallery.db")
                .fallbackToDestructiveMigration()
                .build()
        ;
    }


    @Provides
    public static FlickrFetchr sFlickrFetchr() {
        return new FlickrFetchr();
    }



}
