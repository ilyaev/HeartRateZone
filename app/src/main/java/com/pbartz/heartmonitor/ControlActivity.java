package com.pbartz.heartmonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pbartz.heartmonitor.service.BluetoothLeService;
import com.pbartz.heartmonitor.service.RandomService;
import com.pbartz.heartmonitor.view.Button;
import com.pbartz.heartmonitor.view.ZoneGauge;
import com.pbartz.heartmonitor.view.ZoneProgress;
import com.pbartz.heartmonitor.zone.Config;

public class ControlActivity extends AppCompatActivity {

    /* BT fields start */

    private final static String TAG = ControlActivity.class.getSimpleName();

    public static final int APPLICATION_MODE_PRODUCTION = 1;
    public static final int APPLICATION_MODE_RANDOM = 2;

    private CheckBox cbSaved;

    private String mDeviceAddress = null;

    private int mAppMode = APPLICATION_MODE_PRODUCTION;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;

    private static final int SCAN_PERIOD = 10000;
    private long mTimestamp = 0;
    public static final String PREFS_NAME = "pbartzHRMPreferencesFile";

    /* Bt Fields end */

    public static final int MODE_DISCONNECTED = 0;
    public static final int MODE_CONNECTING = 1;
    public static final int MODE_CONNECTED = 2;

    public int state = MODE_DISCONNECTED;

    ZoneGauge viewGauge;
    ZoneProgress viewProgress;
    View viewChart;

    RelativeLayout layoutOff;
    RelativeLayout layoutOn;

    android.widget.ImageButton btnPlay;
    android.widget.ImageButton btnSettings;
    android.widget.ImageButton btnAudio;

    TextView labelStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_control);

        initView();
        Config.init(185);

        if (mAppMode == APPLICATION_MODE_PRODUCTION) {

            initBt();

        } else if(mAppMode == APPLICATION_MODE_RANDOM) {

            initRandomAdaptor();

        }

        loadPreferences();

    }

    private void initView() {

        viewGauge = (ZoneGauge) findViewById(R.id.viewGauge);
        viewProgress = (ZoneProgress) findViewById(R.id.viewProgress);
        viewChart = findViewById(R.id.viewChart);

        btnPlay = (android.widget.ImageButton) findViewById(R.id.btnPlay);
        btnSettings = (android.widget.ImageButton) findViewById(R.id.btnSettings);
        btnAudio = (android.widget.ImageButton) findViewById(R.id.btnAudio);

        layoutOff = (RelativeLayout) findViewById(R.id.layoutOff);
        layoutOn = (RelativeLayout) findViewById(R.id.layoutOn);

        labelStatus = (TextView) findViewById(R.id.labelStatus);


        btnPlay.setOnClickListener(onPlayBtnClickListener);
        btnSettings.setOnClickListener(onSettingsBtnClickListener);
        btnAudio.setOnClickListener(onAudioBtnClickListener);

        cbSaved = (CheckBox) findViewById(R.id.cbSaved);

        cbSaved.setVisibility(CheckBox.INVISIBLE);

    }

    private void setActivityState(int state) {

        this.state = state;

        applyState();

    }

    public void applyState() {
        if (state != MODE_CONNECTED) {

            layoutOff.setVisibility(View.VISIBLE);
            layoutOn.setVisibility(View.INVISIBLE);

        } else {

            layoutOff.setVisibility(View.INVISIBLE);
            layoutOn.setVisibility(View.VISIBLE);

        }

        if (state == MODE_DISCONNECTED) {
            btnPlay.setImageResource(R.drawable.play_white);
        } else {
            btnPlay.setImageResource(R.drawable.stop_white);
        }

        if (state == MODE_CONNECTING) {
            labelStatus.setText("Connecting...");
        } else if (state == MODE_DISCONNECTED) {
            labelStatus.setText("Disconnected");
        }
    }

    private View.OnClickListener onAudioBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

           btnAudio.setImageResource(viewChart.getVisibility() == View.VISIBLE ? R.drawable.audio_on_white : R.drawable.audio_off_white);

        }
    };

    private View.OnClickListener onSettingsBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    private View.OnClickListener onPlayBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (state != MODE_DISCONNECTED) {
                setActivityState(MODE_DISCONNECTED);
                scanForHRM(false);
            } else {
                setActivityState(MODE_CONNECTING);
                scanForHRM(true);
            }

        }
    };

    /* BT Methods start */

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

           // mScanning = false;
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

    private void initRandomAdaptor() {

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

    private RandomService mRandomService;

    private final ServiceConnection mFakeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRandomService = ((RandomService.LocalBinder) service).getService();
            if (mRandomService.isRunning) {
                setActivityState(MODE_CONNECTED);
                //switchConnection.setChecked(true);
                //labelStatus.setText("Reading Random Numbers");
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
                setActivityState(MODE_CONNECTED);
//                mConnected = true;
//                labelStatus.setText("Connected");
//                switchConnection.setChecked(true);
                //ToDo
            } else if (RandomService.ACTION_GATT_DISCONNECTED.equals(action)) {
                setActivityState(MODE_DISCONNECTED);
//                labelStatus.setText("Disconnected");
//                labelValue.setText("-");
//                switchConnection.setChecked(false);
//                mConnected = false;
                //ToDo
            } else if (RandomService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //ToDo
                if (mBluetoothLeService != null) {
//                    labelStatus.setText("Reading Random Number");
//                    labelValue.setText("...");
                }

            } else if (RandomService.ACTION_DATA_AVAILABLE.equals(action)) {

                Log.i(TAG, "HRMrandom: " + intent.getStringExtra(RandomService.EXTRA_DATA));
                viewProgress.updateHrValue(Integer.valueOf(intent.getStringExtra(RandomService.EXTRA_DATA)));
                viewGauge.updateHrValue(Integer.valueOf(intent.getStringExtra(RandomService.EXTRA_DATA)));
                //labelValue.setText(intent.getStringExtra(RandomService.EXTRA_DATA));
                //ToDo
            }

        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {

        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);

        return intentFilter;

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

    private void startTimeCounter() {
        mTimestamp = System.currentTimeMillis();
    }

    private boolean isTimeAlready(int mkSeconds) {
        return System.currentTimeMillis() - mTimestamp > mkSeconds ? true : false;
    }

    private void connectDevice() {
        if (mBluetoothLeService != null) {
            cbSaved.setVisibility(CheckBox.VISIBLE);
            cbSaved.setChecked(true);
            final boolean result = mBluetoothLeService.connect(mDeviceAddress, this);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    private void loadPreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mDeviceAddress = settings.getString("deviceAddress", null);

        if (mDeviceAddress != null) {
            cbSaved.setVisibility(CheckBox.VISIBLE);
            cbSaved.setChecked(true);
        }
    }

    private void saveDevice(BluetoothDevice device) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("deviceAddress", device.getAddress());
        editor.putString("deviceName", device.getName());

        editor.commit();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Device Found: " + device.getName() + " / " + device.getAddress());
                    if (device != null && device.getName() != null && device.getName().equals("HRM")) {
                        mDeviceAddress = device.getAddress();
                        saveDevice(device);
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        connectDevice();
                    } else {

                        if (isTimeAlready(SCAN_PERIOD)) {
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            Log.i(TAG, "Stop BT scan. timeout");
                            setActivityState(MODE_DISCONNECTED);
                            labelStatus.setText("HRM device not found");
                        }

                    }
                }
            });

        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            Log.i(TAG, "BTS Callback action: " + action);

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                labelStatus.setText("Connected. Preparing.");
                //ToDo
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                setActivityState(MODE_DISCONNECTED);
                //ToDo
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //ToDo
                if (mBluetoothLeService != null) {
                    labelStatus.setText("Reading Heart Rate");
                    mBluetoothLeService.turnHRMNotification();
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (state != MODE_CONNECTED) {
                    setActivityState(MODE_CONNECTED);
                }

                Log.i(TAG, "HRM: " + intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));

                viewProgress.updateHrValue(Integer.valueOf(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA)));
                viewGauge.updateHrValue(Integer.valueOf(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA)));

                //ToDo
            }

        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (mBluetoothLeService.isRunning) {
//                switchConnection.setChecked(true);
//                labelStatus.setText("Reading Heart Rate");
//                labelValue.setText("...");
            }
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    /* Bt Methoids End */

}
