package org.fasola.fasolaminutes;

import android.app.Application;
import android.content.Context;

/**
 * Application Override
 * Application-wide initialization code (e.g. Database)
 * Static getContext() for the application context
 */
public class MinutesApplication extends Application {
    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        // Open the database
        MinutesDb.getInstance(mContext);
    }
}
