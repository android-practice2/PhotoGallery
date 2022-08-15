package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "received result: " + getResultCode());
        if (getResultCode() != Activity.RESULT_OK) {
            return;
        }

        Notification notification = intent.getParcelableExtra(PollService.EXTRA_NOTIFICATION);
        int requestCode = intent.getIntExtra(PollService.EXTRA_REQUEST_CODE, 0);

        PollService.publishNotification(context, notification, requestCode);

    }
}
