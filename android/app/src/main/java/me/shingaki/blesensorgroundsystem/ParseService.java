package me.shingaki.blesensorgroundsystem;

import android.app.Activity;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.Date;
import java.util.List;

/**
 * Created by shiva on 15/09/17.
 */
public class ParseService {


    private final static String TAG = LocationService.class.getSimpleName();

    private Activity activity;

    public ParseService(Activity activity) {
        this.activity = activity;
    }


    public ParseObject uploadSensorReport(Date date, String place, Location location, String weather) throws ParseException {
        // Parseにデータを送る
        ParseObject bleObject = new ParseObject("SensorReport");
        if(location != null) {
            ParseGeoPoint point = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            bleObject.put("location", point);
        }else {
            bleObject.put("location", new ParseGeoPoint(0, 0));
        }

        bleObject.put("place", place);
        bleObject.put("weather", weather);
        bleObject.put("current_time", date);
        bleObject.save();

        return bleObject;
    }

    public void uploadSensorValue(Integer data, String type, Location location, ParseObject sensorReportObject)
    {
        // Parseにデータを送る
        ParseObject bleObject = new ParseObject("SensorValue");
        if(location != null) {
            ParseGeoPoint point = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            bleObject.put("location", point);
        }else {
            bleObject.put("location", new ParseGeoPoint(0, 0));
        }
        bleObject.put("report", sensorReportObject);
        bleObject.put("value", data);
        bleObject.put("type", type);
        bleObject.put("current_time", new Date());
        bleObject.saveInBackground();
    }

    public void listSensorReport(FindCallback<ParseObject> fc) {

            ParseQuery<ParseObject> query = ParseQuery.getQuery("SensorReport").orderByDescending("current_time");
            query.findInBackground(fc);


//        query.getInBackground("xWMyZ4YEGZ", new GetCallback<ParseObject>() {
//            public void done(ParseObject object, ParseException e) {
//                if (e == null) {
//                    // object will be your game score
//                } else {
//                    // something went wrong
//                }
//            }
//        });
    }

    /**
     * マーカーのタイトルと座標を保存する
     * @param title
     * @param latLng
     * @throws ParseException
     */
    public void uploadMarker(String title, LatLng latLng, SaveCallback sc) throws ParseException {
        ParseObject object = new ParseObject("MapMarker");
        object.put("title", title);
        object.put("latlng", new ParseGeoPoint(latLng.latitude, latLng.longitude));
        object.saveInBackground(sc);
    }

    /**
     * マーカーリストを取得する
     * @param fc
     */
    public void listMarker(FindCallback<ParseObject> fc) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("MapMarker").orderByDescending("createdAt");
        query.findInBackground(fc);
    }

    /**
     * 指定のマーカーから指定範囲内のマーカーを探す
     * @param latLng 緯度/経度
     * @param distance latLngからの距離範囲(km)
     * @param fc クエリ完了時のコールバック
     */
    public void searchMapMarker(LatLng latLng, double distance, FindCallback<ParseObject> fc) {
        ParseGeoPoint point = new ParseGeoPoint(latLng.latitude, latLng.longitude);
        ParseQuery<ParseObject> query =
                ParseQuery.getQuery("MapMarker")
                    .whereWithinKilometers("latlng", point, distance);
        query.findInBackground(fc);
    }

    public void getSensorReport(String oid, GetCallback<ParseObject> gc){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("SensorReport");
        query.getInBackground(oid, gc);
    }

    public void get(String clazz, String oid, GetCallback<ParseObject> gc){
        ParseQuery<ParseObject> query = ParseQuery.getQuery(clazz);
        query.getInBackground(oid, gc);
    }

    public void list(ParseQuery<ParseObject> query, FindCallback<ParseObject> fc){
        query.findInBackground(fc);
    }

}
