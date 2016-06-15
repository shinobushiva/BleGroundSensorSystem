package me.shingaki.blesensorgroundsystem;

import android.app.Application;

import com.parse.Parse;

public class ParseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.enableLocalDatastore(this);
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("SBlsPGbKWzYu4kF1vKJmrLVlgQmoGbhRPRaMBvUM")
                .clientKey(null)
                .server("http://ble-ground-sensor-parse-20160615.us-east-1.elasticbeanstalk.com/parse/")
                .build()
        );
    }
}