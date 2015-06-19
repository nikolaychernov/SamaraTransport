package air.nikolaychernov.samis.ChernovPryb;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Resources.NotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import java.util.ArrayList;

public class ArrivalActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {

    private Stop st;
    private CountDownTimer t;
    private DownloadArrivalInfoTask task;
    private GoogleApiClient mGoogleApiClient;
    public static String accountName = "";
    private boolean showRouteArrival = false;
    private int KR_ID = 0;

    ListView mListView;

    private class MyTimer extends CountDownTimer {

        public MyTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            // Do something...
            Log.w("ArrivalTimer", "updating");
            cmdUpdate_click(null);
        }

        public void onTick(long millisUntilFinished) {

        }

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("onConnected","mGoogleApiClient.connect()");
        accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
    }


    @Override
    public void onConnectionSuspended(int arg0) {
        Log.d("onConnectionSuspended","mGoogleApiClient.connect()");
        //mGoogleApiClient.connect();
        //updateUI(false);

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d("onConnectionFailed",""+ result.getErrorCode());
        try {
            result.startResolutionForResult(this,10);
        } catch (IntentSender.SendIntentException e) {
            //mIntentInProgress = false;
            mGoogleApiClient.connect();
        }
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.

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
            case R.id.action_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Log.appendLog("ArrivalActivity onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_arrival);

        Intent intent = getIntent();
        st = (Stop) intent.getSerializableExtra(StopSearchActivity.MESSAGE_STOP);

        DataController dataMan = null;
        try {
            dataMan = DataController.getInstance();
        } catch (NullPointerException ex) {
            Intent mainIntent = new Intent(this, StopSearchActivity.class);
            finish();
            startActivity(mainIntent);
            return;
        }

        ActionBar ab = getActionBar();
        ab.setIcon(null);
        ab.setTitle(st.title);
        ab.setSubtitle(st.direction);
        TextView tv;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .build();


        task = new DownloadArrivalInfoTask();

        mListView = (ListView) findViewById(R.id.arrivalList);
        final SwipeRefreshLayout mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mRefreshLayout.setColorSchemeResources(R.color.primary);

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
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cmdUpdate_click(null);
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
        mGoogleApiClient.disconnect();
        if (t != null) {
            t.cancel();
        }
        task.cancel(false);
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
        Intent intent = new Intent(this, SettingsActivity.class);
        DataController dataMan = DataController.getInstance();
        intent.putExtra("radius", dataMan.getRadius());
        intent.putExtra("updateFlag", dataMan.isAutoUpdate());
        intent.putExtra("showTrams", dataMan.isShowTrams());
        intent.putExtra("showTrolls", dataMan.isShowTrolls());
        intent.putExtra("showBuses", dataMan.isShowBuses());
        intent.putExtra("showComm", dataMan.isShowComm());
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Log.appendLog("ArrivalActivity onActivityResult");
        DataController.getInstance().setSettings(data.getIntExtra("radius", 600), data.getBooleanExtra("updateFlag", true), data.getBooleanExtra("showBuses", true), data.getBooleanExtra("showTrolls", true), data.getBooleanExtra("showTrams", true), data.getBooleanExtra("showComm", true));
        if (DataController.getInstance().isAutoUpdate()) {
            cmdUpdate_click(null);
        }
    }


    private class DownloadArrivalInfoTask extends AsyncTask<Activity, Boolean, Boolean> {

        private ArrayList<ArrivalInfo> arrInfo = null;
        private Activity act;
        private String msg = "";

        // Do the long-running work in here
        protected Boolean doInBackground(Activity... parent) {
            // Log.appendLog("ArrivalActivity DownloadArrivalInfoTask doInBackground");
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
                    return false;
                }
                return true;
            } catch (NotFoundException e) {
                // TODO Auto-generated catch block
                msg = e.getLocalizedMessage();
                // Log.appendLog("EXCEPTION NotFoundException in ArrivalActivity DownloadArrivalInfoTask doInBackground "
                // + msg);
                Log.v("TAG1", msg);
                return false;
            } catch (Exception e) {
                msg = e.getMessage();
                Log.v("TAG2", msg);
                return false;
            }

        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Boolean... progress) {

        }

        // This is called when doInBackground() is finished
        protected void onPostExecute(Boolean result) {
            // Log.appendLog("ArrivalActivity DownloadArrivalInfoTask onPostExecute");
            //findViewById(R.id.progressLoading).setVisibility(View.INVISIBLE);
            if (isCancelled()) {
                return;
            }
            if (result) {
                if (!arrInfo.isEmpty()) {
                    findViewById(R.id.txtTransAbsentMessage).setVisibility(View.INVISIBLE);
                    findViewById(R.id.txtConnectionProblem).setVisibility(View.INVISIBLE);

                    ArrivalListAdapter adapter = new ArrivalListAdapter(act, arrInfo);

                    mListView.setAdapter(adapter); // отображаем все объекты
                    //wrapper.onRefreshComplete();
                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
                            if (id > 0) {
                                showRouteArrival = true;
                                KR_ID = (int) id;
                                displayArrivalInfo();
                            }
                        }
                    });
                } else {
                    findViewById(R.id.arrivalList).setVisibility(View.INVISIBLE);
                    findViewById(R.id.txtTransAbsentMessage).setVisibility(View.VISIBLE);
                    findViewById(R.id.txtConnectionProblem).setVisibility(View.INVISIBLE);
                }

                if (DataController.getInstance().isAutoUpdate()) {
                    t = new MyTimer(30000, 60000);
                    t = t.start();
                }
            } else {
                // StopSearchActivity.msgBox(act, msg, "Error");
                findViewById(R.id.arrivalList).setVisibility(View.INVISIBLE);
                findViewById(R.id.txtTransAbsentMessage).setVisibility(View.INVISIBLE);
                findViewById(R.id.txtConnectionProblem).setVisibility(View.VISIBLE);
                // act.finish();
            }
        }
    }

}
