package com.jumptech.tracklib.repository;

import android.content.Context;
import android.database.Cursor;

import com.jumptech.android.util.Util;
import com.jumptech.tracklib.comms.CommandPrompt;
import com.jumptech.tracklib.data.Command;
import com.jumptech.tracklib.data.Prompt;
import com.jumptech.tracklib.data.Route;
import com.jumptech.tracklib.room.TrackDB;

public class RouteRepository {

    public static void storeRoute(Context context, CommandPrompt cmdPrompt) {
        if (cmdPrompt != null && cmdPrompt.prompt != null) {
            updateCommand(context, cmdPrompt.command.name());
            PromptRepository.storePrompt(context, cmdPrompt.prompt, Prompt.Type.PROMPT);
        }
    }

    public static void updateCommand(Context context, String command) {
        TrackDB.getInstance(context).getRouteDao().updateCommand(command);
    }

    public static void updateRouteOrderUpload(Context context, boolean upload) {
        TrackDB.getInstance(context).getRouteDao().updateOrderUpload(upload);
    }

    public static void clearPending(Context context) {
        int deleted = TrackDB.getInstance(context).getStopDao().deleteSignaturesNull();
        if (deleted > 0) {
            clearPendingOrphans(context);
        }
    }

    public static void clearCompleted(Context context){
        int deleted = TrackDB.getInstance(context).getStopDao().deleteSignaturesAll();
        if (deleted > 0){
            clearPendingOrphans(context);
        }
    }

    public static void clearPendingFromStopKey(Context context, long stopKey) {
        int deleted = TrackDB.getInstance(context).getStopDao().deleteSignaturesNullFromStopKey(stopKey);
        if (deleted > 0) {
            clearPendingOrphans(context);
        }
    }

    public static void clearPendingOrphans(Context context) {
        TrackDB.getInstance(context).getDeliveryDao().clearPending();
        TrackDB.getInstance(context).getLineDao().clearPending();
        TrackDB.getInstance(context).getPlateDao().clearPending();
    }

    public static void updateFinishedRoutes(Context context) {
        TrackDB.getInstance(context).getRouteDao().updateFinishedRoutes(true);
    }

    public static Route fetchRoute(Context context) {

        Cursor cursor = TrackDB.getInstance(context).getRouteDao().fetchRoute();

        if (!cursor.moveToFirst()) return null;

        int c = -1;
        Route route = new Route();
        route._key = cursor.getLong(++c);
        route._name = cursor.getString(++c);
        route._command = (cursor.isNull(++c) ? null : Command.valueOf(cursor.getString(c)));
        route._finished = cursor.getInt(++c) == 1;
        route._orderUploaded = cursor.getInt(++c) == 1;
        return route;
    }

    public static boolean routeHasStop(Context context, Boolean finished) {
        Cursor cursor = StopRepository.stopQuery(context, finished, null, null);
        boolean result = cursor.moveToFirst();
        Util.close(cursor);
        return result;
    }

    public static void clearAndInsertRoute(Context context, long routeKey, String name) {
        TrackDB.getInstance(context).getRouteDao().nukeTable();
        TrackDB.getInstance(context).getRouteDao().insertRoute(routeKey, name);
    }

    public static void nukeTable(Context context) {
        TrackDB.getInstance(context).getRouteDao().nukeTable();
    }

}
