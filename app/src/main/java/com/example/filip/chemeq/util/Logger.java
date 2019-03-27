package com.example.filip.chemeq.util;

import android.util.Log;

public class Logger {

    private final String tag;
    private boolean debug = true;
    private boolean warning = false;

    public Logger(String tag) {
        this.tag = tag;
    }

    public Logger(String tag, boolean debug, boolean warning) {
        this.tag = tag;
        this.debug = debug;
        this.warning = warning;
    }

    public void d(final String msg){
        if (debug)
            Log.d(tag, msg);
    }

    public void i(final String msg){
        Log.i(tag, msg);
    }

    public void e(final Throwable t, final String msg) {
        if (warning)
            Log.e(tag, msg, t);
    }

    public void w(final String msg) {
        Log.w(tag, msg);
    }
}
