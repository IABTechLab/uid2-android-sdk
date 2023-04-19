package com.uid2.dev;

import android.app.Application;
import com.uid2.UID2Manager;

public class GMADevApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        UID2Manager.init(getApplicationContext());
    }

}
