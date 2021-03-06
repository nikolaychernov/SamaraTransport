package com.nikolaychernov.samaratransport;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class SettingsActivity extends AppCompatActivity implements OnSeekBarChangeListener {

    private TextView progressText;

    private int radius = 600;
    private boolean isAutoUpdate = true;
    private boolean isBackgroundUpdate = false;
    private static Tracker tracker;
    private static AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyApplication.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tracker = ((MyApplication) getApplication()).getDefaultTracker();
        tracker.setScreenName("SettingsActivity");
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("UX")
                .setAction("show")
                .setLabel("SettingsActivity")
                .build());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DataController dataMan = null;
        try {
            dataMan = DataController.getInstance();
        } catch (NullPointerException ex) {
            Intent mainIntent = new Intent(this, StopSearchActivity.class);
            finish();
            startActivity(mainIntent);
            return;
        }
        radius = getIntent().getIntExtra("radius", 600);
        progressText = (TextView) findViewById(R.id.txtRadiusLabel) ;

        ((SeekBar) findViewById(R.id.seekRadius)).setOnSeekBarChangeListener(this);
        isAutoUpdate = getIntent().getBooleanExtra("updateFlag", true);
        isBackgroundUpdate = getIntent().getBooleanExtra("backgroundFlag", true);
        radius = getIntent().getIntExtra("radius", 600);
        if (radius >= 1000) {
            progressText.setText("Радиус поиска остановок: " + radius / 1000.0 + " км");
        } else {
            progressText.setText("Радиус поиска остановок: " + radius + " м");
        }
        ((SeekBar) findViewById(R.id.seekRadius)).setProgress(radius - 300);
        ((Switch) findViewById(R.id.toggleAutoUpdate)).setChecked(isAutoUpdate);
        ((Switch) findViewById(R.id.toggleBackground)).setChecked(isBackgroundUpdate);
        ((CheckBox) findViewById(R.id.checkShowBuses)).setChecked(getIntent().getBooleanExtra("showBuses", true));
        ((CheckBox) findViewById(R.id.checkShowTrolls)).setChecked(getIntent().getBooleanExtra("showTrolls", true));
        ((CheckBox) findViewById(R.id.checkShowTrams)).setChecked(getIntent().getBooleanExtra("showTrams", true));
        ((CheckBox) findViewById(R.id.checkShowComm)).setChecked(getIntent().getBooleanExtra("showComm", true));
        setResults();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        return true;
    }

    private void setResults() {
        Intent data = new Intent();
        data.putExtra("updateFlag", ((Switch) findViewById(R.id.toggleAutoUpdate)).isChecked());
        data.putExtra("backgroundFlag", ((Switch) findViewById(R.id.toggleBackground)).isChecked());
        data.putExtra("radius", radius);
        data.putExtra("showBuses", ((CheckBox) findViewById(R.id.checkShowBuses)).isChecked());
        data.putExtra("showTrolls", ((CheckBox) findViewById(R.id.checkShowTrolls)).isChecked());
        data.putExtra("showTrams", ((CheckBox) findViewById(R.id.checkShowTrams)).isChecked());
        data.putExtra("showComm", ((CheckBox) findViewById(R.id.checkShowComm)).isChecked());
        setResult(RESULT_OK, data);
    }

    public void toggleButtonClick(View view) {
        switch (view.getId()) {
            case R.id.toggleAutoUpdate:
                isAutoUpdate = ((Switch) view).isChecked();
            case R.id.toggleBackground:
                isBackgroundUpdate = ((Switch) view).isChecked();
        }

        setResults();
    }

    public void themeClick(View view){
        ColorPickerDialog dialog = ColorPickerDialog.create(this);
        dialog.show(getFragmentManager(), null);
    }


    public void onCheckboxClicked(View view) {
        switch (view.getId()) {
            case R.id.toggleAutoUpdate:
                isAutoUpdate = ((Switch) view).isChecked();
            case R.id.toggleBackground:
                isBackgroundUpdate = ((Switch) view).isChecked();
        }

        setResults();
    }

    @Override
    public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
        // TODO Auto-generated method stub
        onStopTrackingTouch(arg0);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        radius = (int) Math.round((seekBar.getProgress() + 300) / 100) * 100;
        if (radius >= 1000) {
            progressText.setText("Радиус поиска остановок: " + radius / 1000.0 + " км");
        } else {
            progressText.setText("Радиус поиска остановок: " + radius + " м");
        }
        seekBar.setProgress(radius - 300);
        setResults();
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tracker.setScreenName("SettingsActivity");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }





}
