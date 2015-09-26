package me.shingaki.blesensorgroundsystem;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by shiva on 15/09/17.
 */
public class SensorReportViewActivity extends Activity {


    private final static String TAG = SensorReportViewActivity.class.getSimpleName();


    private ParseService mParseService;

    private TextView mTextPlace;
    private TextView mTextDateTime;
    private TextView mTextWeather;
    private TextView mTextTemperature;
    private TextView mTextHumidity;
    private TextView mTextResult;
    private TextView mTextGps;

    private GraphView mGraph;
    private ProgressDialog mProgress;

    private TextView mTextViewBLEConnected;
    private TextView mTextViewMeasureing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensoring);


        mTextPlace = (TextView)findViewById(R.id.text_place);
        mTextDateTime = (TextView)findViewById(R.id.text_datetime);
        mTextWeather = (TextView)findViewById(R.id.text_weather);
        mTextTemperature = (TextView)findViewById(R.id.text_temperature);
        mTextHumidity = (TextView)findViewById(R.id.text_humidity);
        mTextResult = (TextView)findViewById(R.id.text_result);
        mTextGps = (TextView)findViewById(R.id.text_gps);

        mTextViewBLEConnected = (TextView)findViewById(R.id.textView_ble_connected);
        mTextViewBLEConnected.setBackgroundColor(Color.YELLOW);
        mTextViewBLEConnected.setVisibility(View.INVISIBLE);
        mTextViewMeasureing = (TextView)findViewById(R.id.textView_measuring);
        mTextViewMeasureing.setBackgroundColor(Color.YELLOW);
        mTextViewMeasureing.setVisibility(View.INVISIBLE);

        mGraph = (GraphView) findViewById(R.id.graph);

        findViewById(R.id.button_start).setVisibility(View.INVISIBLE);
        findViewById(R.id.button_stop).setVisibility(View.INVISIBLE);
        mTextResult.setVisibility(View.INVISIBLE);

        mParseService = new ParseService(this);


        //今回は使ってないけど、飛んできたインテントをゲットしている
        Bundle extras = getIntent().getExtras();
        String oid = extras.getString("ObjectId");

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Loading...");
        mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgress.show();

        Log.d(TAG, "Oid:"+oid);
        mParseService.get("SensorReport", oid, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                Log.d(TAG, "SensorReport Load");
                String place = parseObject.getString("place");
                Date date = parseObject.getDate("current_time");
                String weather = parseObject.getString("weather");
                ParseGeoPoint location = parseObject.getParseGeoPoint("location");
                String oid = parseObject.getObjectId();

                mTextPlace.setText(place);
                mTextWeather.setText(weather);
                mTextDateTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
                mTextGps.setText(""+location.getLatitude()+","+location.getLongitude());


                ParseQuery<ParseObject> query = ParseQuery.getQuery("SensorValue")
                        .whereEqualTo("report", parseObject)
                        .orderByAscending("current_time");
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> list, ParseException e) {

                        Log.d(TAG, "SensorValues Load");

                        LineGraphSeries<DataPoint> mSeries1 = new LineGraphSeries<DataPoint>();
                        LineGraphSeries<DataPoint> mSeries2 = new LineGraphSeries<DataPoint>();
                        mGraph.removeAllSeries();

                        mGraph.addSeries(mSeries1);
                        mGraph.addSeries(mSeries2);
                        mSeries2.setColor(Color.RED);


                        Viewport viewport = mGraph.getViewport();
                        viewport.setXAxisBoundsManual(true);
                        viewport.setYAxisBoundsManual(true);
//                        viewport.setScrollable(true);
                        viewport.setMinX(1);
                        viewport.setMinY(0);
                        viewport.setMaxY(100);

                        int lastX1=0;
                        int lastX2=0;

                        float temp=0;
                        float humid=0;

                        for(ParseObject po : list){
                            int value = po.getInt("value");
                            String type = po.getString("type");
                            ParseGeoPoint location = po.getParseGeoPoint("location");

                            if("temperature".equals(type)) {
                                lastX1++;
                                temp+=value;
                            }

                            if("humidity".equals(type)){
                                lastX2++;
                                humid+=value;
                            }
                        }

                        viewport.setMaxX(Math.max(lastX1, lastX2));
                        mTextTemperature.setText("" + Math.round(temp / lastX1));
                        mTextHumidity.setText("" + Math.round(humid / lastX2));

                         lastX1=0;
                         lastX2=0;

                        for(ParseObject po : list){
                            int value = po.getInt("value");
                            String type = po.getString("type");
                            ParseGeoPoint location = po.getParseGeoPoint("location");

                            if("temperature".equals(type)) {
                                addSeriesEntry(mSeries1, value, lastX1, 0, 70);
                                lastX1++;
                            }

                            if("humidity".equals(type)){
                                addSeriesEntry(mSeries2, value, lastX2, 0, 1000);
                                lastX2++;
                            }
                        }

                        mProgress.dismiss();

                    }
                });


            }
        });


        //今回は使ってないけど、レイアウトのidからviewの情報をゲットしている
        //RelativeLayout rl = (RelativeLayout)findViewById(R.id.RelativeLayout0);

    }

    // add random data to graph
    private void addSeriesEntry(LineGraphSeries<DataPoint> series, Integer data, int num, float offset, float divBy) {
        // here, we choose to display max 10 points on the viewport and we scroll to end
        int plotData = (int) ((data + offset) / divBy * 100);
        series.appendData(new DataPoint(num, plotData), true, 300);
    }
}
