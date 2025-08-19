package com.jumptech.tracklib.utils;

import java.util.regex.Pattern;

public class CommonUtil {

    private static String SERVER_ADDRESS_REGEX = "^(.+\\.)?myjumptrack.com";

    public static boolean isValidServerAddress(String serverAddress) {
        return Pattern.matches(SERVER_ADDRESS_REGEX, serverAddress);
    }
}
