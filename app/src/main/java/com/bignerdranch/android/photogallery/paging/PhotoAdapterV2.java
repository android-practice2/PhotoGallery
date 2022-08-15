package com.bignerdranch.android.photogallery.paging;

import static autodispose2.AutoDispose.autoDisposable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;

import com.bignerdranch.android.photogallery.GalleryItem;
import com.bignerdranch.android.photogallery.R;

public class PhotoAdapterV2 extends PagingDataAdapter<GalleryItem, PhotoHolderV2> {
    // Define Loading ViewType
    public static final int LOADING_ITEM = 0;
    // Define Movie ViewType
    public static final int MOVIE_ITEM = 1;


    public PhotoAdapterV2(@NonNull DiffUtil.ItemCallback<GalleryItem> diffCallback) {
        super(diffCallback);

    }

    @NonNull
    @Override
    public PhotoHolderV2 onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_gallery, parent, false);
        return new PhotoHolderV2(inflate, parent.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoHolderV2 holder, int position) {
        GalleryItem item = getItem(position);
        holder.bind(item);
    }

//    @Override
//    public int getItemViewType(int position) {
//        if (position == getItemCount()) {//fix: not robust. do not support prepend
//            return  LOADING_ITEM;
//        }else {
//            return MOVIE_ITEM;
//        }
//    }
}
