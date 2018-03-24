package com.notrace.application;

import android.app.Application;

import com.notrace.config.AppConfig;
import com.notrace.utils.WebsocketConn;

/**
 * Created by paron on 2018/3/24.
 */

public class DemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppConfig.init(this);
        WebsocketConn.conn(AppConfig.getWebsocketUrl());
    }
}
