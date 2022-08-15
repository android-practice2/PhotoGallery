package com.bignerdranch.android.photogallery.db_caching;

import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bignerdranch.android.photogallery.GalleryItem;

import java.util.List;

@Dao
public interface GalleryItemDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE )
    long[] insertAll(List<GalleryItem> items);

    @Query("select * from gallery_item")
    PagingSource<Integer, GalleryItem> pagingSource();

    @Query( "delete from gallery_item")
    int clearAll();
}
