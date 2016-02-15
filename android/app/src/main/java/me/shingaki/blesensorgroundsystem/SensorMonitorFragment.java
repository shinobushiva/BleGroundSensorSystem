package me.shingaki.blesensorgroundsystem;

import android.app.Fragment;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by shiva on 15/09/17.
 */
public class SensorMonitorFragment extends Fragment{


    private final static String TAG = SensorMonitorFragment.class.getSimpleName();


    private Button mStopButton;
    private Button mStartButton;

    private TextView mTextDatetime;

    private Timer mTimerGraphUpdate;
    private Timer mTimerParseUpload;

    private MainActivity activity;

    private TextView mTextViewBLEConnected;
    private TextView mTextViewMeasureing;
    private TextView mTextResult;


    protected LocationService mLocationService;
    protected BLEService mBleService;

    private ParseService mParseService;

    private Handler mHandler = new Handler();

    public SensorMonitorFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        activity = (MainActivity)getActivity();
        mLocationService = activity.mLocationService;
        mParseService = activity.mParseService;
        mBleService = activity.mBleService;

        View view =  inflater.inflate(R.layout.activity_sensoring, container, false);
        mBleService.setView(view);

        mTextDatetime = (TextView)view.findViewById(R.id.text_datetime);
        mTextDatetime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        mTextViewBLEConnected = (TextView)view.findViewById(R.id.textView_ble_connected);
        mTextViewBLEConnected.setBackgroundColor(Color.YELLOW);
        mTextViewMeasureing = (TextView)view.findViewById(R.id.textView_measuring);
        mTextViewMeasureing.setBackgroundColor(Color.LTGRAY);

        mTextResult = (TextView)view.findViewById(R.id.text_result);


        mLocationService.setView(view);

        Log.d(TAG, "" + activity.getClass());

        mStartButton = (Button) view.findViewById(R.id.button_start);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startSensorReporting();
            }
        });

        mStopButton = (Button) view.findViewById(R.id.button_stop);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopSensorReporting();
            }
        });

        mTimerGraphUpdate = new Timer("GraphUpdate");
        mTimerGraphUpdate.scheduleAtFixedRate(new GraphUpdateTask(this, view), 0, 2000);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        //may need to manage timer
    }

    @Override
    public void onPause() {
        super.onPause();
        //may need to manage timer
    }


    int count = 0;
    double humiditySum;
    double tempertureSum;

    private void startSensorReporting() {
        try {
            mTextDatetime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            final ParseObject sensorReportObject = mParseService.uploadSensorReport(
                    new Date(),
                    mLocationService.getPlace(),
                    mLocationService.getLocation(),
                    mLocationService.getWeather()
            );
            count = 0;
            tempertureSum = 0;
            humiditySum = 0;

            mTimerParseUpload = new Timer("ParseUpload");
            mTimerParseUpload.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Location location = mLocationService.getLocation();

                    count++;
                    tempertureSum += mBleService.getTemperture();
                    humiditySum += mBleService.getHumidity();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {

                            long temperatureAve = Math.round(tempertureSum / count);
                            long humidityAve = Math.round(humiditySum / count);

                            mTextResult.setText("平均温度:" + temperatureAve + "/ 平均湿度：" + humidityAve);

                        }
                    });
                    mParseService.uploadSensorValue(mBleService.getTemperture(), "temperature", location, sensorReportObject);
                    mParseService.uploadSensorValue(mBleService.getHumidity(), "humidity", location, sensorReportObject);
                    Log.d(TAG, "ParseTimer");

                }

            }, new Date(), 2000);

            mTextViewMeasureing.setBackgroundColor(Color.GREEN);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    private void stopSensorReporting() {
        mTimerParseUpload.cancel();
        mTimerParseUpload = null;



        mTextViewMeasureing.setBackgroundColor(Color.LTGRAY);
    }

}
