package com.nikolaychernov.samaratransport;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.BannerCallbacks;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.net.UnknownHostException;
import java.util.ArrayList;


public class ArrivalActivity extends ActionBarActivity {

    private Stop st;
    private CountDownTimer t;
    private DownloadArrivalInfoTask task;
    private boolean showRouteArrival = false;
    private int KR_ID = 0;
    ListView mListView;
    Tracker tracker;
    RelativeLayout closeLayout;
    ImageButton close;

    private class MyTimer extends CountDownTimer {

        public MyTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);        }

        @Override
        public void onFinish() {
            Log.w("ArrivalTimer", "updating");
            cmdUpdate_click(null);
        }

        public void onTick(long millisUntilFinished) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.arrival_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_settings:
                cmdSettings_click();
                return true;
            case R.id.action_rate:
                Intent googlePlayIntent = DataController.createIntentForGooglePlay(this);
                startActivity(googlePlayIntent);
                return true;
            case R.id.action_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
            case R.id.action_share:
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("UX")
                        .setAction("click")
                        .setLabel("Share")
                        .build());
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Рассказать друзьям"));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyApplication.getCurrentTheme());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_arrival);

        tracker = ((MyApplication) getApplication()).getDefaultTracker();
        tracker.setScreenName("ArrivalActivity");
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("UX")
                .setAction("show")
                .setLabel("ArrivalActivity")
                .build());

        Intent intent = getIntent();
        st = (Stop) intent.getSerializableExtra(StopSearchActivity.MESSAGE_STOP);

        try {
            DataController.getInstance();
        } catch (NullPointerException ex) {
            Intent mainIntent = new Intent(this, StopSearchActivity.class);
            finish();
            startActivity(mainIntent);
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView title = (TextView) findViewById(R.id.title);
        TextView subtitle = (TextView) findViewById(R.id.subtitle);
        title.setText(st.title);
        subtitle.setText(st.direction);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        close = (ImageButton) findViewById(R.id.close);
        closeLayout = (RelativeLayout) findViewById(R.id.close_layout);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.appodealBannerView).setVisibility(View.GONE);
                closeLayout.setVisibility(View.GONE);
            }
        });

        Appodeal.setBannerViewId(R.id.appodealBannerView);
        Appodeal.setBannerCallbacks(new BannerCallbacks() {

            @Override
            public void onBannerLoaded() {
                findViewById(R.id.appodealBannerView).setVisibility(View.VISIBLE);
            }

            @Override
            public void onBannerFailedToLoad() {

            }

            @Override
            public void onBannerShown() {
                closeLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onBannerClicked() {

            }
        });

        task = new DownloadArrivalInfoTask();

        mListView = (ListView) findViewById(R.id.arrivalList);
        final SwipeRefreshLayout mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.theme_color, typedValue, true);
        int color = typedValue.data;
        mRefreshLayout.setColorSchemeColors(color);

        // Set a listener to be invoked when the list should be refreshed.
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                displayArrivalInfo();
                mRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        cmdUpdate_click(null);
        Appodeal.show(this, Appodeal.BANNER_VIEW);
        tracker.setScreenName("ArrivalActivity");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (t != null) {
            t.cancel();
        }
        task.cancel(false);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (t != null) {
            t.cancel();
        }
        task.cancel(false);
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    private void displayArrivalInfo() {
        task.cancel(false);
        if (t != null) {
            t.cancel();
        }
        task = new DownloadArrivalInfoTask();
        task.execute(this);
    }

    public void cmdUpdate_click(View view) {
        showRouteArrival = false;
        displayArrivalInfo();
    }

    public void cmdSettings_click() {
        Intent intent = new Intent(this, ProperSettingsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ArrivalActivity.this);
        if (sharedPref.getBoolean("updateFlag", true)) {
            cmdUpdate_click(null);
        }
    }


    private class DownloadArrivalInfoTask extends AsyncTask<Activity, Boolean, Integer> {

        private ArrayList<ArrivalInfo> arrInfo = null;
        private Activity act;
        private String msg = "";

        // Do the long-running work in here
        protected Integer doInBackground(Activity... parent) {
            act = parent[0];
            try {
                if (showRouteArrival) {
                    Log.v("TAG3", "" + st.KS_ID);
                    arrInfo = DataController.getRouteArrivalAPI(st.KS_ID, KR_ID, "ArrAct");
                    showRouteArrival = false;

                } else {
                    Log.v("TAG4", "" + st.KS_ID);
                    arrInfo = DataController.getInstance().getArrivalInfo(st.KS_ID);

                }
                if (isCancelled()) {
                    Log.v("TAG", "isCancelled");
                    return 3;
                }
                return 0;
            } catch (NotFoundException e) {
                msg = e.getLocalizedMessage();
                Log.v("TAG1", msg);
                return 1;
            } catch (UnknownHostException e) {
                msg = e.getMessage();
                Log.v("TAG2", msg);
                return 2;
            } catch (Exception e) {
                msg = e.getMessage();
                Log.v("TAG3", msg);
                return 3;
            }
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Boolean... progress) {

        }

        // This is called when doInBackground() is finished
        protected void onPostExecute(Integer result) {
            if (isCancelled()) {
                return;
            }
            switch (result){
                case 0: if (!arrInfo.isEmpty()) {
                    findViewById(R.id.txtTransAbsentMessage).setVisibility(View.GONE);
                    findViewById(R.id.txtConnectionProblem).setVisibility(View.GONE);

                    ArrivalListAdapter adapter = new ArrivalListAdapter(act, arrInfo);

                    mListView.setAdapter(adapter);

                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
                            tracker.send(new HitBuilders.EventBuilder()
                                    .setCategory("UX")
                                    .setAction("click")
                                    .setLabel("List item")
                                    .build());
                            if (id > 0) {
                                showRouteArrival = true;
                                KR_ID = (int) id;
                                displayArrivalInfo();
                            }
                        }
                    });
                } else {
                    findViewById(R.id.arrivalList).setVisibility(View.GONE);
                    findViewById(R.id.txtTransAbsentMessage).setVisibility(View.VISIBLE);
                    findViewById(R.id.txtConnectionProblem).setVisibility(View.GONE);
                }
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ArrivalActivity.this);
                    if (sharedPref.getBoolean("updateFlag", true)) {
                        t = new MyTimer(30000, 60000);
                        t = t.start();
                    }
                    break;
                case 1: findViewById(R.id.arrivalList).setVisibility(View.GONE);
                    findViewById(R.id.txtTransAbsentMessage).setVisibility(View.GONE);
                    TextView textView = (TextView) findViewById(R.id.txtConnectionProblem);
                    textView.setText(R.string.no_response_from_server);
                    textView.setVisibility(View.VISIBLE);
                    break;
                case 2: findViewById(R.id.arrivalList).setVisibility(View.GONE);
                    findViewById(R.id.txtTransAbsentMessage).setVisibility(View.GONE);
                    findViewById(R.id.txtConnectionProblem).setVisibility(View.VISIBLE);
                    break;
                case 3: findViewById(R.id.arrivalList).setVisibility(View.GONE);
                    findViewById(R.id.txtTransAbsentMessage).setVisibility(View.GONE);
                    textView = (TextView) findViewById(R.id.txtConnectionProblem);
                    textView.setText(R.string.no_response_from_server);
                    textView.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

}
