package com.jumptech.android.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Manages all interactions with static resources such as reading, writing, transformations, etc.
 */
public class FileManager {

    /**
     * Reads a raw source file and transforms it into a normal string.
     *
     * @param context current context.
     * @param resId resource Id reference.
     */
    public static String getContentFromRawResource(Context context, int resId) throws IOException {
        InputStream inputStream = context.getResources().openRawResource(resId);

        InputStreamReader inputReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputReader);
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }
}
