package com.bignerdranch.android.photogallery.paging;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.paging.LoadState;
import androidx.paging.LoadStateAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bignerdranch.android.photogallery.R;
import com.bignerdranch.android.photogallery.databinding.LoadStateItemBinding;

public class PhotoLoadStateAdapter extends LoadStateAdapter<PhotoLoadStateAdapter.PhotoViewHolder> {
    private final View.OnClickListener mRetryCallback;

    public PhotoLoadStateAdapter(View.OnClickListener mRetryCallback) {
        this.mRetryCallback = mRetryCallback;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, @NonNull LoadState loadState) {

        return new PhotoViewHolder(viewGroup, mRetryCallback);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder photoViewHolder, @NonNull LoadState loadState) {

        photoViewHolder.bind(loadState);
    }

    protected static class PhotoViewHolder extends RecyclerView.ViewHolder {

        private final com.bignerdranch.android.photogallery.databinding.LoadStateItemBinding mBinding;

        public PhotoViewHolder(@NonNull ViewGroup viewGroup,
                               View.OnClickListener listener
        ) {
            super(LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.load_state_item, viewGroup, false)
            );
            mBinding = LoadStateItemBinding.bind(super.itemView);

            mBinding.retryButton.setOnClickListener(listener);

        }

        public void bind(LoadState loadState) {

            mBinding.progressBar.setVisibility(loadState instanceof LoadState.Loading ? View.VISIBLE : View.GONE);
            mBinding.retryButton.setVisibility(loadState instanceof LoadState.Error ? View.VISIBLE : View.GONE);
            mBinding.errorMsg.setVisibility(loadState instanceof LoadState.Error ? View.VISIBLE : View.GONE);

            if (loadState instanceof LoadState.Error) {
                mBinding.errorMsg.setText(((LoadState.Error) loadState).getError().getLocalizedMessage());

            }


        }
    }


}
