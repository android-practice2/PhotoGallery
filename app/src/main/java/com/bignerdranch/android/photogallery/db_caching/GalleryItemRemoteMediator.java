package com.bignerdranch.android.photogallery.db_caching;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.paging.LoadType;
import androidx.paging.PagingState;
import androidx.paging.RemoteMediator;
import androidx.paging.rxjava3.RxRemoteMediator;

import com.bignerdranch.android.photogallery.FlickrFetchr;
import com.bignerdranch.android.photogallery.GalleryItem;
import com.bumptech.glide.load.HttpException;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

@androidx.paging.ExperimentalPagingApi
public class GalleryItemRemoteMediator extends RxRemoteMediator<Integer, GalleryItem> {

    private static final String TAG = "GRRemoteMediator";

    protected FlickrFetchr mFlickrFetchr;

    protected AppDatabase mAppDatabase;

    protected GalleryItemDAO mGalleryItemDAO;
    protected RemoteKeysDAO mRemoteKeysDAO;

    private String mQuery;

    public GalleryItemRemoteMediator(
            String query,
            FlickrFetchr flickrFetchr,
            AppDatabase appDatabase) {
        mQuery = query;
        mFlickrFetchr = flickrFetchr;
        mAppDatabase = appDatabase;
        mGalleryItemDAO = mAppDatabase.mGalleryItemDAO();
        mRemoteKeysDAO = mAppDatabase.mRemoteKeysDAO();
    }

    @NonNull
    @Override
    public Single<MediatorResult> loadSingle(@NonNull LoadType loadType, @NonNull PagingState<Integer, GalleryItem> pagingState) {

//        return doLoadSingle(loadType, pagingState);
        return doLoadSingleV2(loadType, pagingState);
    }

    private Single<MediatorResult> doLoadSingleV2(@NonNull LoadType loadType, @NonNull PagingState<Integer, GalleryItem> pagingState) {
        Single<RemoteKeys> remoteKeysSingle = null;
        switch (loadType) {
            case REFRESH:
                remoteKeysSingle = Single.just(RemoteKeys.NULL);
                break;
            case APPEND:
                GalleryItem galleryItem = pagingState.lastItemOrNull();
                if (galleryItem != null) {
                    remoteKeysSingle = Single.fromCallable(new Callable<RemoteKeys>() {
                        @Override
                        public RemoteKeys call() throws Exception {
                            RemoteKeys remoteKeys = mRemoteKeysDAO.findByPK(galleryItem.getId());
                            if (remoteKeys == null) {
                                return RemoteKeys.NULL;
                            }
                            return remoteKeys;
                        }
                    });
                } else {
                    remoteKeysSingle = Single.just(RemoteKeys.NULL);

                }
                break;
            case PREPEND:

                GalleryItem firstItemOrNull = pagingState.firstItemOrNull();
                if (firstItemOrNull != null) {
                    remoteKeysSingle = Single.fromCallable(new Callable<RemoteKeys>() {
                        @Override
                        public RemoteKeys call() throws Exception {
                            RemoteKeys remoteKeys = mRemoteKeysDAO.findByPK(firstItemOrNull.getId());
                            if (remoteKeys == null) {
                                return RemoteKeys.NULL;
                            }
                            return remoteKeys;
                        }
                    });
                } else {
                    remoteKeysSingle = Single.just(RemoteKeys.NULL);

                }
                break;

        }

        return remoteKeysSingle.subscribeOn(Schedulers.io())
                .map((Function<RemoteKeys, MediatorResult>) remoteKeys -> {
                    Log.i(TAG, "PREPEND remoteKeys:" + remoteKeys);

                    Integer prevKey;
                    Integer nextKey;
                    int key;
                    if (remoteKeys != null && remoteKeys != RemoteKeys.NULL) {
                        prevKey = remoteKeys.getPrevKey();
                        nextKey = remoteKeys.getNextKey();
                        switch (loadType) {
                            case PREPEND:
                                key = prevKey != null ? prevKey : 1;
                                break;
                            case APPEND:
                                if (nextKey == null) {
                                    return new MediatorResult.Success(true);
                                } else {
                                    key = nextKey;
                                }
                                break;
                            case REFRESH:
                                key = 1;
                                break;
                            default:
                                key = 1;

                        }

                    } else {
                        key = 1;
                    }

                    Log.i(TAG, "page key: " + key);

                    List<GalleryItem> galleryItems;
                    if (mQuery == null) {
                        galleryItems = mFlickrFetchr.fetchRecentPhotos(key);
                    } else {
                        galleryItems = mFlickrFetchr.searchPhotos(key, mQuery);

                    }

                    Integer finalKey = key;
                    mAppDatabase.runInTransaction(new Runnable() {
                        @Override
                        public void run() {
                            if (loadType == LoadType.REFRESH) {
                                mGalleryItemDAO.clearAll();
                                mRemoteKeysDAO.clearAll();
                            }
                            mGalleryItemDAO.insertAll(galleryItems);

                            List<RemoteKeys> remoteKeysList = new LinkedList<>();
                            for (GalleryItem item : galleryItems) {
                                remoteKeysList.add(new RemoteKeys(item.getId(),
                                        finalKey <= 1 ? null : finalKey - 1,
                                        finalKey + 1
                                ));
                            }
                            mRemoteKeysDAO.insertAll(remoteKeysList);
                        }
                    });
                    return new MediatorResult.Success(false);

                })
                .onErrorResumeNext(e -> {
                    if (e instanceof IOException || e instanceof HttpException) {
                        return Single.just(new MediatorResult.Error(e));
                    }
                    return Single.error(e);
                })
                ;

    }

    private Single<MediatorResult> doLoadSingle(@NonNull LoadType loadType, @NonNull PagingState<Integer, GalleryItem> pagingState) {
        AtomicInteger finalKey = new AtomicInteger();

        return Single.fromCallable(new Callable<List<GalleryItem>>() {
                    @Override
                    public List<GalleryItem> call() throws Exception {
                        AtomicReference<Integer> key = new AtomicReference<>();
                        switch (loadType) {
                            case PREPEND:
                                GalleryItem firstItemOrNull = pagingState.firstItemOrNull();
                                if (firstItemOrNull != null) {
                                    RemoteKeys remoteKeys = mRemoteKeysDAO.findByPK(firstItemOrNull.getId());
                                    Log.i(TAG, "PREPEND remoteKeys:" + remoteKeys);
                                    if (remoteKeys != null) {
                                        key.set(remoteKeys.getPrevKey());
                                        break;
                                    }
                                }
                                key.set(1);
                                break;
                            case APPEND:
                                GalleryItem lastItemOrNull = pagingState.lastItemOrNull();
                                if (lastItemOrNull != null) {
                                    RemoteKeys remoteKeys = mRemoteKeysDAO.findByPK(lastItemOrNull.getId());
                                    Log.i(TAG, "APPEND remoteKeys:" + remoteKeys);
                                    if (remoteKeys != null) {
                                        key.set(remoteKeys.getNextKey());
                                        break;
                                    }
                                }
                                key.set(1);
                                break;
                            case REFRESH:
                                //ignore
                                break;
                        }

                        int handledKey = key.get() != null ? key.get() : 1;
                        finalKey.set(handledKey);

                        Log.i(TAG, "page key: " + handledKey);

                        if (mQuery == null) {
                            return mFlickrFetchr.fetchRecentPhotos(handledKey);
                        } else {
                            return mFlickrFetchr.searchPhotos(handledKey, mQuery);

                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .map((Function<List<GalleryItem>, MediatorResult>) items -> {
                    if (items.isEmpty()) {
                        return new MediatorResult.Success(true);
                    }
                    mAppDatabase.runInTransaction(new Runnable() {
                        @Override
                        public void run() {
                            if (loadType == LoadType.REFRESH) {
                                mGalleryItemDAO.clearAll();
                                mRemoteKeysDAO.clearAll();
                            }
                            mGalleryItemDAO.insertAll(items);

                            List<RemoteKeys> remoteKeysList = new LinkedList<>();
                            for (GalleryItem item : items) {
                                remoteKeysList.add(new RemoteKeys(item.getId(),
                                        finalKey.get() <= 1 ? null : finalKey.get() - 1,
                                        finalKey.get() + 1
                                ));
                            }
                            mRemoteKeysDAO.insertAll(remoteKeysList);
                        }
                    });
                    return new MediatorResult.Success(false);

                })
                .onErrorResumeNext(e -> {
                    if (e instanceof IOException || e instanceof HttpException) {
                        return Single.just(new MediatorResult.Error(e));
                    }
                    return Single.error(e);
                })
                ;
    }
}
