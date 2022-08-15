package com.bignerdranch.android.photogallery;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "gallery_item")
public class GalleryItem {
    @PrimaryKey
    @NonNull
    private String mId;
    private String mCaption;

    private String mUrl;
    private String mOwner;

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    public Uri getPhotoPageUri() {
        return Uri.parse("https://www.flickr.com/photos/")
                .buildUpon()
                .appendPath(mOwner)
                .appendPath(mId)
                .build();
    }

    @Override
    public String toString() {
        return mCaption;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GalleryItem that = (GalleryItem) o;
        return Objects.equals(mCaption, that.mCaption) && mId.equals(that.mId) && Objects.equals(mUrl, that.mUrl) && Objects.equals(mOwner, that.mOwner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mCaption, mId, mUrl, mOwner);
    }
}