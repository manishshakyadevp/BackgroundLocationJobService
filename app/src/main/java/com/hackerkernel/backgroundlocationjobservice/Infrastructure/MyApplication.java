package com.hackerkernel.backgroundlocationjobservice.Infrastructure;

import android.app.Application;
import android.content.Context;

import com.android.volley.RequestQueue;
import com.hackerkernel.backgroundlocationjobservice.Network.MyVolley;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static MyApplication mInstance;
    public static RequestQueue mRequestQue;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mRequestQue = MyVolley.getInstance().getRequestQueue();
    }

    public static MyApplication getInstance() {
        return mInstance;
    }

    public static Context getAppContext() {
        return mInstance.getApplicationContext();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
}