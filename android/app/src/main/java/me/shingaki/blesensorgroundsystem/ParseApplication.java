package me.shingaki.blesensorgroundsystem;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseCrashReporting;

public class ParseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ParseCrashReporting.enable(this);
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "SBlsPGbKWzYu4kF1vKJmrLVlgQmoGbhRPRaMBvUM", "neeJ39h45acmGl4S8Z6pgDb0YsRUBDlIct2QyV9o");
    }
}