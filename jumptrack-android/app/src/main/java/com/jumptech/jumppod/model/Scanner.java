package com.jumptech.jumppod.model;

import com.jumptech.jumppod.R;

public enum Scanner {
    CAMERA(R.string.camera, false),
    KDC(R.string.empty, true),
    SOCKET_MOBILE(R.string.socketMobile, true);

    private int _userStr;
    private boolean _bluetooth;

    Scanner(int userStr, boolean bluetooth) {
        _userStr = userStr;
        _bluetooth = bluetooth;
    }

    public int getUserStr() {
        return _userStr;
    }

    public boolean isBluetooth() {
        return _bluetooth;
    }

    public boolean isManualTrigger() {
        return Scanner.CAMERA == this;
    }

    public boolean enabled() {
        return Scanner.KDC != this;
    }
}
