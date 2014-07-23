package com.altabel.cast;


import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by nikolai on 23.07.2014.
 */
public abstract class BaseCastActivity extends ActionBarActivity {
    private static final String TAG = "BaseCastPlayerActivity";
    private String mMediaReceiverAppId;
    MediaRouter mMediaRouter;
    MediaRouteSelector mMediaRouteSelector;
    boolean isRouteSelected = false;
    MyMediaRouterCallback mMediaRouterCallback;
    protected CastDevice selectedDevice;
    private GoogleApiClient apiClient;
    protected boolean isApplicationStarted;
    protected ListView listView;

    protected abstract void onRouteSelected(RouteInfo route);
    protected abstract void onRouteUnselected(RouteInfo route);
    protected abstract void onConnected(Bundle bundle);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView)findViewById(R.id.listView);
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(getControlCategory()).build();
        mMediaRouterCallback = new MyMediaRouterCallback();
        mMediaReceiverAppId = getString(R.string.receiver_app_id);
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
        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
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
        setSelectedDevice(null);
        mMediaRouter.removeCallback(mMediaRouterCallback);
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        // If a route is selected, deselect it.
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
        Log.d(TAG, "setSelectedDevice: " + device);
        selectedDevice = device;
        if (selectedDevice != null) {
            try {
                stopApplication();
                disconnectApiClient();
                connectApiClient();
            }
            catch (IllegalStateException e)
            {
                Log.w(TAG, "Exception while connecting API client", e);
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
            Log.d(TAG, "onRouteSelected: route=" + route);
            isRouteSelected = true;
            BaseCastActivity.this.onRouteSelected(route);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteUnselected: route=" + route);
            isRouteSelected = false;
            BaseCastActivity.this.onRouteUnselected(route);
        }
    }
}
