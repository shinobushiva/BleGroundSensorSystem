package me.shingaki.blesensorgroundsystem;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by keisuke on 15/09/16.
 */
public class LocationService implements LocationListener {


    private final static String TAG = LocationService.class.getSimpleName();

    private MainActivity activity;
    private TextView mTextGps;

    private LocationManager mLocationManager;
    private final String provider;

    public LocationService(MainActivity activity){
        this.activity = activity;

        mTextGps = (TextView)activity.findViewById(R.id.text_gps);

        mLocationManager =
                (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        boolean gpsFlg = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d("GPS Enabled", gpsFlg?"OK":"NG");

        // Criteriaオブジェクトを生成
        Criteria criteria = new Criteria();

        // Accuracyを指定(低精度)
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        // PowerRequirementを指定(低消費電力)
        criteria.setPowerRequirement(Criteria.POWER_HIGH);

        // ロケーションプロバイダの取得
        provider = mLocationManager.getBestProvider(criteria, true);
        Log.d(TAG, "provider:" + provider);

        startLocationService();


    }

    public void startLocationService() {
        // LocationListenerを登録
        mLocationManager.requestLocationUpdates(provider, 0, 0, this);
        Log.d(TAG, "get location manager");

    }

    public void stopLocationService() {
        mLocationManager.removeUpdates(this);
    }


    @Override
    public void onLocationChanged(final Location location) {
        activity.location = location;
        Log.d(TAG, "get location" + location.getLatitude());
        // グラフの表示
        Runnable  mTimer2 = new Runnable() {
            @Override
            public void run() {
                mTextGps.setText(location.toString());
            }
        };
        activity.mHandler.post(mTimer2);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.v(TAG, "きてんのか！");
        switch (i) {
            case LocationProvider.AVAILABLE:
                Log.v(TAG, "Status" + "AVAILABLE");
                break;
            case LocationProvider.OUT_OF_SERVICE:
                Log.v(TAG, "Status" + "OUT_OF_SERVICE");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.v(TAG, "Status" + "TEMPORARILY_UNAVAILABLE");
                break;
        }
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.v(TAG, "onProviderEnabled きてんのか！");
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.v(TAG, "onProviderDisabled きてんのか！");
    }

}

