package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.fragment.app.Fragment;

public class PhotoPageActivity  extends  SingleFragmentActivity{

    private PhotoPageFragment mPhotoPageFragment;

    public static Intent newIntent(Context context, Uri uri) {
        Intent intent = new Intent(context, PhotoPageActivity.class);
        intent.setData(uri);
        return intent;

    }

    @Override
    protected Fragment createFragment() {
        Uri uri=getIntent().getData();
        mPhotoPageFragment = PhotoPageFragment.newInstance(uri);
        return mPhotoPageFragment;
    }

    @Override
    public void onBackPressed() {
        if (!mPhotoPageFragment.onBackPressed()) {
            super.onBackPressed();
        }

    }
}
