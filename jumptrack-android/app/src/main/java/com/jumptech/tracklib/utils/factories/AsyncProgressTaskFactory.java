package com.jumptech.tracklib.utils.factories;

import android.os.AsyncTask;
import android.util.Log;

import com.jumptech.tracklib.comms.TrackException;
import com.jumptech.tracklib.db.Business;

/**
 * Creates {@link AsyncTask} to perform background process
 */
public class AsyncProgressTaskFactory {
    private static String TAG = "AsyncProgressTaskFactory";

    private AsyncProgressTaskFactory() {
        //none
    }

    /**
     * Creates an async task to sync data operations
     *
     * @param business {@link Business}
     * @return {@link AsyncTask}
     */
    public static AsyncTask<Void, Void, Void> createSyncQuietTask(final Business business){
        return new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    business.syncQuiet();
                } catch (TrackException e) {
                    Log.e(TAG,"createSyncQuietTask", e);
                }
                return null;
            }
        };
    }
}
