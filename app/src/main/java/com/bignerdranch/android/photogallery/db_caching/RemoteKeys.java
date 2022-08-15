package com.bignerdranch.android.photogallery.db_caching;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "remote_keys")
public class RemoteKeys{
    public static final RemoteKeys NULL = new RemoteKeys();
    @PrimaryKey
    @NonNull
    private String mId;
    private Integer prevKey;
    private Integer nextKey;

    public RemoteKeys() {
    }

    public RemoteKeys(@NonNull String id, Integer prevKey, Integer nextKey) {
        mId = id;
        this.prevKey = prevKey;
        this.nextKey = nextKey;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    public void setId(@NonNull String id) {
        mId = id;
    }

    public Integer getPrevKey() {
        return prevKey;
    }

    public void setPrevKey(Integer prevKey) {
        this.prevKey = prevKey;
    }

    public Integer getNextKey() {
        return nextKey;
    }

    public void setNextKey(Integer nextKey) {
        this.nextKey = nextKey;
    }

    @Override
    public String toString() {
        return "{" +
                "mId='" + mId + '\'' +
                ", prevKey=" + prevKey +
                ", nextKey=" + nextKey +
                '}';
    }
}