package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollService extends IntentService {
    private static final String TAG = "PollService";
    private static final long POLL_INTERVAL_MS = TimeUnit.SECONDS.toMillis(60);//>=60s is effected
    private static final String NOTIFICATION_CHANNEL_ID = "com.bignerdranch.android.photogallery.new_results";
    public static final String ACTION_SHOW_NOTIFICATION = "com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE = "com.bignerdranch.android.photogallery.PRIVATE";
    public static final String EXTRA_REQUEST_CODE = "REQUEST_CODE";
    public static final String EXTRA_NOTIFICATION = "NOTIFICATION";

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    public PollService() {
        super(TAG);
    }



    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent intent = newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (isOn) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        QueryPreferences.setAlarmOn(context, isOn);
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent intent = newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (!isNetworkAvailableAndConnected()) {
            return;
        }
        Log.i(TAG, "Received an intent: " + intent);

        String storedQuery = QueryPreferences.getStoredQuery(this);

        List<GalleryItem> galleryItems;
        if (storedQuery == null || storedQuery.isEmpty()) {
            galleryItems = new FlickrFetchr().fetchRecentPhotos(1);
        } else {
            galleryItems = new FlickrFetchr().searchPhotos(1, storedQuery);
        }

        if (galleryItems.isEmpty()) {
            return;
        }
        GalleryItem galleryItem = galleryItems.get(0);

        String lastResultId = QueryPreferences.getLastResultId(this);
        String resultId = galleryItem.getId();
        if (!resultId.equals(lastResultId)) {
            Log.i(TAG, "Got a new result: " + resultId);
            QueryPreferences.setLastResultId(this, resultId);

            broadcastShowingBackgroundNotification();

        } else {
            Log.i(TAG, "Got an old result: " + resultId);
        }


    }

    private void broadcastShowingBackgroundNotification() {
        Log.i(TAG, "Send a notification");

        Notification notification = buildNotification();
//        publishNotification(notification);
//        sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE);

        Intent intent = new Intent(ACTION_SHOW_NOTIFICATION);
        intent.putExtra(EXTRA_NOTIFICATION, notification);
        intent.putExtra(EXTRA_REQUEST_CODE, 0);

        sendOrderedBroadcast(intent, PERM_PRIVATE
                , null, null, Activity.RESULT_OK, null, null);

    }

    public static void publishNotification(Context context, Notification notification, int requestCode) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (isApiAfterOrEqual26()) {//API level >=26
            NotificationChannelCompat.Builder channelBuilder = new NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID
                    , NotificationManagerCompat.IMPORTANCE_HIGH);
            channelBuilder.setName(context.getString(R.string.new_pictures_title));
            notificationManager.createNotificationChannel(channelBuilder.build());
        }
        notificationManager.notify(requestCode, notification);
    }

    @NonNull
    private Notification buildNotification() {
        Intent photoIntent = PhotoGalleryActivity.newIntent(this);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, photoIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setTicker(getString(R.string.new_pictures_title))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(getString(R.string.new_pictures_title))
                .setContentText(getString(R.string.new_pictures_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (isApiAfterOrEqual26()) {
            notificationBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }
        Notification notification = notificationBuilder.build();
        return notification;
    }

    private static boolean isApiAfterOrEqual26() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo() != null
                && connectivityManager.getActiveNetworkInfo().isConnected();

    }
}
