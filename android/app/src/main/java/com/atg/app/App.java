package com.atg.app;

import android.app.Application;

import com.atg.app.data.ApiClient;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.init(this);
    }
}
