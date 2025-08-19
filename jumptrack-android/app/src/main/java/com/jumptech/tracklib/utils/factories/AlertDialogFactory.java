package com.jumptech.tracklib.utils.factories;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;

import com.jumptech.android.util.Util;
import com.jumptech.jumppod.R;
import com.jumptech.tracklib.db.TrackPreferences;

/**
 * Creates alert dialogs
 */
public class AlertDialogFactory {
    private AlertDialogFactory() {
        //none
    }

    /**
     * Creates an info dialog
     *
     * @param cntx a context
     * @param msgId resource id to get the message
     * @return {@link AlertDialog}
     */
    public static AlertDialog createInfoDialog(final Context cntx, final int msgId) {
        return new AlertDialog.Builder(cntx)
                .setCancelable(false)
                .setMessage(msgId)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    public static AlertDialog createMapListDialog(final Context context, final Double latitude , final Double longitude){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.select_map_app));

        String[] mapList = {Util.MapsPackageNames.GoogleMaps.toString(), Util.MapsPackageNames.Waze.toString()};
        int checkedItem = 0;
        builder.setSingleChoiceItems(mapList, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: {
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + latitude + "," + longitude)).setPackage(Util.googleMapsPackageName));
                        TrackPreferences preferences = new TrackPreferences(context);
                        preferences.setDefaulMapApp(Util.googleMapsPackageName);
                        dialog.dismiss();
                        break;
                    }
                    case 1:{
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + latitude + "," + longitude+"?q=" + latitude + "," + longitude )).setPackage(Util.wazePackageName));
                        TrackPreferences preferences = new TrackPreferences(context);
                        preferences.setDefaulMapApp(Util.wazePackageName);
                        dialog.dismiss();
                        break;
                    }
                }
            }
        });

        return builder.create();
    }


    public static AlertDialog selectDefaultMapApp(final Context context, boolean isWazeInstalled){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.select_map_app));

        String[] bothApps = {Util.MapsPackageNames.GoogleMaps.toString(), Util.MapsPackageNames.Waze.toString()};
        String[] onlyGoogleMaps = {Util.MapsPackageNames.GoogleMaps.toString()};
        String[] list;
        if (isWazeInstalled){
            list = bothApps;
        }else{
            list = onlyGoogleMaps;
        }
        int checkedItem = 0;
        TrackPreferences preferences = new TrackPreferences(context);
        if(preferences.getDefaulMapApp().equals(Util.googleMapsPackageName)){
            checkedItem = 0;
        }else if (preferences.getDefaulMapApp().equals(Util.wazePackageName)){
            checkedItem = 1;
        }

        builder.setSingleChoiceItems(list, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: {
                        TrackPreferences preferences = new TrackPreferences(context);
                        preferences.setDefaulMapApp(Util.googleMapsPackageName);
                        dialog.dismiss();
                        break;
                    }

                    case 1:{
                        TrackPreferences preferences = new TrackPreferences(context);
                        preferences.setDefaulMapApp(Util.wazePackageName);
                        dialog.dismiss();
                        break;
                    }
                }
            }
        });
        return builder.create();
    }
}
