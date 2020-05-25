package com.shureck.assistentv2;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class App extends Application {
    private static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static boolean sendLocalBroadcastMessage(String action,String result) {
        Intent sendIntent = new Intent(action);
        sendIntent.putExtra("Result",result);
        return LocalBroadcastManager.getInstance(context).sendBroadcast(sendIntent);
    }
}
