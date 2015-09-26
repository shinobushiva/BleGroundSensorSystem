package me.shingaki.blesensorgroundsystem;

import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * Created by keisuke on 15/09/16.
 */
public class LocationService implements LocationListener {

    private final static String TAG = LocationService.class.getSimpleName();

    private Handler mHandler = new Handler();

    private MainActivity activity;
    private TextView mTextGps;
    private TextView mTextPlace;
    private TextView mTextWeather;

    private LocationManager mLocationManager;
    private final String provider;

    private Location location;
    public Location getLocation(){
        return location;
    }
    public String getPlace(){
        return mTextPlace.getText().toString();
    }

    public String getWeather() {
        return mTextWeather.getText().toString();
    }

    public LocationService(MainActivity activity){
        this.activity = activity;
        Log.d(TAG, ""+activity.getClass());

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

    public void setView (View view) {
        mTextGps = (TextView)view.findViewById(R.id.text_gps);
        mTextPlace = (TextView)view.findViewById(R.id.text_place);
        mTextWeather = (TextView)view.findViewById(R.id.text_weather);
//        Log.d(TAG, ""+mTextGps);
    }

    public void startLocationService() {
        // LocationListenerを登録
        mLocationManager.requestLocationUpdates(provider, 30*1000, 0, this);
        Log.d(TAG, "get location manager");

    }

    public void stopLocationService() {
        mLocationManager.removeUpdates(this);
    }


    @Override
    public void onLocationChanged(final Location location) {
        this.location = location;
        Log.d(TAG, "get location" + location.getLatitude());
        stopLocationService();

        mTextGps.setText(""+location.getLatitude()+","+location.getLongitude());

        if(Geocoder.isPresent()) {

            try {
                Geocoder geocoder = new Geocoder(activity);
                List<Address> addrs = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                for(Address addr : addrs){
                    Log.d(TAG, addr.toString());
                    mTextPlace.setText(addr.getAddressLine(1));
                }

            }catch(IOException ioEx) {
                ioEx.printStackTrace();
            }
        }

        new AsyncTask<String, Void, Integer>(){
            @Override
            protected Integer doInBackground(String[] params) {

                final String weather = getWeather(location.getLatitude(), location.getLongitude());
                Runnable  mTimer2 = new Runnable() {
                    @Override
                    public void run() {
                        mTextWeather.setText(weather);
                    }
                };
                mHandler.post(mTimer2);

                return 0;
            }
        }.execute(new String[0]);

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

    private String getWeather(double latitude, double longitude) {
        try {
            String requestURL = "http://api.openweathermap.org/data/2.5/weather?"
                    + "lat=" + latitude
                    + "&lon=" + longitude
                    + "&mode=json";

            URL url = new URL(requestURL);
            InputStream is = url.openConnection().getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while (null != (line = reader.readLine())) {
                sb.append(line);
            }
            String data = sb.toString();
            Log.d(TAG, data);
            JSONObject jsonObject = new JSONObject(data);
            JSONObject jo = (JSONObject)jsonObject.getJSONArray("weather").get(0);
            Log.d(TAG, jo.toString());
            String w = jo.getString("main");

            return w;

        }catch(Exception e){
            e.printStackTrace();
        }

        return "";
    }

}

