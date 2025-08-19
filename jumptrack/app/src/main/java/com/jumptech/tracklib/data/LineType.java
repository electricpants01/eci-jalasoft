package com.jumptech.tracklib.data;

import com.google.gson.JsonObject;

public enum LineType {
    NONE,
    SCAN,
    LICENSE_PLATE;

    public static LineType fromJsonObject(JsonObject o) {
        if (o.has("scan")) {
            return SCAN;
        }
        if (o.has("license-plate")) {
            return LICENSE_PLATE;
        }
        return NONE;
    }

}
