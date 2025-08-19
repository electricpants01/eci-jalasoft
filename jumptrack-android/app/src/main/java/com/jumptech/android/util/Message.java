package com.jumptech.android.util;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.jumptech.jumppod.R;

public class Message {

    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    public static final String MESSAGE_STRING = "messageString";
    public static final String POSITIVE_ACTION = "positiveAction";
    public static final String NEGATIVE_ACTION = "negativeAction";

    public static final int NONE = 0;
    public static final String SIMPLE_OK_MESSAGE_RESPONSE = "simpleOkMessageResponse";
    public static final String ERROR_MESSAGE_RESPONSE = "errorMessageResponse";

    public interface MessageListener {
        void onMessageOk(String tag);

        void onMessageCancel(String tag);
    }

    private static void show(int title, int message, int positiveAction, int negativeAction, AppCompatActivity activity, String tag) {
        Bundle args = new Bundle();
        args.putInt(TITLE, title);
        args.putInt(MESSAGE, message);
        args.putInt(POSITIVE_ACTION, positiveAction);
        args.putInt(NEGATIVE_ACTION, negativeAction);

        MessageDialogFragment dialog = new MessageDialogFragment();
        dialog.setArguments(args);

        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.add(dialog, tag);
        ft.commitAllowingStateLoss();
    }

    private static void show(int title, String messageString, int positiveAction, int negativeAction, AppCompatActivity activity, String tag) {
        Bundle args = new Bundle();
        args.putInt(TITLE, title);
        args.putString(MESSAGE_STRING, messageString);
        args.putInt(POSITIVE_ACTION, positiveAction);
        args.putInt(NEGATIVE_ACTION, negativeAction);

        MessageDialogFragment dialog = new MessageDialogFragment();
        dialog.setArguments(args);

        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.add(dialog, tag);
        ft.commitAllowingStateLoss();
    }

    public static void show(String messageString, AppCompatActivity activity) {
        show(NONE, messageString, android.R.string.ok, NONE, activity, SIMPLE_OK_MESSAGE_RESPONSE);
    }

    public static void show(int message, AppCompatActivity activity) {
        show(NONE, message, android.R.string.ok, NONE, activity, SIMPLE_OK_MESSAGE_RESPONSE);
    }

    public static void showQuestion(int title, int message, AppCompatActivity activity, String tag) {
        show(title, message, android.R.string.ok, android.R.string.cancel, activity, tag);
    }

    public static void showQuestion(int message, AppCompatActivity activity, String tag) {
        show(NONE, message, android.R.string.ok, android.R.string.cancel, activity, tag);
    }

    public static void showInfo(int title, int message, AppCompatActivity activity) {
        show(title, message, android.R.string.ok, NONE, activity, SIMPLE_OK_MESSAGE_RESPONSE);
    }

    public static void showInfo(int title, AppCompatActivity activity) {
        show(title, NONE, android.R.string.ok, NONE, activity, SIMPLE_OK_MESSAGE_RESPONSE);
    }

    public static void showError(int message, AppCompatActivity activity) {
        show(R.string.error, message, android.R.string.ok, NONE, activity, ERROR_MESSAGE_RESPONSE);
    }

    public static void showSimpleError(String message, AppCompatActivity activity) {
        show(NONE, message, android.R.string.ok, NONE, activity, ERROR_MESSAGE_RESPONSE);
    }
}
