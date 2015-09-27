package com.pbartz.heartmonitor.service;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.pbartz.heartmonitor.MainActivity;
import com.pbartz.heartmonitor.R;
import com.pbartz.heartmonitor.utils.SampleGattAttributes;
import com.pbartz.heartmonitor.zone.Chart;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by yura.ilyaev on 9/11/2015.
 */
public class BluetoothLeService extends Service {

    private final static String TAG = "BTService@" + Math.round(Math.random() * 1000);

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private static BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private NotificationCompat.Builder mBuilder = null;
    private NotificationManager mNotificationManager = null;

    private TextToSpeech ts;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public static boolean isRunning = false;
    public static boolean isInited = false;
    public static boolean isStarted = false;

    public static Chart dataSet;


    public int mId = 2;

    public final static String ACTION_GATT_CONNECTED = "pbartz.hrm.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "pbartz.hrm.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "pbartz.hrm.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "pbartz.hrm.ACTION_GATT_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "pbartz.hrm.EXTRA_DATA";

    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    @Override
    public void onCreate() {
        super.onCreate();
        dataSet = new Chart();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            String intentAction;

            Log.i(TAG, "BT SERVICE STATE CHANGE: " + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {

                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);

                Log.i(TAG, "Connected to GATT Server");
                Log.i(TAG, "Attempting to start service discovery: " + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;

                Log.i(TAG, "Disconnected from GATT Server");

                broadcastUpdate(intentAction);

            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

            } else {

                Log.w(TAG, "onServiceDiscovered received: " + status);

            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Log.i(TAG, "CHaracteristic read!!");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            Log.i(TAG, "CHaracteristic changed!" + gatt.toString());

            if (!isRunning) {
                isRunning = true;
            }

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);


        }
    };

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {

        if (!isStarted) {
            return;
        }

        final Intent intent = new Intent(action);

        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {

            int flag = characteristic.getProperties();
            int format = -1;

            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }

            final int heartRate = characteristic.getIntValue(format, 1);

            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));

            updateNotifier(String.valueOf(heartRate));
            dataSet.push(heartRate);

        } else {

            final byte[] data = characteristic.getValue();

            if (data != null && data.length > 0) {

                final StringBuilder stringBuilder = new StringBuilder(data.length);

                for(byte byteChar : data) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                }

                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());

            }

        }

        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action) {
        if (!isStarted) {
            return;
        }
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {

            return BluetoothLeService.this;

        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;

    }


    public boolean initialize() {

        if (ts == null) {
            ts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        ts.setLanguage(Locale.US);
                    }
                }
            });
        }

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */

    public boolean connect(final String address, Activity mainActivity) {

        isRunning = false;
        isStarted = true;

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.w(TAG, "Trying to use existing mBluetoothGatt for connection");
            isInited = true;
            if (mBluetoothGatt.connect()) {
                startNotifier(mainActivity.getIntent());
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        if (mBluetoothGatt != null) {
            Log.e(TAG, "Destroy Prev GATT " + mBluetoothGatt.toString());
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect");
            return false;
        }

        if (mBluetoothGatt == null) {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            Log.e(TAG, "Trying to create a new connection");
        } else {
            Log.w(TAG, "Connection already created??");
        }

        mBluetoothGatt.connect();

        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;

        isInited = true;

        startNotifier(mainActivity.getIntent());


        return true;
    }

    public void startNotifier(Intent resultIntent) {

        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.heartbeat)
                .setContentTitle("Heart rate zone notifier")
                .setContentText("Connecting...");

        //TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        //stackBuilder.addParentStack(MainActivity.class);

        //stackBuilder.addNextIntent(resultIntent);
        //resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        //PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);



        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);



        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(mId, mBuilder.build());

        startForeground(mId, mBuilder.build());


    }

    private void updateNotifier(String hrValue) {

        if (!isRunning) {
            Log.i(TAG, "SERVICE SHOLD NOT BE RUNNINGN!!!!");
            isRunning = false;
            disconnect();
            close();
        }

        if (mNotificationManager != null) {
            mBuilder.setContentText("" + hrValue);
            mNotificationManager.notify(mId, mBuilder.build());
            //ts.speak(hrValue, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void disconnect() {
        isStarted = false;

        Log.i(TAG, "DISCONNECT!");

        isRunning = false;


        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
        }

        if (mBluetoothGatt == null) {
            Log.w(TAG, "GATT not initialized");
            try{

                mBluetoothGatt.disconnect();

            }
            catch (Exception e)
            {
                Log.e(TAG, "Exception: "+Log.getStackTraceString(e));
            }
        }

        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }

        stopForeground(true);

        stopSelf();
    }

    public void close() {

        Log.i(TAG, "CLOSE");

        if (mBluetoothGatt == null) {
            return;
        }

        if (ts != null) {
            ts.shutdown();
            ts = null;
        }

       // mBluetoothGatt.close();
       // mBluetoothGatt = null;

    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdpter not initialized");
            return;
        }

        mBluetoothGatt.readCharacteristic(characteristic);

    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdpter not initialized");
            return;
        }



        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {

            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));

            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            //descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);


        }

    }

    public List<BluetoothGattService> getSupportedGattServices() {

        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();

    }

    public void turnHRMNotification() {
        Log.i(TAG, "Trying turn on HRM notifications");
        if (mBluetoothGatt == null || mBluetoothGatt.getServices() == null) {
            Log.i(TAG, "Error while accessing gatt services");
            return;
        }
        for (BluetoothGattService gattService : mBluetoothGatt.getServices()) {

            for(BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                if (UUID_HEART_RATE_MEASUREMENT.equals(gattCharacteristic.getUuid())) {
                    Log.i(TAG, "HRM service found");
                    readCharacteristic(gattCharacteristic);
                    setCharacteristicNotification(gattCharacteristic, true);
                }

            }

        }

    }

    public void stopIfNotRunning() {
        Log.i(TAG, "STOP? " + isRunning);
        if (isRunning == false) {
            Log.i(TAG, "STOP. Not running");
            this.disconnect();
            this.close();
        }
    }

    public Chart getDataSet() {
        return dataSet;
    }



}
