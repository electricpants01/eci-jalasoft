package com.jumptech.android.util;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class MessageDialogFragment extends DialogFragment {

    private Message.MessageListener listener;
    private int title;
    private int message;
    private String messageString;
    private int positiveAction;
    private int negativeAction;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getInt(Message.TITLE);
            message = getArguments().getInt(Message.MESSAGE);
            messageString = getArguments().getString(Message.MESSAGE_STRING);
            positiveAction = getArguments().getInt(Message.POSITIVE_ACTION);
            negativeAction = getArguments().getInt(Message.NEGATIVE_ACTION);
        }
        setCancelable(false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (title != Message.NONE) {
            builder.setTitle(title);
        }
        if (message != Message.NONE) {
            builder.setMessage(message);
        }
        if (!TextUtils.isEmpty(messageString)) {
            builder.setMessage(messageString);
        }
        builder.setPositiveButton(positiveAction, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (getActivity() instanceof Message.MessageListener) {
                    listener = (Message.MessageListener) getActivity();
                }
                if (listener != null && getTag() != null) listener.onMessageOk(getTag());
            }
        });
        if (negativeAction != Message.NONE) {
            builder.setNegativeButton(negativeAction, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (getActivity() instanceof Message.MessageListener) {
                        listener = (Message.MessageListener) getActivity();
                    }
                    if (listener != null && getTag() != null) listener.onMessageCancel(getTag());
                }
            });
        }
        return builder.create();
    }
}
