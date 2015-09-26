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
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

/**
 * Created by shiva on 15/09/17.
 */
public class BLEService {

    private final static String TAG = BLEService.class.getSimpleName();

    private MainActivity activity;
    private View view;

    private BluetoothManager mBleManager;
    private BluetoothAdapter mBleAdapter;
    private boolean mIsBluetoothEnable = false;
    private BluetoothLeScanner mBleScanner;

    private BluetoothGatt mBleGatt1;
    private BluetoothGatt mBleGatt2;
    private BluetoothDevice device;

    private TextView mTextViewBLEConnected;
    private TextView mTextViewMeasureing;


    private final BluetoothGattCallback mGattCallback = new MyBluetoothGattCallback(0);
    private final BluetoothGattCallback mGattCallback2 = new MyBluetoothGattCallback(1);

    private int temperature = 0;
    private int humidity = 0;


    boolean temperatureReceived = false;
    boolean humidityRecieved = false;

    private Handler mHandler = new Handler();

    public int getTemperture(){
        return temperature;
    }

    public int getHumidity(){
        return humidity;
    }

    public void setView(View view){
        this.view = view;

        mTextViewBLEConnected = (TextView)view.findViewById(R.id.textView_ble_connected);
        mTextViewMeasureing = (TextView)view.findViewById(R.id.textView_measuring);

        // Bluetoothの使用準備.
        mBleManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = mBleManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBleAdapter == null) {
            Toast.makeText(activity, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            activity.finish();
            return;
        }

        // BLEが使用可能ならスキャン開始.
        this.startScanByBleScanner();
    }

    public BLEService(MainActivity activity) {
        this.activity = activity;
    }

    public void disconnect() {
        if(mBleGatt1 != null)
            mBleGatt1.disconnect();
        if(mBleGatt2 != null)
            mBleGatt2.disconnect();
        if(mBleScanner != null)
            mBleScanner.stopScan(new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                }
            });

        mTextViewBLEConnected.setBackgroundColor(Color.YELLOW);

        temperatureReceived = false;
        humidityRecieved = false;
    }


    public void startScanByBleScanner()
    {
        mBleScanner = mBleAdapter.getBluetoothLeScanner();

        // デバイスの検出.
        mBleScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                // スキャン中に見つかったデバイスに接続を試みる.第三引数には接続後に呼ばれるBluetoothGattCallbackを指定する.
                device = result.getDevice();

                device.connectGatt(activity.getApplicationContext(), false, mGattCallback);
                device.connectGatt(activity.getApplicationContext(), false, mGattCallback2);
            }

            @Override
            public void onScanFailed(int intErrorCode) {
                super.onScanFailed(intErrorCode);
            }
        });
    }




    private class MyBluetoothGattCallback extends BluetoothGattCallback {

        private int num;

        public MyBluetoothGattCallback(int n){
            num = n;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {            // 接続状況が変化したら実行.
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // 接続に成功したらサービスを検索する.
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
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
                temperature = characteristic.getIntValue(format, 0);

                Log.d(TAG, "temperature value!! :" + temperature);
                temperatureReceived = true;

            }

            // キャラクタリスティックのUUIDをチェック(getUuidの結果が全て小文字で帰ってくるのでUpperCaseに変換)
            if (GroundSensorGattAttributes.HUMIDITY_CHARACTERISTIC_UUID.equals(characteristic.getUuid().toString().toUpperCase()))
            {
                int format = BluetoothGattCharacteristic.FORMAT_UINT16;
                humidity = characteristic.getIntValue(format, 0);

                Log.d(TAG, "humidity value!! :" + humidity);
                humidityRecieved = true;
            }

            if(temperatureReceived && humidityRecieved){
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTextViewBLEConnected.setBackgroundColor(Color.GREEN);
                    }
                });
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
                    if(num == 0) {
                        // 指定したUUIDを持つCharacteristicを確認する.
                        BluetoothGattCharacteristic mBleCharacteristic1 = service.getCharacteristic(UUID.fromString(GroundSensorGattAttributes.TEMPERATURE_CHARACTERISTIC_UUID));
                        Log.d(TAG, "bleC1:" + mBleCharacteristic1);

                        if (mBleCharacteristic1 != null) {
                            Log.d(TAG, "get Characteristic1!!");
                            // Service, CharacteristicのUUIDが同じならBluetoothGattを更新する.
                            mBleGatt1 = gatt;

                            // キャラクタリスティックが見つかったら、Notificationをリクエスト.
                            boolean registered = mBleGatt1.setCharacteristicNotification(mBleCharacteristic1, true);

                            // Characteristic の Notificationを有効化する.
                            BluetoothGattDescriptor descriptor1 = mBleCharacteristic1.getDescriptor(
                                    UUID.fromString(GroundSensorGattAttributes.CHARACTERISTIC_CONFIG_UUID));

                            Log.d(TAG, "desc1:" + descriptor1);
                            if (descriptor1 != null) {
                                descriptor1.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mBleGatt1.writeDescriptor(descriptor1);
                                // 接続が完了したらデータ送信を開始する.
                                mIsBluetoothEnable = true;
                            }

                        }
                    }

                    if(num == 1) {
                        // 指定したUUIDを持つCharacteristicを確認する.
                        BluetoothGattCharacteristic mBleCharacteristic2 = service.getCharacteristic(UUID.fromString(GroundSensorGattAttributes.HUMIDITY_CHARACTERISTIC_UUID));
                        Log.d(TAG, "bleC2:" + mBleCharacteristic2);

                        if (mBleCharacteristic2 != null) {
                            Log.d(TAG, "get Characteristic2!!");
                            // Service, CharacteristicのUUIDが同じならBluetoothGattを更新する.
                            mBleGatt2 = gatt;

                            // キャラクタリスティックが見つかったら、Notificationをリクエスト.
                            boolean registered = mBleGatt2.setCharacteristicNotification(mBleCharacteristic2, true);

                            // Characteristic の Notificationを有効化する.
                            BluetoothGattDescriptor descriptor2 = mBleCharacteristic2.getDescriptor(
                                    UUID.fromString(GroundSensorGattAttributes.CHARACTERISTIC_CONFIG_UUID));

                            Log.d(TAG, "desc2:" + descriptor2);
                            if (descriptor2 != null) {
                                descriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mBleGatt2.writeDescriptor(descriptor2);
                                // 接続が完了したらデータ送信を開始する.
                                mIsBluetoothEnable = true;
                            }
                        }
                    }
                }
            }
        }

    }

//    private void sleep(long t) {
//        try {
//            Thread.sleep(t);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

}