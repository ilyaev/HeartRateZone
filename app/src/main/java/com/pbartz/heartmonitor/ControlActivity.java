package com.pbartz.heartmonitor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.pbartz.heartmonitor.view.Button;
import com.pbartz.heartmonitor.view.ZoneProgress;

public class ControlActivity extends AppCompatActivity {

    View viewGauge;
    ZoneProgress viewProgress;
    View viewChart;

    android.widget.ImageButton btnPlay;
    android.widget.ImageButton btnSettings;
    android.widget.ImageButton btnAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_control);

        initView();

    }

    private void initView() {

        viewGauge = findViewById(R.id.viewGauge);
        viewProgress = (ZoneProgress) findViewById(R.id.viewProgress);
        viewChart = findViewById(R.id.viewChart);

        btnPlay = (android.widget.ImageButton) findViewById(R.id.btnPlay);
        btnSettings = (android.widget.ImageButton) findViewById(R.id.btnSettings);
        btnAudio = (android.widget.ImageButton) findViewById(R.id.btnAudio);

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        btnAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAudio.setImageResource(viewChart.getVisibility() == View.VISIBLE ? R.drawable.audio_on_white : R.drawable.audio_off_white);
                viewProgress.updateHrValue((int)(Math.round(Math.random() * viewProgress.getHeight())));
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_control, menu);
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
}
