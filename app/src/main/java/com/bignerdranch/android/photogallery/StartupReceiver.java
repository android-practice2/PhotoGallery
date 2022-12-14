package com.bignerdranch.android.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast intent: " + intent.getAction());

        boolean alarmOn = QueryPreferences.getAlarmOn(context);
        if (PhotoGalleryFragment.MARKER_USE_JOB) {
            PollServiceV2.setServiceAlarm(context, alarmOn);

        }else{
            PollService.setServiceAlarm(context, alarmOn);

        }

    }
}
