package com.pbartz.heartmonitor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.pbartz.heartmonitor.service.BluetoothLeService;
import com.pbartz.heartmonitor.service.RandomService;

import java.util.ArrayList;
import java.util.logging.Handler;

public class MainActivity extends Activity {

    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public static final int APPLICATION_MODE_PRODUCTION = 1;
    public static final int APPLICATION_MODE_RANDOM = 2;

    private String mDeviceName;
    private String mDeviceAddress = null;
    private BluetoothDevice mDevice;


    //private int mAppMode = APPLICATION_MODE_PRODUCTION;
    private int mAppMode = APPLICATION_MODE_RANDOM;

    private BluetoothLeService mBluetoothLeService;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private boolean mConnected;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private TextView labelStatus;
    private TextView labelValue;

    private Switch switchConnection;

    private BluetoothAdapter mBluetoothAdapter;

    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;

    private static final int SCAN_PERIOD = 10000;

    private long mTimestamp = 0;

    public static final String PREFS_NAME = "pbartzHRMPreferencesFile";

    private CheckBox cbSaved;


    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (mBluetoothLeService.isRunning) {
                switchConnection.setChecked(true);
                labelStatus.setText("Reading Heart Rate");
                labelValue.setText("...");
            }
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth" );
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            Log.i(TAG, "BTS Callback action: " + action);

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                labelStatus.setText("Connected");
                switchConnection.setChecked(true);
                //ToDo
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                labelStatus.setText("Disconnected");
                labelValue.setText("-");
                switchConnection.setChecked(false);
                mConnected = false;
                //ToDo
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //ToDo
                if (mBluetoothLeService != null) {
                    labelStatus.setText("Reading Heart Rate");
                    labelValue.setText("...");
                    mBluetoothLeService.turnHRMNotification();
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.i(TAG, "HRM: " + intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
                labelValue.setText(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
                //ToDo
            }

        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.i(TAG, "New INTENTDDD!!!");

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Device Found: " + device.getName() + " / " + device.getAddress());
                    if (device != null && device.getName() != null && device.getName().equals("HRM")) {
                        mScanning = false;
                        mDeviceName = device.getName();
                        mDeviceAddress = device.getAddress();
                        mDevice = device;
                        saveDevice(device);
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        connectDevice();
                    } else {

                        if (isTimeAlready(SCAN_PERIOD)) {
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            Log.i(TAG, "Stop BT scan. timeout");
                            switchConnection.setChecked(false);
                            labelStatus.setText("HRM device not found");
                        }

                    }
                }
            });

        }
    };

    public MainActivity() {

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "MainActivty Created!");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        if (mAppMode == APPLICATION_MODE_PRODUCTION) {

            initBt();

        } else if(mAppMode == APPLICATION_MODE_RANDOM) {

            initRandomAdaptor();

        }

        loadPreferences();

    }

    private void initRandomAdaptor() {

    }

    private void loadPreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mDeviceAddress = settings.getString("deviceAddress", null);
        mDeviceName = settings.getString("deviceName", null);

        if (mDeviceAddress != null) {
            cbSaved.setVisibility(CheckBox.VISIBLE);
        }
    }

    private void saveDevice(BluetoothDevice device) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("deviceAddress", device.getAddress());
        editor.putString("deviceName", device.getName());

        editor.commit();
    }

    private boolean initBt() {

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth not found", Toast.LENGTH_SHORT).show();
            return false;
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "LE Bluetooth not supported", Toast.LENGTH_SHORT);
            return false;
        }

        return true;

    }

    private void initView() {
        labelStatus = (TextView) findViewById(R.id.labelStatus);
        labelValue = (TextView) findViewById(R.id.labelValue);

        cbSaved = (CheckBox) findViewById(R.id.cbSavedDevice);

        cbSaved.setVisibility(CheckBox.INVISIBLE);

        switchConnection = (Switch) findViewById(R.id.switchConnection);

        switchConnection.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean isChecked = switchConnection.isChecked();

                Log.i(TAG, "Switch click!: " + switchConnection.isChecked());

                if (isChecked) {

                    labelStatus.setText("Searching for HRM device");
                    labelValue.setText("-");
                    scanForHRM(true);

                } else {

                    labelStatus.setText("Disconnected");
                    labelValue.setText("-");
                    scanForHRM(false);

                }

            }
        });

//        switchConnection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//
//                labelValue.setText("-");
//
//                if (isChecked) {
//
//                    labelStatus.setText("Searching for HRM device");
//                    scanForHRM(true);
//
//                } else {
//
//                    labelStatus.setText("Disconnected");
//                    scanForHRM(false);
//
//                }
//
//            }
//        });

    }

    private void startTimeCounter() {
        mTimestamp = System.currentTimeMillis();
    }

    private boolean isTimeAlready(int mkSeconds) {
        return System.currentTimeMillis() - mTimestamp > mkSeconds ? true : false;
    }

    private void scanForHRM(final boolean enable) {

        if (mAppMode != APPLICATION_MODE_PRODUCTION) {
            if (enable) {
                mRandomService.connect("RANDOM", this);
            } else {
                mRandomService.disconnect();
            }
            return;
        }

        if (enable) {

            if (mDeviceAddress == null || !cbSaved.isChecked()) {
                mScanning = true;
                startTimeCounter();
                if (!cbSaved.isChecked()) {
                    cbSaved.setVisibility(CheckBox.INVISIBLE);
                }
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                Log.i(TAG, "Connect to stored device: " + mDeviceAddress);
                connectDevice();
            }
        } else {

            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            if (mBluetoothLeService != null) {
                mBluetoothLeService.disconnect();
                mBluetoothLeService.close();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mAppMode == APPLICATION_MODE_PRODUCTION) {

            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);

            startService(gattServiceIntent);

            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        } else {

            Intent fakeServiceIntent = new Intent(this, RandomService.class);

            startService(fakeServiceIntent);

            bindService(fakeServiceIntent, mFakeServiceConnection, BIND_AUTO_CREATE);


        }
    }

    private void connectDevice() {
        if (mBluetoothLeService != null) {
            cbSaved.setVisibility(CheckBox.VISIBLE);
            cbSaved.setChecked(true);
            //final boolean result = mBluetoothLeService.connect(mDeviceAddress, this);
            //Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAppMode == APPLICATION_MODE_PRODUCTION) {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        } else {
            registerReceiver(mRandomUpdateReceiver, makeGattUpdateIntentFilter());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mAppMode == APPLICATION_MODE_PRODUCTION) {
            unregisterReceiver(mGattUpdateReceiver);
        } else {
            unregisterReceiver(mRandomUpdateReceiver);
        }
    }

    @Override
    protected void onDestroy() {

        Log.i(TAG, "Application DESTROUED!");

        if (mAppMode == APPLICATION_MODE_PRODUCTION) {

            unbindService(mServiceConnection);
            if (mBluetoothLeService != null) {
                mBluetoothLeService.stopIfNotRunning();
            }

        } else {

            unbindService(mFakeServiceConnection);


        }

        super.onDestroy();

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


    private static IntentFilter makeGattUpdateIntentFilter() {

        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);

        return intentFilter;

    }


    private RandomService mRandomService;

    private final ServiceConnection mFakeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRandomService = ((RandomService.LocalBinder) service).getService();
            if (mRandomService.isRunning) {
                switchConnection.setChecked(true);
                labelStatus.setText("Reading Random Numbers");
            }
            if (!mRandomService.initialize()) {
                Log.e(TAG, "Unable to initialize Random Service" );
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRandomService = null;
        }
    };

    private final BroadcastReceiver mRandomUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            Log.i(TAG, "BTS Callback action: " + action);

            if (RandomService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                labelStatus.setText("Connected");
                switchConnection.setChecked(true);
                //ToDo
            } else if (RandomService.ACTION_GATT_DISCONNECTED.equals(action)) {
                labelStatus.setText("Disconnected");
                labelValue.setText("-");
                switchConnection.setChecked(false);
                mConnected = false;
                //ToDo
            } else if (RandomService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //ToDo
                if (mBluetoothLeService != null) {
                    labelStatus.setText("Reading Random Number");
                    labelValue.setText("...");
                }

            } else if (RandomService.ACTION_DATA_AVAILABLE.equals(action)) {

                Log.i(TAG, "HRMrandom: " + intent.getStringExtra(RandomService.EXTRA_DATA));
                labelValue.setText(intent.getStringExtra(RandomService.EXTRA_DATA));
                //ToDo
            }

        }
    };



}
