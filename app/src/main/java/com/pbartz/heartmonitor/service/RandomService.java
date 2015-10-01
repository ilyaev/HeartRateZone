package com.pbartz.heartmonitor.service;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.pbartz.heartmonitor.MainActivity;
import com.pbartz.heartmonitor.R;
import com.pbartz.heartmonitor.zone.Chart;

/**
 * Created by yura.ilyaev on 9/11/2015.
 */
public class RandomService extends Service {

    private final static String TAG = "RndService";

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public static boolean isRunning = false;
    public static boolean isInited = false;

    private NotificationCompat.Builder mBuilder = null;
    private NotificationManager mNotificationManager = null;

    public final static String ACTION_GATT_CONNECTED = "pbartz.hrm.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "pbartz.hrm.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "pbartz.hrm.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "pbartz.hrm.ACTION_GATT_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "pbartz.hrm.EXTRA_DATA";

    public static Chart dataSet;

    private int mCurrentValue = 60;

    int mId = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        dataSet = new Chart();
        Log.i(TAG, "SERVICE CREATED!!!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "SERVICE DESTROUED!");
    }

    public void startGenerator() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {

                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {

                    }

                    if (isRunning) {

                        mCurrentValue = mCurrentValue + (int) (Math.round(Math.random() * 40) - 20);
                        if (mCurrentValue < 60) {
                            mCurrentValue += Math.round(Math.random() * 10);
                        } else if (mCurrentValue > 195) {
                            mCurrentValue -= Math.round(Math.random() * 10);
                        }

                        broadcastUpdate(ACTION_DATA_AVAILABLE, mCurrentValue);
                        dataSet.push(mCurrentValue);

                        updateNotifier("" + mCurrentValue);
                        //playSound();


                    }

                }

            }
        }).start();
    }

    private void playSound() {
        Log.i(TAG, "Try To Play SOUND");
        Intent intent = new Intent(this, PlayAudioService.class);
        intent.putExtra(PlayAudioService.RAW_ID, R.raw.pickup_coin);
        startService(intent);
    }

    private void broadcastUpdate(final String action, int number) {

        final Intent intent = new Intent(action);

        intent.putExtra(EXTRA_DATA, Integer.toString(number));

        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public RandomService getService() {

            return RandomService.this;

        }
    }

    public final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;

    }

    public boolean initialize() {
        Log.i(TAG, "Init SERVICE: " + isRunning + " / " + isInited);
        return true;
    }

    public boolean connect(final String address, final Activity mainActivity) {

        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
        mConnectionState = STATE_CONNECTING;

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(15000);
                } catch (Exception e) {

                }

                if (mConnectionState != STATE_DISCONNECTED) {

                    mConnectionState = STATE_CONNECTED;

                    broadcastUpdate(ACTION_GATT_CONNECTED);

                    isRunning = true;

                    if (!isInited) {
                        startGenerator();
                        isInited = true;
                    }

                    startNotifier(mainActivity.getIntent());

                }
            }
        }).start();

        return true;
    }

    public void startNotifier(Intent resultIntent) {

        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.heartbeat)
                .setContentTitle("Heart Rate Monitor")
                .setContentText("Connecting...");

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);

        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);


        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(mId, mBuilder.build());

        startForeground(mId, mBuilder.build());


    }

    private void updateNotifier(String hrValue) {
        if (mNotificationManager != null) {
            mBuilder.setContentText("" + hrValue);
            mNotificationManager.notify(mId, mBuilder.build());
        }
    }

    public void disconnect() {
        mConnectionState = STATE_DISCONNECTED;
        broadcastUpdate(ACTION_GATT_DISCONNECTED);
        isRunning = false;
        Log.i(TAG, "STOP FOREGROUND??");
        stopForeground(true);
        stopSelf();
    }

    public void close() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(mId);
        }
       return;

    }

    public Chart getDataSet() {
        return dataSet;
    }

}
