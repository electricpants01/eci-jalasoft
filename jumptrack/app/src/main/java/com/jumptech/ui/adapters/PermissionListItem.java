package com.jumptech.ui.adapters;

import android.content.Context;

import com.jumptech.tracklib.utils.PermissionUtil;

public class PermissionListItem {

    private final int permissionTitle;
    private final String permissionKey;
    private final int permissionDenyMessage;
    private boolean permissionGranted;

    public PermissionListItem(Context context, int permissionTitle, String permissionKey, int permissionDenyMessage) {
        this.permissionTitle = permissionTitle;
        this.permissionKey = permissionKey;
        updateStatus(context);
        this.permissionDenyMessage = permissionDenyMessage;
    }

    public void updateStatus(Context context) {
        this.permissionGranted = PermissionUtil.hasPermission(context, this.permissionKey);
    }

    public boolean isPermissionGranted() {
        return permissionGranted;
    }

    public int getPermissionTitle() {
        return permissionTitle;
    }

    public String getPermissionKey() {
        return permissionKey;
    }

    public int getPermissionDenyMessage() {
        return permissionDenyMessage;
    }
}
