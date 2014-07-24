package com.altabel.cast;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.altabel.cast.dialog.MessageDialog;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import static com.altabel.cast.utils.LoggerUtils.i;
/**
 * Created by nikolai on 23.07.2014.
 */
public abstract class BaseCastActivity extends ActionBarActivity implements MessageDialog.MessageDialogListener {
    private static final String TAG = "BaseCastActivity";
    protected static final double VOLUME_INCREMENT = 0.05;
    protected static final double MAX_VOLUME_LEVEL = 20;
    public int indexDialog = -1;
    public final int SERVER_ERROR_DIALOG = 0;
    public final int INTERNET_CONNECTION_DIALOG = 1;

    private String mMediaReceiverAppId;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private boolean isRouteSelected = false;
    private MyMediaRouterCallback mMediaRouterCallback;
    protected CastDevice selectedDevice;
    private GoogleApiClient apiClient;
    protected boolean isApplicationStarted;
    private boolean isUserAdjustingVolume;

    protected ListView mListView;
    protected RelativeLayout mRelativeProgressBar;
    protected SeekBar mVolumeBar;
    protected TextView mCurChannelName;
    protected Button mStopButton;

    protected abstract void onRouteSelected(RouteInfo route);
    protected abstract void onRouteUnselected(RouteInfo route);
    protected abstract void onConnected(Bundle bundle);
    protected abstract void onVolumeChange(double delta);
    protected abstract void onDeviceVolumeBarMoved(int volume);
    protected abstract void onStopClicked();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(getControlCategory()).build();
        mMediaRouterCallback = new MyMediaRouterCallback();
        mMediaReceiverAppId = getString(R.string.receiver_app_id);

        setUpVolumeControls(mVolumeBar);
        isUserAdjustingVolume = false;
    }

    private void initViews(){
        mListView = (ListView)findViewById(R.id.listView);
        mRelativeProgressBar = (RelativeLayout)findViewById(R.id.relativeProgressBar);
        mVolumeBar = (SeekBar)findViewById(R.id.device_volume_bar);
        mCurChannelName = (TextView)findViewById(R.id.curChannelName);
        mStopButton = (Button)findViewById(R.id.stopButton);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStopClicked();
            }
        });
    }

    protected void disableView(View view, boolean isDisable){
        if(view != null && ((isDisable && view.isEnabled()) || (!isDisable && !view.isEnabled())))
            view.setEnabled(!isDisable);
    }

    protected void showView(View view, boolean isShow){
        int show = isShow ? View.VISIBLE : View.GONE;
        if(view.getVisibility() != show)
            view.setVisibility(show);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            onVolumeChange(VOLUME_INCREMENT);
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            onVolumeChange(-VOLUME_INCREMENT);
        } else {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    private void setUpVolumeControls(final SeekBar volumeBar) {
        volumeBar.setMax((int) MAX_VOLUME_LEVEL);
        volumeBar.setProgress(0);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserAdjustingVolume = false;
                volumeBar.setSecondaryProgress(0);
                    onDeviceVolumeBarMoved(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserAdjustingVolume = true;
                volumeBar.setSecondaryProgress(seekBar.getProgress());
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }
        });
    }

    protected final void refreshDeviceVolume(double percent, boolean muted) {
        if (!isUserAdjustingVolume) {
            mVolumeBar.setProgress((int) (percent * MAX_VOLUME_LEVEL));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onStop() {
        mMediaRouter.removeCallback(mMediaRouterCallback);
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (isRouteSelected) {
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
            isRouteSelected = false;
        }
        super.onBackPressed();
    }

    protected String getControlCategory() {
        return CastMediaControlIntent.categoryForRemotePlayback(getReceiverApplicationId());
    }

    protected final String getReceiverApplicationId() {
        if(TextUtils.isEmpty(mMediaReceiverAppId))
            return CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;
        return mMediaReceiverAppId;
    }

    protected void setSelectedDevice(CastDevice device) {
        i("setSelectedDevice: " + device);
        selectedDevice = device;
        if (selectedDevice != null) {
            try {
                stopApplication();
                disconnectApiClient();
                connectApiClient();
            }
            catch (IllegalStateException e) {
                i("Exception while connecting API client " + e.getMessage());
                disconnectApiClient();
            }
        } else {
            if (apiClient != null) {
                disconnectApiClient();
            }
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        }
    }

    private void connectApiClient() {
        Cast.CastOptions apiOptions = Cast.CastOptions.builder(selectedDevice, castListener).build();
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(Cast.API, apiOptions)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(connectionFailedListener)
                .build();
        apiClient.connect();
    }

    Cast.Listener castListener = new Cast.Listener() {
        @Override
        public void onApplicationStatusChanged() {
            super.onApplicationStatusChanged();
        }

        @Override
        public void onApplicationDisconnected(int statusCode) {
            super.onApplicationDisconnected(statusCode);
        }

        @Override
        public void onVolumeChanged() {
            super.onVolumeChanged();
        }
    };

    GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            BaseCastActivity.this.onConnected(bundle);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    };

    GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }
    };

    private void disconnectApiClient() {
        if (apiClient != null) {
            apiClient.disconnect();
            apiClient = null;
        }
    }

    private void stopApplication() {
        if (apiClient == null) return;

        if (isApplicationStarted) {
            Cast.CastApi.stopApplication(apiClient);
            isApplicationStarted = false;
        }
    }

    private class MyMediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
            i("onRouteSelected: route=" + route);
            isRouteSelected = true;
            BaseCastActivity.this.onRouteSelected(route);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            i("onRouteUnselected: route=" + route);
            isRouteSelected = false;
            BaseCastActivity.this.onRouteUnselected(route);
        }
    }

    public void showMsgDialog(String message, String namePositiveBtn, String nameNegativeBtn,  int indexDialog, boolean isCancelable){
        this.indexDialog = indexDialog;
        DialogFragment dialogFragment = new MessageDialog(getString(R.string.app_name), message, namePositiveBtn, nameNegativeBtn);
        dialogFragment.setCancelable(isCancelable);
        try{
            dialogFragment.show(getSupportFragmentManager(), "MessageVPDialogFragment");
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void showMsgDialog(String message, String namePositiveBtn, String nameNegativeBtn,  int indexDialog){
        showMsgDialog(message, namePositiveBtn, nameNegativeBtn, indexDialog, false);
    }

    @Override
    public void onMsgDialogPositiveClick(DialogFragment dialog) {
        switch (indexDialog){
            case SERVER_ERROR_DIALOG:
                dialog.dismiss();
                break;
            case INTERNET_CONNECTION_DIALOG:
                dialog.dismiss();
                break;
        }
    }

    @Override
    public void onMsgDialogNegativeClick(DialogFragment dialog) {
        switch (indexDialog){
            case SERVER_ERROR_DIALOG:
                dialog.dismiss();
                break;
            case INTERNET_CONNECTION_DIALOG:
                dialog.dismiss();
                break;
        }
    }
}