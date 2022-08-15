package com.bignerdranch.android.photogallery;

import static autodispose2.AutoDispose.autoDisposable;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.ExperimentalPagingApi;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bignerdranch.android.photogallery.databinding.FragmentPhotoGalleryBinding;
import com.bignerdranch.android.photogallery.paging.PhotoAdapterV2;
import com.bignerdranch.android.photogallery.paging.PhotoGalleryViewModel;
import com.bignerdranch.android.photogallery.paging.PhotoLoadStateAdapter;
import com.bignerdranch.android.photogallery.paging.UserComparator;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import autodispose2.androidx.lifecycle.AndroidLifecycleScopeProvider;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
@ExperimentalPagingApi
public class PhotoGalleryFragment extends VisibleFragment {
    public static final boolean MARK_USE_LOCAL_DB_CACHING = true;
    public static final boolean MARK_USE_PAGING3_FRAMEWORK = true;
    public static final boolean MARKER_USE_JOB = false;
    public static final boolean MARK_USE_PRELOAD = false;

    private static final String TAG = "PhotoGalleryFragment";
    public static final int COLUMN_WIDTH_DP = 160;
    public static final int SPAN_COUNT = 3;
    private int page = 0;

    private RecyclerView mPhoto_recycler_view;
    private List<GalleryItem> mItems = new ArrayList<>();
    private PhotoAdapter mAdapter;
    private GridLayoutManager mLayoutManager;

    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private LruCache<String, Drawable> mImageCache;
    private boolean hasFirstPaged;

    private ThumbnailPreloader mThumbnailPreloader;
    private PhotoGalleryViewModel mViewModel;

    public PhotoGalleryFragment() {
        mImageCache = new LruCache<>(100);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        Handler responseHandler = new Handler();

        mThumbnailDownloader = new ThumbnailDownloader<PhotoHolder>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap bitmap, String url) {
                ImageView itemView = target.getItemView();
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                itemView.setImageDrawable(drawable);
                mImageCache.put(url, drawable);
            }
        });


        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();

        if (MARK_USE_PRELOAD) {
            settlePreload();

        }

        Log.i(TAG, "Background thread started");


        fetchItems();
    }

    private void settlePreload() {
        mThumbnailPreloader = new ThumbnailPreloader(this);
        mThumbnailPreloader.setImageCache(mImageCache);

        mThumbnailPreloader.start();
        mThumbnailPreloader.getLooper();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem item = menu.findItem(R.id.menu_item_search);
        SearchView actionView = (SearchView) item.getActionView();
        actionView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.i(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                if (MARK_USE_PAGING3_FRAMEWORK) {
                    invalidatePagingSource();
                } else {
                    reset();
                    fetchItems();
                }

                actionView.onActionViewCollapsed();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.i(TAG, "QueryTextChange: " + newText);
                return false;
            }
        });

        actionView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String storedQuery = QueryPreferences.getStoredQuery(getActivity());
                actionView.setQuery(storedQuery, false);
            }
        });

        MenuItem togglePollingMenu = menu.findItem(R.id.menu_item_toggle_polling);
        boolean serviceAlarmOn = PollService.isServiceAlarmOn(getActivity());
        togglePollingMenu.setTitle(serviceAlarmOn ? getString(R.string.stop_polling)
                : getString(R.string.start_polling));
    }

    private void invalidatePagingSource() {
        String storedQuery = QueryPreferences.getStoredQuery(getActivity());
        mViewModel.setQuery(storedQuery);
        mViewModel.getPagingSource().invalidate();

    }

    private void reset() {
        mItems.clear();
        page = 0;
        hasFirstPaged = false;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                if (MARK_USE_PAGING3_FRAMEWORK) {
                    invalidatePagingSource();

                } else {
                    reset();
                    fetchItems();
                }

                return true;

            case R.id.menu_item_toggle_polling:
                togglePolling();
                getActivity().invalidateOptionsMenu();

                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }


    private void togglePolling() {
        if (MARKER_USE_JOB) {
            boolean serviceAlarmOn = PollServiceV2.isServiceAlarmOn(getActivity());
            PollServiceV2.setServiceAlarm(getActivity(), !serviceAlarmOn);
        } else {
            boolean serviceAlarmOn = PollService.isServiceAlarmOn(getActivity());
            PollService.setServiceAlarm(getActivity(), !serviceAlarmOn);
        }

    }


    private void fetchItems() {
        if (!MARK_USE_PAGING3_FRAMEWORK) {
            String storedQuery = QueryPreferences.getStoredQuery(getActivity());
            if (page == 0) {
                new FetchItemsTask(storedQuery).execute(page++);
            }
            new FetchItemsTask(storedQuery).execute(page++);

        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View layout = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
//        mPhoto_recycler_view = layout.findViewById(R.id.photo_recycler_view);
//        mSyncing_image_view = layout.findViewById(R.id.syncing_image_view);

        FragmentPhotoGalleryBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_photo_gallery, container, false);
        mPhoto_recycler_view = binding.photoRecyclerView;


//        mViewModel = new PhotoGalleryViewModel();
//        binding.setViewModel(mViewModel);
        mViewModel = new ViewModelProvider(this).get(PhotoGalleryViewModel.class);

        mLayoutManager = new GridLayoutManager(getActivity(), SPAN_COUNT);
        mPhoto_recycler_view.setLayoutManager(mLayoutManager);

        if (MARK_USE_PAGING3_FRAMEWORK) {
            initializeRecyclerViewV2();

        } else {
            initializeRecyclerView();

        }

        adaptSpanCount();


        return binding.getRoot();
    }


    private void initializeRecyclerViewV2() {
        PhotoAdapterV2 adapterV2 = new PhotoAdapterV2(new UserComparator());

        mViewModel.getFlowable().to(autoDisposable(AndroidLifecycleScopeProvider.from(this)))
                .subscribe(pagingData -> {
                    adapterV2.submitData(getLifecycle(), pagingData);
                })
        ;


        ConcatAdapter adapter = adapterV2
                .withLoadStateHeaderAndFooter(new PhotoLoadStateAdapter(v -> {
                    adapterV2.retry();
                }), new PhotoLoadStateAdapter(v -> {
                    adapterV2.retry();
                }))
//                .withLoadStateFooter(new PhotoLoadStateAdapter(v -> {
//                    adapterV2.retry();
//                }))
                ;

        mPhoto_recycler_view.setAdapter(adapter
        );


//        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
//            @Override
//            public int getSpanSize(int position) {
//                int itemViewType = adapterV2.getItemViewType(position);
//                if (itemViewType == PhotoAdapterV2.LOADING_ITEM) {
//                    return  SPAN_COUNT;
//                }
//                return 1;
//            }
//        });


    }

    private void initializeRecyclerView() {

        setupAdapter(Collections.emptyList());

        mPhoto_recycler_view.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int childCount = mLayoutManager.getChildCount();
                int itemCount = mLayoutManager.getItemCount();
                int firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition();

                if (dy > 0 && (firstVisibleItemPosition + childCount) >= itemCount) {
                    fetchItems();
                }

            }
        });

    }


    private void adaptSpanCount() {
        ViewTreeObserver viewTreeObserver = mPhoto_recycler_view.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mPhoto_recycler_view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int measuredWidth = mPhoto_recycler_view.getMeasuredWidth();
                int columns = measuredWidth / COLUMN_WIDTH_DP;
                mLayoutManager.setSpanCount(columns);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mThumbnailDownloader != null) {
            mThumbnailDownloader.quit();

        }
        if (mThumbnailPreloader != null) {
            mThumbnailPreloader.quit();
        }

        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    private void setupAdapter(List<GalleryItem> galleryItems) {
        if (isAdded()) {
            doSetupAdapter1(galleryItems);
//            doSetupAdapter2(galleryItems);

        }
    }

    private void doSetupAdapter1(List<GalleryItem> galleryItems) {
        mItems.addAll(galleryItems);
        if (mAdapter == null) {
            mAdapter = new PhotoAdapter(mItems);
        } else {
            mAdapter.notifyDataSetChanged();
        }
        RecyclerView.Adapter adapter = mPhoto_recycler_view.getAdapter();
        if (adapter == null) {
            mPhoto_recycler_view.setAdapter(mAdapter);
        }
    }

    private void doSetupAdapter2(List<GalleryItem> galleryItems) {//wrong, because paging
        mItems.addAll(galleryItems);
        mAdapter = new PhotoAdapter(mItems);
//        mAdapter.notifyDataSetChanged();

        Log.i(TAG, "new RecyclerView.Adapter");
        mPhoto_recycler_view.setAdapter(mAdapter);

    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {
        private String mQuery;

        public FetchItemsTask(String query) {
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Integer... page) {

            if (mQuery == null || mQuery.isEmpty()) {
                return new FlickrFetchr().fetchRecentPhotos(page[0]);
            } else {
                return new FlickrFetchr().searchPhotos(page[0], mQuery);
            }

        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            setupAdapter(galleryItems);

            //preload
            tryPreload(galleryItems);

        }
    }

    private void tryPreload(List<GalleryItem> galleryItems) {
        if (hasFirstPaged && mThumbnailPreloader != null) {
            if (isAdded()) {
                for (GalleryItem galleryItem : galleryItems) {
                    mThumbnailPreloader.preLoad(galleryItem.getUrl());
                }
            }
        }
        hasFirstPaged = true;
    }


    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> list;

        public PhotoAdapter(List<GalleryItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View layout = LayoutInflater.from(getActivity()).inflate(R.layout.list_item_gallery, parent, false);

            return new PhotoHolder(layout);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
            if (list.isEmpty()) {
                holder.bindDrawable(getResources().getDrawable(R.drawable.bill_up_close));
                return;
            }
            holder.bind(list.get(position));
        }


        @Override
        public int getItemCount() {
            return list.size();
        }

    }



    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemView;
        private GalleryItem mGalleryItem;


        public PhotoHolder(@NonNull View itemLayout) {
            super(itemLayout);
            mItemView = itemLayout.findViewById(R.id.item_image_view);

            mItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    Intent intent = new Intent(Intent.ACTION_VIEW, mGalleryItem.getPhotoPageUri());
                    Intent intent = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
                    startActivity(intent);

                }
            });
        }

        public void bindDrawable(Drawable drawable) {
            mItemView.setImageDrawable(drawable);
        }

        public ImageView getItemView() {
            return mItemView;
        }

        public void bind(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;
            Drawable drawable = mImageCache.get(galleryItem.getUrl());
            if (drawable != null) {
                getItemView().setImageDrawable(drawable);
                return;
            }

//            useDefaultImage(holder);
//            mThumbnailDownloader.queueThumbnail(holder, galleryItem.getUrl());
            useGlideToLoadImage(galleryItem);
        }
        private void useGlideToLoadImage(GalleryItem galleryItem) {
            Glide.with(getActivity())
                    .load(galleryItem.getUrl())
                    .placeholder(R.drawable.bill_up_close)
                    .into(getItemView());
        }

    }

}
