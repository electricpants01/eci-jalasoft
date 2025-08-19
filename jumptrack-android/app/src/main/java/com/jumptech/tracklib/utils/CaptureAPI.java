package com.jumptech.tracklib.utils;

import com.socketmobile.capture.CaptureError;
import com.socketmobile.capture.client.ConnectionState;

public interface CaptureAPI {
    void onCaptureAPIData(String data);

    void onCaptureAPIError(CaptureError captureError, ConnectionState connectionState);

    void onDeviceReady();
}
