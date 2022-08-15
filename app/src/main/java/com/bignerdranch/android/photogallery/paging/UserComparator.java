package com.bignerdranch.android.photogallery.paging;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.bignerdranch.android.photogallery.GalleryItem;

public class UserComparator extends DiffUtil.ItemCallback<GalleryItem>{
    @Override
    public boolean areItemsTheSame(@NonNull GalleryItem oldItem, @NonNull GalleryItem newItem) {
        return oldItem.getId().equals(newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull GalleryItem oldItem, @NonNull GalleryItem newItem) {
        return oldItem.equals(newItem);

    }
}
