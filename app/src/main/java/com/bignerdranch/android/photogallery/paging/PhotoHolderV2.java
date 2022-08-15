package com.bignerdranch.android.photogallery.paging;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bignerdranch.android.photogallery.GalleryItem;
import com.bignerdranch.android.photogallery.PhotoGalleryFragment;
import com.bignerdranch.android.photogallery.PhotoPageActivity;
import com.bignerdranch.android.photogallery.R;
import com.bumptech.glide.Glide;

public class PhotoHolderV2 extends RecyclerView.ViewHolder {

    private ImageView mItemView;
    private final Context mContext;
    private GalleryItem mGalleryItem;

    public PhotoHolderV2(@NonNull View layout, Context context) {
        super(layout);
        mItemView = layout.findViewById(R.id.item_image_view);
        mContext = context;

        mItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = PhotoPageActivity.newIntent(mContext, mGalleryItem.getPhotoPageUri());
                mContext.startActivity(intent);

            }
        });
    }

    public void bind(GalleryItem galleryItem) {
        mGalleryItem = galleryItem;

        useGlideToLoadImage(this, galleryItem);
        // TODO: 2022/7/22
    }
    private void useGlideToLoadImage(@NonNull PhotoHolderV2 holder, GalleryItem galleryItem) {
        Glide.with(mContext)
                .load(galleryItem.getUrl())
                .placeholder(R.drawable.bill_up_close)
                .into(holder.getItemView());
    }

    public ImageView getItemView() {
        return mItemView;
    }
}
