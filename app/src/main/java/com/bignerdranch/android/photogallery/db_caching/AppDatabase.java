package com.bignerdranch.android.photogallery.db_caching;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.bignerdranch.android.photogallery.GalleryItem;

@Database(entities ={GalleryItem.class,RemoteKeys.class} ,version = 2,exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract GalleryItemDAO mGalleryItemDAO();

    public abstract RemoteKeysDAO mRemoteKeysDAO();


}
