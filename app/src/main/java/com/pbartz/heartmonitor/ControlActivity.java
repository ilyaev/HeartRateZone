package com.pbartz.heartmonitor;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pbartz.heartmonitor.fragment.SettingsFragment;
import com.pbartz.heartmonitor.service.BluetoothLeService;
import com.pbartz.heartmonitor.service.RandomService;
import com.pbartz.heartmonitor.view.Button;
import com.pbartz.heartmonitor.view.SpinnerView;
import com.pbartz.heartmonitor.view.StatusView;
import com.pbartz.heartmonitor.view.Transition;
import com.pbartz.heartmonitor.view.ZoneChart;
import com.pbartz.heartmonitor.view.ZoneGauge;
import com.pbartz.heartmonitor.view.ZoneProgress;
import com.pbartz.heartmonitor.zone.Chart;
import com.pbartz.heartmonitor.zone.Config;


public class ControlActivity extends FragmentActivity implements SettingsFragment.SettingsDialogListener {

    /* BT fields start */

    private final static String TAG = ControlActivity.class.getSimpleName();

    public static final int APPLICATION_MODE_PRODUCTION = 1;
    public static final int APPLICATION_MODE_RANDOM = 2;

    private CheckBox cbSaved;

    private String mDeviceAddress = null;
    private String mDeviceName = null;

    private int mAppMode = APPLICATION_MODE_PRODUCTION;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    SettingsFragment settingsFragment;

    public DisplayMetrics displayMetrics;

    public boolean audioMode = true;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;

    private static final int SCAN_PERIOD = 10000;
    private long mTimestamp = 0;
    public static final String PREFS_NAME = "pbartzHRMPreferencesFile";

    public String btnColorDown =  "#3b4662";// "#B70312";
    public String btnColorUp = "#ff5200";

    /* Bt Fields end */

    public static final int MODE_DISCONNECTED = 0;
    public static final int MODE_CONNECTING = 1;
    public static final int MODE_CONNECTED = 2;
    public static final int MODE_SERVICE_DISCOVERED = 3;

    private Chart.HrDataSet dataSet = null;

    public int state = MODE_DISCONNECTED;

    ZoneGauge viewGauge;
    public ZoneProgress viewProgress;
    ZoneChart viewChart;
    Transition viewTransition;
    StatusView statusView;
    SpinnerView viewLoader;

    RelativeLayout layoutOff;
    RelativeLayout layoutOn;

    android.widget.ImageButton btnPlay;
    android.widget.ImageButton btnSettings;
    android.widget.ImageButton btnAudio;

    TextView labelStatus;
    private String mSettingsAge;
    private String mSettingsMaxHr;
    private String mSettingsSchema;
    private String mSettingsRestingHr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                        builder.show();
            }
        }

        displayMetrics = getResources().getDisplayMetrics();

        setContentView(R.layout.activity_control);

        initView();

        loadPreferences();

        Config.init(Integer.parseInt(mSettingsMaxHr), Integer.parseInt(mSettingsRestingHr), mSettingsSchema);

        if (mAppMode == APPLICATION_MODE_PRODUCTION) {

            initBt();

        } else if(mAppMode == APPLICATION_MODE_RANDOM) {

            initRandomAdaptor();

        }



    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    private void initView() {

        viewGauge = (ZoneGauge) findViewById(R.id.viewGauge);
        viewProgress = (ZoneProgress) findViewById(R.id.viewProgress);
        viewChart = (ZoneChart) findViewById(R.id.viewChart);
        viewTransition = (Transition) findViewById(R.id.viewTransition);
        statusView = (StatusView) findViewById(R.id.statusView);
        viewLoader = (SpinnerView) findViewById(R.id.viewSpinner);

        viewChart.setParentActivity(this);
        viewGauge.setParentActivity(this);
        viewProgress.setParentActivity(this);

        btnPlay = (android.widget.ImageButton) findViewById(R.id.btnPlay);
        btnSettings = (android.widget.ImageButton) findViewById(R.id.btnSettings);
        btnAudio = (android.widget.ImageButton) findViewById(R.id.btnAudio);

        layoutOff = (RelativeLayout) findViewById(R.id.layoutOff);
        layoutOn = (RelativeLayout) findViewById(R.id.layoutOn);

        labelStatus = (TextView) findViewById(R.id.labelStatus);


        btnPlay.setOnClickListener(onPlayBtnClickListener);
        btnPlay.setOnTouchListener(onPlayBtnTouchListener);

        btnSettings.setOnClickListener(onSettingsBtnClickListener);
        btnSettings.setOnTouchListener(onSettingsBtnTouchListener);

        btnAudio.setOnClickListener(onAudioBtnClickListener);
        btnAudio.setOnTouchListener(onAudioBtnTouchListener);

        cbSaved = (CheckBox) findViewById(R.id.cbSaved);

        cbSaved.setVisibility(CheckBox.INVISIBLE);

    }

    private void setActivityState(int state) {


        this.state = state;

        if (state == MODE_CONNECTED) {
            if (getDataSet() != null) {
                getDataSet().resetTimeStamp();
            }
        }

        applyState();

    }

    public void applyState() {

        if (state != MODE_CONNECTED) {

            if (layoutOff.getVisibility() == View.INVISIBLE) {

                viewTransition.setTargetLayout(layoutOff);
                viewTransition.setSourceLayout(layoutOn);
                viewTransition.startTransition();
                viewProgress.enterAnimation(0, 1);

                viewGauge.animateTo(viewProgress.getGaugeCenter().x, -200, 200, 50);
                viewChart.animateShift(viewChart.getWidth(), 0, 200, 20);

                statusView.setHeartShiftX(statusView.getWidth() / 2 * -1);
                statusView.animateTo(0, 0, 200, 200);

            }

        } else {

            if (layoutOn.getVisibility() == View.INVISIBLE) {

                viewTransition.setTargetLayout(layoutOn);
                viewTransition.setSourceLayout(layoutOff);
                viewTransition.startTransition();

                viewProgress.enterAnimation(200, -1);

                viewGauge.setCenterY(-200);
                viewGauge.animateTo(viewProgress.getGaugeCenter().x, viewProgress.getGaugeCenter().y, 200, 200);

                viewChart.setShiftX(viewChart.getWidth());
                viewChart.animateShift(0, 0, 200, 210);

                statusView.animateTo(statusView.getWidth() / 2 * -1, 0, 200, 0);


            }

            Log.i(TAG, "Gauge IN");
        }

        if (state == MODE_DISCONNECTED) {
            btnPlay.setImageResource(R.drawable.play_white);
        } else {
            btnPlay.setImageResource(R.drawable.stop_white);
        }

        if (state == MODE_CONNECTING) {

            animateSettingsBtnMargin(0, 300, 200);
            labelStatus.setText("Connecting...");

        } else if (state == MODE_DISCONNECTED) {
            labelStatus.setText("Disconnected");

            animateAudioBtnMargin(-70, 400, 50);
            animateSettingsBtnMargin(-70, 400, 0);

        } else if (state == MODE_CONNECTED) {

            animateAudioBtnMargin(0, 300, 300);
            animateSettingsBtnMargin(0, 300, 200);

        }

        if (state == MODE_SERVICE_DISCOVERED) {

            statusView.lightUpHeart(250);

        } else if (state == MODE_CONNECTING) {

            statusView.lightUpHeart(100);


        } else if (state != MODE_CONNECTED) {

            statusView.fadeDownHeart();

        }

        //loader
        if (state == MODE_CONNECTING) {
            viewLoader.startAnimation();
        } else if (state != MODE_SERVICE_DISCOVERED) {
            viewLoader.endAnimation();
        }
    }

    private View.OnClickListener onAudioBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setAudioMode(!audioMode);
            saveAudioMode();
        }
    };

    public void setAudioMode(boolean audioMode) {
        this.audioMode = audioMode;

        btnAudio.setImageResource(audioMode ? R.drawable.audio_on_white : R.drawable.audio_off_white);

        if (mAppMode == APPLICATION_MODE_RANDOM) {
            if (mRandomService != null) {
                mRandomService.setIsMute(!audioMode);
            }

        } else {
            if (mBluetoothLeService != null) {
                mBluetoothLeService.setIsMute(!audioMode);
            }

        }
    }

    private View.OnClickListener onSettingsBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            showSettingsDialog();

        }
    };

    private View.OnTouchListener onAudioBtnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            Log.i(TAG, "" + event.getAction());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                Log.i(TAG, "Play DOWN!");
                animateAudioBtnWeight(2);
                animateAudioBtnHeight(80);
                btnAudio.setBackgroundColor(Color.parseColor(btnColorDown));

            } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {

                Log.i(TAG, "Play UPPP!!!");
                animateAudioBtnWeight(1);
                animateAudioBtnHeight(50);
                btnAudio.setBackgroundColor(Color.parseColor(btnColorUp));
            }


            return false;
        }

    };

    private View.OnTouchListener onSettingsBtnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            Log.i(TAG, "" + event.getAction());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                Log.i(TAG, "Play DOWN!");
                animateSettingsBtnWeight(2);
                animateSettingsBtnHeight(80);
                btnSettings.setBackgroundColor(Color.parseColor(btnColorDown));

            } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {

                Log.i(TAG, "Play UPPP!!!");
                animateSettingsBtnWeight(1);
                animateSettingsBtnHeight(50);
                btnSettings.setBackgroundColor(Color.parseColor(btnColorUp));
            }


            return false;
        }

    };



    private View.OnTouchListener onPlayBtnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            Log.i(TAG, "" + event.getAction());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                Log.i(TAG, "Play DOWN!");
                animatePlayBtnWeight(2);
                animatePlayBtnHeight(80);
                btnPlay.setBackgroundColor(Color.parseColor(btnColorDown));

            } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {

                Log.i(TAG, "Play UPPP!!!");
                animatePlayBtnWeight(1);
                animatePlayBtnHeight(70);
                btnPlay.setBackgroundColor(Color.parseColor(btnColorUp));
            }


            return false;
        }

    };

    private void animatePlayBtnWeight(float weight) {
        ObjectAnimator hyAnimation = ObjectAnimator.ofFloat(this, "playBtnWeight", this.getPlayBtnWeight());
        hyAnimation.setFloatValues(weight);
        hyAnimation.setDuration(200);
        hyAnimation.setInterpolator(new OvershootInterpolator(3));
        hyAnimation.start();
    }

    public void setPlayBtnWeight(float weight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnPlay.getLayoutParams();
        params.weight = weight;
        btnPlay.setLayoutParams(params);
    }

    public float getPlayBtnWeight() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnPlay.getLayoutParams();
        return params.weight;
    }

    public void setAudioBtnMargin(int margin) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnAudio.getLayoutParams();
        params.bottomMargin = margin;
        btnAudio.setLayoutParams(params);
    }

    public int getAudioBtnMargin() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnAudio.getLayoutParams();
        return params.bottomMargin;
    }

    private void animateAudioBtnMargin(int margin, long duration, long delay) {
        ObjectAnimator hyAnimation = ObjectAnimator.ofInt(this, "audioBtnMargin", this.getAudioBtnMargin());
        hyAnimation.setStartDelay(delay);
        hyAnimation.setIntValues(dp2px(margin));
        hyAnimation.setDuration(duration);
        hyAnimation.setInterpolator(new OvershootInterpolator(3));
        hyAnimation.start();
    }



    public void setSettingsBtnMargin(int margin) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnSettings.getLayoutParams();
        params.bottomMargin = margin;
        btnSettings.setLayoutParams(params);
    }

    public int getSettingsBtnMargin() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnSettings.getLayoutParams();
        return params.bottomMargin;
    }

    private void animateSettingsBtnMargin(int margin, long duration, long delay) {
        ObjectAnimator hyAnimation = ObjectAnimator.ofInt(this, "settingsBtnMargin", this.getSettingsBtnMargin());
        hyAnimation.setStartDelay(delay);
        hyAnimation.setIntValues(dp2px(margin));
        hyAnimation.setDuration(duration);
        hyAnimation.setInterpolator(new OvershootInterpolator(3));
        hyAnimation.start();
    }


    public void setPlayBtnHeight(int height) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnPlay.getLayoutParams();
        params.height = height;
        btnPlay.setLayoutParams(params);
    }

    public int getPlayBtnHeight() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnPlay.getLayoutParams();
        return params.height;
    }

    private void animatePlayBtnHeight(int height) {
        ObjectAnimator hyAnimation = ObjectAnimator.ofInt(this, "playBtnHeight", this.getPlayBtnHeight());
        hyAnimation.setIntValues(dp2px(height));
        hyAnimation.setDuration(200);
        hyAnimation.setInterpolator(new OvershootInterpolator(3));
        hyAnimation.start();
    }




    public void setSettingsBtnHeight(int height) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnSettings.getLayoutParams();
        params.height = height;
        btnSettings.setLayoutParams(params);
    }

    public int getSettingsBtnHeight() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnSettings.getLayoutParams();
        return params.height;
    }

    private void animateSettingsBtnHeight(int height) {
        ObjectAnimator hyAnimation = ObjectAnimator.ofInt(this, "settingsBtnHeight", this.getSettingsBtnHeight());
        hyAnimation.setIntValues(dp2px(height));
        hyAnimation.setDuration(200);
        hyAnimation.setInterpolator(new OvershootInterpolator(3));
        hyAnimation.start();
    }




    public void setAudioBtnHeight(int height) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnAudio.getLayoutParams();
        params.height = height;
        btnAudio.setLayoutParams(params);
    }

    public int getAudioBtnHeight() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnAudio.getLayoutParams();
        return params.height;
    }

    private void animateAudioBtnHeight(int height) {
        ObjectAnimator hyAnimation = ObjectAnimator.ofInt(this, "audioBtnHeight", this.getAudioBtnHeight());
        hyAnimation.setIntValues(dp2px(height));
        hyAnimation.setDuration(200);
        hyAnimation.setInterpolator(new OvershootInterpolator(3));
        hyAnimation.start();
    }


    private void animateAudioBtnWeight(float weight) {
        ObjectAnimator hyAnimation = ObjectAnimator.ofFloat(this, "audioBtnWeight", this.getAudioBtnWeight());
        hyAnimation.setFloatValues(weight);
        hyAnimation.setDuration(200);
        hyAnimation.setInterpolator(new OvershootInterpolator(3));
        hyAnimation.start();
    }

    public void setAudioBtnWeight(float weight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnAudio.getLayoutParams();
        params.weight = weight;
        btnAudio.setLayoutParams(params);
    }

    public float getAudioBtnWeight() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnAudio.getLayoutParams();
        return params.weight;
    }

    private void animateSettingsBtnWeight(float weight) {
        ObjectAnimator hyAnimation = ObjectAnimator.ofFloat(this, "settingsBtnWeight", this.getSettingsBtnWeight());
        hyAnimation.setFloatValues(weight);
        hyAnimation.setDuration(200);
        hyAnimation.setInterpolator(new OvershootInterpolator(3));
        hyAnimation.start();
    }

    public void setSettingsBtnWeight(float weight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnSettings.getLayoutParams();
        params.weight = weight;
        btnSettings.setLayoutParams(params);
    }

    public float getSettingsBtnWeight() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)btnSettings.getLayoutParams();
        return params.weight;
    }

    private View.OnClickListener onPlayBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (layoutOn.getVisibility() == View.VISIBLE && layoutOff.getVisibility() == View.VISIBLE) {
                return;
            }

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
                mDeviceName = "RND";
                mDeviceAddress = "A:B:C";
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

    public Chart.HrDataSet getDataSet() {
        return dataSet;
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

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        return true;

    }

    private RandomService mRandomService;

    private final ServiceConnection mFakeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRandomService = ((RandomService.LocalBinder) service).getService();
            mRandomService.setIsMute(!audioMode);

            dataSet = RandomService.dataSet.dataSet;

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
                viewChart.invalidate();
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
        viewLoader.endAnimation();

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

            //cbSaved.setVisibility(CheckBox.VISIBLE);

            cbSaved.setChecked(true);
            final boolean result = mBluetoothLeService.connect(mDeviceAddress, this);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    private void loadPreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mDeviceAddress = settings.getString("deviceAddress", null);
        
        mSettingsAge = settings.getString("settingsAge", "30");
        mSettingsMaxHr = settings.getString("settingsMaxHr", "190");
        mSettingsSchema = settings.getString("settingsSchema", "Strava");
        mSettingsRestingHr = settings.getString("settingsRestingHr", "60");
        audioMode = settings.getBoolean("settingsAudioMode", true);

        setAudioMode(audioMode);

        if (mDeviceAddress != null) {
            //cbSaved.setVisibility(CheckBox.VISIBLE);

            cbSaved.setChecked(true);
        }
    }

    private void forgetDevice() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("deviceAddress", null);
        editor.putString("deviceName", null);
        editor.commit();
        mDeviceAddress = null;
        mDeviceName = null;

        if (state != MODE_DISCONNECTED) {
            setActivityState(MODE_DISCONNECTED);
            scanForHRM(false);
        }

    }

    private void saveDevice(BluetoothDevice device) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        //todo

        editor.putString("deviceAddress", device.getAddress());
        editor.putString("deviceName", device.getName());

        editor.commit();
    }

    private void saveAudioMode() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        //todo

        editor.putBoolean("settingsAudioMode", audioMode);

        editor.commit();
    }

    private void saveSettings(String age, String maxHr, String restingHr, String schema) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("settingsAge", age);
        editor.putString("settingsMaxHr", maxHr);
        editor.putString("settingsRestingHr", restingHr);
        editor.putString("settingsSchema", schema);

        editor.commit();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Device Found: " + device.getName() + " / " + device.getAddress());
                    if (device != null && device.getType() == BluetoothDevice.DEVICE_TYPE_LE && device.getName() != null
                            && (device.getName().toUpperCase().contains("RHYTHM") || device.getName().toUpperCase().contains("ALPHA") || device.getName().toUpperCase().contains("FUSE") || device.getName().toUpperCase().contains("MIO") || device.getName().toUpperCase().contains("HR SENSOR") || device.getName().toUpperCase().contains("BLUEHR") || device.getName().toUpperCase().contains("TICKR") || device.getName().toUpperCase().contains("JABRA PULSE") || device.getName().toUpperCase().contains("ZEPHYR") ||  device.getName().toUpperCase().contains("WAHOO") || device.getName().toUpperCase().contains("HXM BLU") ||  device.getName().toUpperCase().contains("HRM") || device.getName().toUpperCase().contains("POLAR") )) {
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
                    setActivityState(MODE_SERVICE_DISCOVERED);
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (state != MODE_CONNECTED) {
                    setActivityState(MODE_CONNECTED);
                }

                Log.i(TAG, "HRM: " + intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));

                viewProgress.updateHrValue(Integer.valueOf(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA)));
                viewGauge.updateHrValue(Integer.valueOf(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA)));
                viewChart.invalidate();

                //ToDo
            }

        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            mBluetoothLeService.setIsMute(!audioMode);

            dataSet = mBluetoothLeService.dataSet.dataSet;

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

    public int dp2px(float dp) {
        return (int) (dp * displayMetrics.density + 0.5f);
    }

    /* Bt Methoids End */


    private void showSettingsDialog() {

        settingsFragment = new SettingsFragment();
        settingsFragment.show(getFragmentManager(), "Settings");
        settingsFragment.setsAge(mSettingsAge);
        settingsFragment.setsMaxHr(mSettingsMaxHr);
        settingsFragment.setsSchema(mSettingsSchema);
        settingsFragment.setsRestingHr(mSettingsRestingHr);
        settingsFragment.setsDeviceName(mDeviceAddress);
        settingsFragment.setsDeviceAddr(mDeviceAddress);

    }


    @Override
    public void onDialogPositiveClick(DialogFragment dialog, int age, int maxHr, int restingHr, String schema) {
        Config.init(maxHr, restingHr, schema);

        viewProgress.invalidate();
        viewGauge.invalidate();
        saveSettings("" + age, "" + maxHr, "" + restingHr, schema);
        mSettingsAge = "" + age;
        mSettingsMaxHr = "" + maxHr;
        mSettingsSchema = schema;
        mSettingsRestingHr = "" + restingHr;
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        Log.i(TAG, "onCancel Click!");
    }

    @Override
    public void onDeviceForgetClick(DialogFragment dialog) {
        Log.i(TAG, "Forget Click!");
        forgetDevice();
        settingsFragment.setsDeviceAddr("");
        settingsFragment.setsDeviceName("");
        settingsFragment.setDevice("", "");
    }
}
