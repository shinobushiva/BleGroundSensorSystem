package me.shingaki.blesensorgroundsystem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private BluetoothManager mBleManager;
    private BluetoothAdapter mBleAdapter;
    private boolean mIsBluetoothEnable = false;
    private BluetoothLeScanner mBleScanner;
    private BluetoothGatt mBleGatt1;
    private BluetoothGatt mBleGatt2;
    private BluetoothGattCharacteristic mBleCharacteristic1;
    private BluetoothGattCharacteristic mBleCharacteristic2;
    private int lastX1 = 0;
    private int lastX2 = 0;

    final Handler mHandler = new Handler();
    private Runnable mTimer1;
    private Runnable mTimer2;

    private LineGraphSeries<DataPoint> mSeries1;
    private LineGraphSeries<DataPoint> mSeries2;

    private int temperatureGraphValue = 0;
    private int humidityGraphValue = 0;

    Location location;
    private Button stopButton;
    private Button startButton;

    private TextView mTextTemperature;
    private TextView mTextHumidity;
    private Timer mTimer;
    private ParseObject sensorReportObject;
    private BluetoothDevice device;
    private LocationService locationService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextTemperature = (TextView) findViewById(R.id.text_temperature);
        mTextHumidity = (TextView) findViewById(R.id.text_humidity);


        locationService = new LocationService(this);

        // Bluetoothの使用準備.
        mBleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = mBleManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBleAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // BLEが使用可能ならスキャン開始.
        this.startScanByBleScanner();

        GraphView graph = (GraphView) findViewById(R.id.graph);
        mSeries1 = new LineGraphSeries<DataPoint>();
        mSeries2 = new LineGraphSeries<DataPoint>();
        graph.addSeries(mSeries1);
        graph.addSeries(mSeries2);
        mSeries2.setColor(Color.RED);

        Viewport viewport = graph.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setScrollable(true);
        viewport.setMinX(0);
        viewport.setMaxX(20);
        viewport.setMinY(0);
        viewport.setMaxY(100);


        startButton = (Button) findViewById(R.id.button_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

//                locationService.startLocationService();

                try {
                    sensorReportObject = uploadSensorReport(new Date(), "熊本", location, "晴れ");
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                mTimer = new Timer("ParseTimer");
                mTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        uploadSensorValue(humidityGraphValue, "humidity");
                        uploadSensorValue(temperatureGraphValue, "temperature");
                        Log.d(TAG, "ParseTimer");
                    }
                },
                new Date(),
                2000);
            }
        });

        stopButton = (Button) findViewById(R.id.button_stop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mTimer.cancel();
                mTimer = null;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startScanByBleScanner()
    {
        mBleScanner = mBleAdapter.getBluetoothLeScanner();

        // デバイスの検出.
        mBleScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                // スキャン中に見つかったデバイスに接続を試みる.第三引数には接続後に呼ばれるBluetoothGattCallbackを指定する.
                device = result.getDevice();

                device.connectGatt(getApplicationContext(), false, mGattCallback);
                device.connectGatt(getApplicationContext(), false, mGattCallback2);
            }
            @Override
            public void onScanFailed(int intErrorCode)
            {
                super.onScanFailed(intErrorCode);
            }
        });
    }

    private final BluetoothGattCallback mGattCallback = new MyBluetoothGattCallback();



    private final BluetoothGattCallback mGattCallback2 = new MyBluetoothGattCallback();


    // add random data to graph
    private void addSeries1Entry(Integer data) {
        // here, we choose to display max 10 points on the viewport and we scroll to end
        int plotData = (int) ((data + 0.0) / 70 * 100);
        mSeries1.appendData(new DataPoint(lastX1++, plotData), true, 300);
        Log.d(TAG, "addSeries1Entry");
    }

    // add random data to graph
    private void addSeries2Entry(Integer data) {
        // here, we choose to display max 10 points on the viewport and we scroll to end
        int plotData = (int) ((data + 0.0) / 1000 * 100);
        mSeries2.appendData(new DataPoint(lastX2++, plotData), true, 300);
        Log.d(TAG, "addSeries2Entry");
    }

    private ParseObject uploadSensorReport(Date date, String place, Location location, String weather) throws ParseException {
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

    private void uploadSensorValue(Integer data, String type)
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

    private class MyBluetoothGattCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {            // 接続状況が変化したら実行.
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // 接続に成功したらサービスを検索する.
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 接続が切れたらGATTを空にする.
//                if (mBleGatt1 != null)
//                {
//                    mBleGatt1.close();
//                    mBleGatt1 = null;
//                    sleep(1000);
//                    device.connectGatt(getApplicationContext(), false, mGattCallback);
//
//                }
//                if (mBleGatt2 != null)
//                {
//                    mBleGatt2.close();
//                    mBleGatt2 = null;
//                    sleep(1000);
//                    device.connectGatt(getApplicationContext(), false, mGattCallback2);
//                }
//                mIsBluetoothEnable = false;
                gatt.connect();
                Log.d(TAG, "gatt.connect");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
            // キャラクタリスティックのUUIDをチェック(getUuidの結果が全て小文字で帰ってくるのでUpperCaseに変換)
            if (GroundSensorGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID.equals(characteristic.getUuid().toString().toUpperCase()))
            {
                // Peripheralで値が更新されたらNotificationを受ける.
                //mStrReceivedNum = characteristic.getStringValue(0);
                // メインスレッドでTextViewに値をセットする.
                //mBleHandler.sendEmptyMessage(MESSAGE_NEW_RECEIVEDNUM);

                int format = BluetoothGattCharacteristic.FORMAT_UINT16;
                temperatureGraphValue = characteristic.getIntValue(format, 0);

                Log.d(TAG, "temperature value!! :" + temperatureGraphValue);

                // グラフの表示
                mTimer1 = new Runnable() {
                    @Override
                    public void run() {
                        addSeries1Entry(temperatureGraphValue);
                        mTextTemperature.setText("" + temperatureGraphValue);
                    }
                };
                mHandler.postDelayed(mTimer1, 100);
            }

            // キャラクタリスティックのUUIDをチェック(getUuidの結果が全て小文字で帰ってくるのでUpperCaseに変換)
            if (GroundSensorGattAttributes.HUMIDITY_CHARACTERISTIC_UUID.equals(characteristic.getUuid().toString().toUpperCase()))
            {
                int format = BluetoothGattCharacteristic.FORMAT_UINT16;
                humidityGraphValue = characteristic.getIntValue(format, 0);

                Log.d(TAG, "humidity value!! :" + humidityGraphValue);

                // グラフの表示
                mTimer2 = new Runnable() {
                    @Override
                    public void run() {
                        addSeries2Entry(humidityGraphValue);
                        mTextHumidity.setText("" + humidityGraphValue);
                    }
                };
                mHandler.postDelayed(mTimer2, 100);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // Serviceが見つかったら実行.
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // UUIDが同じかどうかを確認する.
                BluetoothGattService service = gatt.getService(UUID.fromString(GroundSensorGattAttributes.SERVICE_UUID));
                if (service != null)
                {
                    // 指定したUUIDを持つCharacteristicを確認する.
                    mBleCharacteristic1 = service.getCharacteristic(UUID.fromString(GroundSensorGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID));

                    if (mBleCharacteristic1 != null) {
                        Log.d(TAG, "get Characteristic1!!");
                        // Service, CharacteristicのUUIDが同じならBluetoothGattを更新する.
                        mBleGatt1 = gatt;

                        // キャラクタリスティックが見つかったら、Notificationをリクエスト.
                        boolean registered = mBleGatt1.setCharacteristicNotification(mBleCharacteristic1, true);

                        // Characteristic の Notificationを有効化する.
                        BluetoothGattDescriptor descriptor1 = mBleCharacteristic1.getDescriptor(
                                UUID.fromString(GroundSensorGattAttributes.CHARACTERISTIC_CONFIG_UUID));


                        descriptor1.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBleGatt1.writeDescriptor(descriptor1);
                        // 接続が完了したらデータ送信を開始する.
                        mIsBluetoothEnable = true;
                    }

                    // 指定したUUIDを持つCharacteristicを確認する.
                    mBleCharacteristic2 = service.getCharacteristic(UUID.fromString(GroundSensorGattAttributes.HUMIDITY_CHARACTERISTIC_UUID));

                    if (mBleCharacteristic2 != null) {
                        Log.d(TAG, "get Characteristic2!!");
                        // Service, CharacteristicのUUIDが同じならBluetoothGattを更新する.
                        mBleGatt2 = gatt;

                        // キャラクタリスティックが見つかったら、Notificationをリクエスト.
                        boolean registered = mBleGatt2.setCharacteristicNotification(mBleCharacteristic2, true);

                        // Characteristic の Notificationを有効化する.
                        BluetoothGattDescriptor descriptor2 = mBleCharacteristic2.getDescriptor(
                                UUID.fromString(GroundSensorGattAttributes.CHARACTERISTIC_CONFIG_UUID));


                        descriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBleGatt2.writeDescriptor(descriptor2);
                        // 接続が完了したらデータ送信を開始する.
                        mIsBluetoothEnable = true;
                    }
                }
            }
        }

    }

    private void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
