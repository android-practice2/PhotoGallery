package com.bignerdranch.android.photogallery;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;
import java.util.concurrent.TimeUnit;
public class PollServiceV2 extends JobService {
    private static final String TAG = "PollServiceV2";
    private static final int JOB_ID=1;
    private static final long POLL_INTERVAL_MS = TimeUnit.SECONDS.toMillis(60);//>=60s is effected
    private PollTask mPollTask;



    public static void setServiceAlarm(Context context, boolean isOn) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (isOn) {
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, PollServiceV2.class))
//                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)//for test
                    .setPersisted(true)
                    .setPeriodic(1000*10)
                    .build();

            jobScheduler.schedule(jobInfo);

        }else{

            jobScheduler.cancel(JOB_ID);

        }
        QueryPreferences.setAlarmOn(context, isOn);
    }
    public static boolean isServiceAlarmOn(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        List<JobInfo> allPendingJobs = jobScheduler.getAllPendingJobs();
        for (JobInfo pendingJob : allPendingJobs) {
            if (pendingJob.getId() == JOB_ID) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        mPollTask = new PollTask();
        mPollTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (mPollTask != null) {
            mPollTask.cancel(true);
        }

        return true;
    }

    private class PollTask extends AsyncTask<JobParameters,Void,Void>{
        @Override
        protected Void doInBackground(JobParameters... jobParameters) {
            JobParameters jobParameter = jobParameters[0];

            handleIntent();

            jobFinished(jobParameter,true);
            return null;
        }
    }

    protected void handleIntent() {
        if (!isNetworkAvailableAndConnected()) {
            return;
        }
        Log.i(TAG, "Scheduled a job"  );

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

            Intent photoIntent = PhotoGalleryActivity.newIntent(this);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, photoIntent, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(getString(R.string.new_pictures_title))
                    .setContentText(getString(R.string.new_pictures_text))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();

            NotificationManagerCompat.from(this)
                    .notify(0,notification);


        } else {
            Log.i(TAG, "Got an old result: " + resultId);
        }


    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo() != null
                && connectivityManager.getActiveNetworkInfo().isConnected();

    }

}
