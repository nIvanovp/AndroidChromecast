package com.altabel.cast.utils;

import android.util.Log;

/**
 * Created by nikolai on 30.12.13.
 */
public class LoggerUtils {
    public static boolean isDebug = false;
    public final static String TAG = "CC_DEBUG";
    private final static String INFO_TAG = "INFO:";
    private final static String ERROR_TAG = "ERROR:";

    public static void i(String message){
        if(isDebug)
            Log.i(TAG, INFO_TAG + message);
    }

    public static void e(String message){
        if(isDebug)
            Log.e(TAG, ERROR_TAG + message);
    }
}
