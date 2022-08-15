package com.bignerdranch.android.photogallery.paging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingSource;
import androidx.paging.PagingState;
import androidx.paging.rxjava3.RxPagingSource;

import com.bignerdranch.android.photogallery.FlickrFetchr;
import com.bignerdranch.android.photogallery.GalleryItem;

import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FlickerPagingSource extends RxPagingSource<Integer, GalleryItem> {


    private String query;

    public FlickerPagingSource(String query) {
        this.query = query;
    }

    @NonNull
    @Override
    public Single<LoadResult<Integer, GalleryItem>> loadSingle(@NonNull LoadParams<Integer> loadParams) {
        Integer key = loadParams.getKey();
        int nextPage;
        if (key == null) {
            nextPage = 1;
        } else {
            nextPage = key + loadParams.getLoadSize()/ FlickrFetchr.PAGE_SIZE;
        }
        Integer prePage;
        if (key == null || key <= 1) {
            prePage = null;
        } else {
            prePage = key - 1;
        }
        Single<LoadResult<Integer, GalleryItem>> page = doLoadSingle(nextPage, prePage);

        return page;
    }

    private Single<LoadResult<Integer, GalleryItem>> doLoadSingle(int nextPage, Integer prePage) {

        Single<LoadResult<Integer, GalleryItem>> single = null;
        try {
            single = Single.fromCallable(new Callable<List<GalleryItem>>() {
                        @Override
                        public List<GalleryItem> call() throws Exception {
                            if (query == null) {
                                return new FlickrFetchr().fetchRecentPhotos(nextPage);

                            } else {
                                return new FlickrFetchr().searchPhotos(nextPage, query);

                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .map(galleryItems -> {
                        LoadResult<Integer, GalleryItem> page = new LoadResult.Page<>(
                                galleryItems,
                                prePage,
                                nextPage
                        );
                        return page;
                    })
                    .onErrorReturn(LoadResult.Error::new);
        } catch (Exception e) {
            return Single.just(new LoadResult.Error<Integer, GalleryItem>(e));
        }
        return single
                ;
    }


    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, GalleryItem> pagingState) {
//        return doGetRefreshKey(pagingState);
        return null;

    }

    @Nullable
    private Integer doGetRefreshKey(@NonNull PagingState<Integer, GalleryItem> pagingState) {
        Integer anchorPosition = pagingState.getAnchorPosition();

        if (anchorPosition == null) {
            return null;
        }
        LoadResult.Page<Integer, GalleryItem> closestPage = pagingState.closestPageToPosition(anchorPosition);
        if (closestPage == null) {
            return null;
        }
        Integer prevKey = closestPage.getPrevKey();
        if (prevKey != null) {
            return prevKey + 1;
        }
        Integer nextKey = closestPage.getNextKey();
        if (nextKey != null) {
            return nextKey - 1;
        }

        return null;
    }


    public void setQuery(String query) {
        this.query = query;
    }
}
