package com.pbartz.heartmonitor;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;


/**
 * Created by yura.ilyaev on 9/13/2015.
 */
public class PlayAudioService  extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener{

    public static final String RAW_ID = "RAW_ID";

    public static final String TAG = "AudioService";

    public int counter = 0;

    MediaPlayer mMediaPlayer = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mMediaPlayer = MediaPlayer.create(this, intent.getIntExtra(RAW_ID, 0));
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            //mMediaPlayer.prepareAsync();
            mMediaPlayer.start();

            counter += 1;
            Log.i(TAG, "Counter: " + counter);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(TAG, "Media play completed");
        stopSelf();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i(TAG, "Media Prepared");
        mp.start();
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

        Log.i(TAG, "Service Destroyed");

        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }
}
