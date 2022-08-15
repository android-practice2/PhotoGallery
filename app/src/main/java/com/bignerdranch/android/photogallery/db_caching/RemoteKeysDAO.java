package com.bignerdranch.android.photogallery.db_caching;

import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bignerdranch.android.photogallery.GalleryItem;

import java.util.List;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface RemoteKeysDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE )
    long[] insertAll(List<RemoteKeys> items);

    @Query("select * from remote_keys where mId=:id")
    RemoteKeys findByPK(String id);

    @Query( "delete from remote_keys")
    int clearAll();



}
