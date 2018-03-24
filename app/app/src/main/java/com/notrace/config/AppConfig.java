package com.notrace.config;

import android.content.Context;


/**
 * Created by budao on 2016/10/12.
 */
public class AppConfig {
    private static final String TAG = "AppConfig";
    private static AppPreferences sPreferences = new AppPreferences();

    public static void init(Context context) {
        loadAppSetting(context);
    }

    public static String getWebsocketUrl() {
        return sPreferences.getString("websocketurl","");
    }

    private static void loadAppSetting(Context context) {
        AppConfigXmlParser parser = new AppConfigXmlParser();
        parser.parse(context);
        sPreferences = parser.getPreferences();
    }
}
