package com.lty.videofile;

import android.app.Application;
import android.content.Context;

/**
 * @author litaoyuanli
 * @date 11/5/21
 * 描述信息
 */
public class BaseApplication extends Application {
    private static BaseApplication instance;
    private static Context context;

    public static Context getContext() {
        return context;
    }

    public static BaseApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        context = this;
    }
}
