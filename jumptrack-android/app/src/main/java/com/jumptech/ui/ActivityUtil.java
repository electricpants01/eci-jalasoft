package com.jumptech.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.jumptech.android.util.Preferences;
import com.jumptech.jumppod.R;
import com.jumptech.tracklib.db.TrackPreferences;

/**
 * Created by nbrewster on 2/25/16.
 */
public class ActivityUtil {
    private ActivityUtil() {};

    public static void showEnvironment(Activity activity) {
        //if we  are on an alternate server
        String server = Preferences.baseUrl(activity);
        if (!Preferences.isDefaultBaseUrl(activity)) {
            TextView textViewServer = activity.findViewById(R.id.environment);
            if (textViewServer != null) {
                textViewServer.setVisibility(View.VISIBLE);
                textViewServer.setText(activity.getText(R.string.serverLabel) + " " + server);
            }
        }
    }

    /**
     * This method evaluates the TrackPreferences data against valid login session
     * if it is not valid, redirects the navigation to LoginActivity screen and close the activity
     *
     * @param activity the current activity as context input
     */
    public static void checkLoginSession(Activity activity) {
        if (new TrackPreferences(activity).getAuthToken() == null) {
            activity.startActivity(new Intent(activity, LoginActivity.class));
            activity.finish();
        }
    }
}
