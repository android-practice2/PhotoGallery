package com.bignerdranch.android.photogallery.paging;

import static com.bignerdranch.android.photogallery.PhotoGalleryFragment.MARK_USE_LOCAL_DB_CACHING;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingSource;
import androidx.paging.rxjava3.PagingRx;

import com.bignerdranch.android.photogallery.FlickrFetchr;
import com.bignerdranch.android.photogallery.GalleryItem;
import com.bignerdranch.android.photogallery.db_caching.AppDatabase;
import com.bignerdranch.android.photogallery.db_caching.GalleryItemDAO;
import com.bignerdranch.android.photogallery.db_caching.GalleryItemRemoteMediator;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.core.Flowable;
import kotlinx.coroutines.CoroutineScope;

@HiltViewModel
@androidx.paging.ExperimentalPagingApi
public class PhotoGalleryViewModel extends ViewModel {

    private PagingSource<Integer, GalleryItem>  mPagingSource;
    private Flowable<PagingData<GalleryItem>> mFlowable;
    private String query;

    public FlickrFetchr mFlickrFetchr;
    public AppDatabase mAppDatabase;
    public GalleryItemDAO mGalleryItemDAO;

    @Inject
    public PhotoGalleryViewModel(
            FlickrFetchr mFlickrFetchr,
            AppDatabase mAppDatabase

    ) {
        this.mFlickrFetchr = mFlickrFetchr;
        this.mAppDatabase = mAppDatabase;
        this.mGalleryItemDAO = mAppDatabase.mGalleryItemDAO();

        Pager<Integer, GalleryItem> pager;
        PagingConfig config = new PagingConfig(FlickrFetchr.PAGE_SIZE,
                FlickrFetchr.PAGE_SIZE,
                false, FlickrFetchr.PAGE_SIZE * 2
                , FlickrFetchr.PAGE_SIZE * 4
        );
        if (MARK_USE_LOCAL_DB_CACHING) {
            pager = new Pager<Integer, GalleryItem>(
                    config,
                    null,
                    new GalleryItemRemoteMediator(query, mFlickrFetchr, mAppDatabase),
                    () -> {
                        mPagingSource = mGalleryItemDAO.pagingSource();
                        return mPagingSource;
                    }
            );
            mFlowable = PagingRx.getFlowable(pager);

        } else {
            pager = new Pager<Integer, GalleryItem>(
                    config,
                    () -> {
                        mPagingSource = new FlickerPagingSource(query);
                        return mPagingSource;
                    }//each time PagingSource should be new
            );
            Flowable<PagingData<GalleryItem>> pagingDataFlowable = PagingRx.getFlowable(pager);

            CoroutineScope viewModelScope = ViewModelKt.getViewModelScope(this);
            mFlowable = PagingRx.cachedIn(pagingDataFlowable, viewModelScope);

        }



    }

    public Flowable<PagingData<GalleryItem>> getFlowable() {
        return mFlowable;
    }

    public PagingSource<Integer, GalleryItem>  getPagingSource() {
        return mPagingSource;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
