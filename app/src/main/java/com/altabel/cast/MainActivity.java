package com.altabel.cast;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.media.*;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import com.altabel.cast.adapter.AdapterListChannels;
import com.altabel.cast.item.Channel;
import com.altabel.cast.item.MediaTVItem;
import com.altabel.cast.parser.JsonUTF8Request;
import com.android.volley.*;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.images.WebImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.altabel.cast.utils.LoggerUtils.i;

public class MainActivity extends BaseCastActivity {
    private static final String TAG = "MainActivity";
    private static final String ACTION_RECEIVE_SESSION_STATUS_UPDATE = "com.altabel.cast.RECEIVE_SESSION_STATUS_UPDATE";
    private static final String ACTION_RECEIVE_MEDIA_STATUS_UPDATE = "com.altabel.cast.RECEIVE_MEDIA_STATUS_UPDATE";
    private static final int REQUEST_GMS_ERROR = 0;
    private AdapterListChannels adapterListChannels;
    private RequestQueue requestQueue;
    private JsonUTF8Request jsonUTF8Request;
    private ArrayList<Channel> channels = new ArrayList<Channel>();
    private MediaRouter.RouteInfo mCurrentRoute;
    private String mSessionId;
    private boolean isSessionActive;
    private boolean isDeviceConnected;
    private PendingIntent mSessionStatusUpdateIntent;
    private IntentFilter mSessionStatusBroadcastIntentFilter;
    private BroadcastReceiver mSessionStatusBroadcastReceiver;
    private PendingIntent mMediaStatusUpdateIntent;
    private IntentFilter mMediaStatusBroadcastIntentFilter;
    private BroadcastReceiver mMediaStatusBroadcastReceiver;
    private ResultBundleHandler mMediaResultHandler;
    private String mLastRouteId;
    private MediaInfo tvMediaInfo;
    private String mCurrentItemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSessionStatusBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                i("Got a session status broadcast intent from the MRP: " + intent);
                processSessionStatusBundle(intent.getExtras());
            }
        };

        mSessionStatusBroadcastIntentFilter = new IntentFilter(ACTION_RECEIVE_SESSION_STATUS_UPDATE);

        Intent intent = new Intent(ACTION_RECEIVE_SESSION_STATUS_UPDATE);
        intent.setComponent(getCallingActivity());
        mSessionStatusUpdateIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mMediaStatusBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                i("Got a media status broadcast intent from the MRP: " + intent);
            }
        };
        mMediaStatusBroadcastIntentFilter = new IntentFilter(ACTION_RECEIVE_MEDIA_STATUS_UPDATE);

        intent = new Intent(ACTION_RECEIVE_MEDIA_STATUS_UPDATE);
        intent.setComponent(getCallingActivity());
        mMediaStatusUpdateIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mMediaResultHandler = new ResultBundleHandler() {
            @Override
            public void handleResult(Bundle bundle) {
            }
        };

        requestQueue = Volley.newRequestQueue(this);
        loadListTask();
        disableView(mVolumeBar, true);
        disableView(mStopButton, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (errorCode != ConnectionResult.SUCCESS) {
            GooglePlayServicesUtil.getErrorDialog(errorCode, this, REQUEST_GMS_ERROR).show();
        }

        mListView.setOnItemClickListener(itemListViewListener);
        registerReceiver(mSessionStatusBroadcastReceiver, mSessionStatusBroadcastIntentFilter);
        registerReceiver(mMediaStatusBroadcastReceiver, mMediaStatusBroadcastIntentFilter);
        requestSessionStatus();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mSessionStatusBroadcastReceiver);
        unregisterReceiver(mMediaStatusBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onRouteSelected(MediaRouter.RouteInfo route) {
        setSelectedRoute(route);
        String routeId = route.getId();
        if (routeId.equals(mLastRouteId) && (mSessionId != null)) {
            i("Trying to rejoin previous session");
            requestSessionStatus();
        }
        mLastRouteId = routeId;
        isDeviceConnected = true;
        startSession();
    }

    @Override
    protected void onRouteUnselected(MediaRouter.RouteInfo route) {
        endSession();
        isSessionActive = false;
        isDeviceConnected = false;
        mSessionId = null;
        disableView(mVolumeBar, true);
        disableView(mStopButton, true);
    }

    @Override
    protected void onStopClicked() {
        i("onStopClicked");
        if (TextUtils.isEmpty(mCurrentItemId))
            return;

        Intent intent = new Intent(MediaControlIntent.ACTION_STOP);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        sendIntentToRoute(intent, mMediaResultHandler);

        disableView(mVolumeBar, true);
        disableView(mStopButton, true);
    }

    @Override
    protected void onConnected(Bundle bundle) {
    }

    @Override
    protected void onVolumeChange(double delta) {
        if (mCurrentRoute != null) {
            mCurrentRoute.requestUpdateVolume((int) (delta * MAX_VOLUME_LEVEL));
            refreshDeviceVolume(mCurrentRoute.getVolume() / MAX_VOLUME_LEVEL, false);
        }
    }

    @Override
    protected void onDeviceVolumeBarMoved(int volume) {
        if (mCurrentRoute != null) {
            mCurrentRoute.requestSetVolume(volume);
        }
    }

    private void processSessionStatusBundle(Bundle statusBundle) {
        i("processSessionStatusBundle()");

        String sessionId = statusBundle.getString(MediaControlIntent.EXTRA_SESSION_ID);
        MediaSessionStatus status = MediaSessionStatus.fromBundle(
                statusBundle.getBundle(MediaControlIntent.EXTRA_SESSION_STATUS));
        int sessionState = status.getSessionState();

        i("got a session status update for session " + sessionId + ", state = "
                + sessionState + ", mSessionId=" + mSessionId);

        if (mSessionId == null) {
            return;
        }

        if (!mSessionId.equals(sessionId)) {
            // Got status on a session other than the one we're tracking. Ignore it.
            i("Received status for unknown session: " + sessionId);
            return;
        }

        switch (sessionState) {
            case MediaSessionStatus.SESSION_STATE_ACTIVE:
                i("session " + sessionId + " is ACTIVE");
                isSessionActive = true;
                syncStatus();
                break;

            case MediaSessionStatus.SESSION_STATE_ENDED:
                i("session " + sessionId + " is ENDED");
                mSessionId = null;
                isSessionActive = false;
                break;

            case MediaSessionStatus.SESSION_STATE_INVALIDATED:
                i("session " + sessionId + " is INVALIDATED");
                mSessionId = null;
                isSessionActive = false;
                break;

            default:
                i("Received unexpected session state: " + sessionState);
                break;
        }
    }

    private void loadListTask(){
        disableView(mListView, true);
        showView(mRelativeProgressBar, true);
        jsonUTF8Request = new JsonUTF8Request(
                Request.Method.GET,
                getString(R.string.url_live_stream),
                null,
                responseListener(),
                responseError());
        requestQueue.add(jsonUTF8Request);
    }

    private Response.Listener<JSONObject> responseListener(){
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                i("TV Response:" + response.toString());
                try {
                    JSONArray jsonArray = response.getJSONArray(Channel.CHANNELS);
                    Channel tvItem = null;
                    for(int index = 0; index < jsonArray.length(); index++){
                        tvItem = new Channel(jsonArray.getJSONObject(index));
                        channels.add(tvItem);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                setListViewData(channels);

                disableView(mListView, false);
                showView(mRelativeProgressBar, false);
            }
        };
    }

    private Response.ErrorListener responseError(){
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                i("TV Response error:" + error.getMessage());
                disableView(mListView, false);
                showView(mRelativeProgressBar, false);
                showMsgDialog(error.getMessage(), getString(R.string.dialog_yes), null, INTERNET_CONNECTION_DIALOG);
            } // todo processing
        };
    }

    private void setListViewData(ArrayList<Channel> channels){
        adapterListChannels = new AdapterListChannels(this, R.layout.item_channel, channels);
        mListView.setAdapter(adapterListChannels);
    }

    AdapterView.OnItemClickListener itemListViewListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(isDeviceConnected && isSessionActive) {
                AdapterListChannels adapter = (AdapterListChannels) parent.getAdapter();
                Channel item = adapter.getItem(position);
                mCurrentItemId = item.getChannelId();
                mCurChannelName.setText(item.getName());
                playLiveStream(item.convertToMediaInfo());
            }else
                showMsgDialog(getString(R.string.err_device_connect), getString(R.string.dialog_yes), null, INTERNET_CONNECTION_DIALOG);
        }
    };

    private void playLiveStream(MediaInfo mediaInfo){
        tvMediaInfo = mediaInfo;
        disableView(mListView, true);
        showView(mRelativeProgressBar, true);
        jsonUTF8Request = new JsonUTF8Request(
                Request.Method.GET,
                mediaInfo.getContentId(),
                null,
                liveStreamResponseListener(),
                liveStreamResponseError());
        jsonUTF8Request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(jsonUTF8Request);
    }

    private Response.Listener<JSONObject> liveStreamResponseListener(){
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                i("TV MediaInfo Response:" + response.toString());
                MediaTVItem mediaTVItem = new MediaTVItem(tvMediaInfo, response);
                sendIntentToRoute(mediaTVItem.getMediaInfo());

                disableView(mListView, false);
                showView(mRelativeProgressBar, false);
                disableView(mVolumeBar, false);
                disableView(mStopButton, false);
            }
        };
    }

    private Response.ErrorListener liveStreamResponseError(){
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                i("TV MediaInfo Response error:" + error.getMessage());
                if( error instanceof NetworkError) {
                    i("NetworkError:" + error.getMessage());
                } else if( error instanceof ServerError) {
                    i("ServerError:" + error.getMessage());
                } else if( error instanceof ParseError) {
                    i("ParseError:" + error.getMessage());
                } else if( error instanceof NoConnectionError) {
                    i("NoConnectionError:" + error.getMessage());
                } else if( error instanceof TimeoutError) {
                    i("TimeoutError:" + error.getMessage());
                } // todo processing

                disableView(mListView, false);
                showView(mRelativeProgressBar, false);
                disableView(mVolumeBar, true);
                disableView(mStopButton, true);
                showMsgDialog(error.getMessage(), getString(R.string.dialog_yes), null, INTERNET_CONNECTION_DIALOG);
            }
        };
    }

    private void setSelectedRoute(MediaRouter.RouteInfo route) {
        mCurrentRoute = route;
    }

    private void sendIntentToRoute(MediaInfo media){
        MediaMetadata metadata = media.getMetadata();
        i("Casting " + metadata.getString(MediaMetadata.KEY_TITLE) + " (" + media.getContentType() + ")");

        Intent intent = new Intent(MediaControlIntent.ACTION_PLAY);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.setDataAndType(Uri.parse(media.getContentId()), media.getContentType());
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        intent.putExtra(MediaControlIntent.EXTRA_ITEM_STATUS_UPDATE_RECEIVER, mMediaStatusUpdateIntent);

        Bundle metadataBundle = new Bundle();

        String title = metadata.getString(MediaMetadata.KEY_TITLE);
        if (!TextUtils.isEmpty(title)) {
            metadataBundle.putString(MediaItemMetadata.KEY_TITLE, title);
        }

        List<WebImage> images = metadata.getImages();
        String artist = metadata.getString(MediaMetadata.KEY_ARTIST);
        if (artist == null) {
            artist = metadata.getString(MediaMetadata.KEY_STUDIO);
        }
        if (!TextUtils.isEmpty(artist)) {
            metadataBundle.putString(MediaItemMetadata.KEY_ARTIST, artist);
        }

        if ((images != null) && !images.isEmpty()) {
            Uri imageUrl = images.get(0).getUrl();
            if (imageUrl != null) {
                metadataBundle.putString(MediaItemMetadata.KEY_ARTWORK_URI, imageUrl.toString());
            }
        }

        intent.putExtra(MediaControlIntent.EXTRA_ITEM_METADATA, metadataBundle);

        sendIntentToRoute(intent, mMediaResultHandler);
    }

    private void sendIntentToRoute(final Intent intent, final ResultBundleHandler resultHandler) {
        String sessionId = intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID);
        i("Sending intent to route: " + intent + ", session: " + sessionId);
        if ((mCurrentRoute == null) || !mCurrentRoute.supportsControlRequest(intent)) {
            i("route is null or doesn't support this request");
            return;
        }

        mCurrentRoute.sendControlRequest(intent, new MediaRouter.ControlRequestCallback() {
            @Override
            public void onResult(Bundle data) {
                i("got onResult for " + intent.getAction() + " with bundle " + data);
                if (data != null) {
                    if (resultHandler != null) {
                        resultHandler.handleResult(data);
                    }
                } else {
                    i("got onResult with a null bundle");
                }
            }

            @Override
            public void onError(String message, Bundle data) {
                showMsgDialog(message != null ? message : getString(R.string.err_request_failed), getString(R.string.dialog_yes), null, INTERNET_CONNECTION_DIALOG);
            }
        });
    }

    private void startSession() {
        Intent intent = new Intent(MediaControlIntent.ACTION_START_SESSION);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_STATUS_UPDATE_RECEIVER, mSessionStatusUpdateIntent);
        intent.putExtra(CastMediaControlIntent.EXTRA_CAST_APPLICATION_ID, getReceiverApplicationId());
        intent.putExtra(CastMediaControlIntent.EXTRA_CAST_RELAUNCH_APPLICATION, true);
        intent.putExtra(CastMediaControlIntent.EXTRA_DEBUG_LOGGING_ENABLED, true);
        intent.putExtra(CastMediaControlIntent.EXTRA_CAST_STOP_APPLICATION_WHEN_SESSION_ENDS, false);
        sendIntentToRoute(intent, new ResultBundleHandler() {
            @Override
            public void handleResult(Bundle bundle) {
                mSessionId = bundle.getString(MediaControlIntent.EXTRA_SESSION_ID);
                i("Got a session ID of: " + mSessionId);
                isSessionActive = true;
            }
        });
    }

    private void syncStatus() {
        i("Invoking SYNC_STATUS request");
        Intent intent = new Intent(CastMediaControlIntent.ACTION_SYNC_STATUS);
        intent.addCategory(CastMediaControlIntent.categoryForRemotePlayback());
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        intent.putExtra(MediaControlIntent.EXTRA_ITEM_STATUS_UPDATE_RECEIVER, mMediaStatusUpdateIntent);
        sendIntentToRoute(intent, mMediaResultHandler);
    }

    private void requestSessionStatus() {
        if (mSessionId == null)
            return;

        Intent intent = new Intent(MediaControlIntent.ACTION_GET_SESSION_STATUS);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        sendIntentToRoute(intent, null);
    }

    private void endSession() {
        if (mSessionId == null)
            return;

        Intent intent = new Intent(MediaControlIntent.ACTION_END_SESSION);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        sendIntentToRoute(intent, new ResultBundleHandler() {
            @Override
            public void handleResult(final Bundle bundle) {
                MediaSessionStatus status = MediaSessionStatus.fromBundle(bundle.getBundle(MediaControlIntent.EXTRA_SESSION_STATUS));
                int sessionState = status.getSessionState();
                i("session state after ending session: " + sessionState);
            }
        });
        mSessionId = null;
    }

    private interface ResultBundleHandler {
        public void handleResult(Bundle bundle);
    }
}
