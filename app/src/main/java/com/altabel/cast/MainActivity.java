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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.altabel.cast.adapter.AdapterListChannels;
import com.altabel.cast.item.Channel;
import com.altabel.cast.item.MediaTVItem;
import com.altabel.cast.parser.JsonUTF8Request;
import com.android.volley.*;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.cast.CastDevice;
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
    private static final int REQUEST_GMS_ERROR = 0;
    AdapterListChannels adapterListChannels;
    private RequestQueue requestQueue;
    private JsonUTF8Request jsonUTF8Request;
    ArrayList<Channel> channels = new ArrayList<Channel>();

    private static final String TAG = "MrpCastPlayerActivity";
    private static final String ACTION_RECEIVE_SESSION_STATUS_UPDATE =
            "com.google.android.gms.cast.samples.democastplayer.RECEIVE_SESSION_STATUS_UPDATE";
    private static final String ACTION_RECEIVE_MEDIA_STATUS_UPDATE =
            "com.google.android.gms.cast.samples.democastplayer.RECEIVE_MEDIA_STATUS_UPDATE";

    private MediaRouter.RouteInfo mCurrentRoute;
    private String mLastRouteId;
    private String mSessionId;
    private boolean mSessionActive;
    private PendingIntent mSessionStatusUpdateIntent;
    private IntentFilter mSessionStatusBroadcastIntentFilter;
    private BroadcastReceiver mSessionStatusBroadcastReceiver;
    private String mCurrentItemId;
    private PendingIntent mMediaStatusUpdateIntent;
    private IntentFilter mMediaStatusBroadcastIntentFilter;
    private BroadcastReceiver mMediaStatusBroadcastReceiver;
    private long mStreamPositionTimestamp;
    private long mLastKnownStreamPosition;
    private long mStreamDuration;
    private boolean mStreamAdvancing;
    private ResultBundleHandler mMediaResultHandler;

    private interface ResultBundleHandler {
        public void handleResult(Bundle bundle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Construct a broadcast receiver and a PendingIntent for receiving session status
        // updates from the MRP.
        mSessionStatusBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Got a session status broadcast intent from the MRP: " + intent);
                processSessionStatusBundle(intent.getExtras());
            }
        };
        mSessionStatusBroadcastIntentFilter = new IntentFilter(
                ACTION_RECEIVE_SESSION_STATUS_UPDATE);

        Intent intent = new Intent(ACTION_RECEIVE_SESSION_STATUS_UPDATE);
        intent.setComponent(getCallingActivity());
        mSessionStatusUpdateIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Construct a broadcast receiver and a PendingIntent for receiving media status
        // updates from the MRP.
        mMediaStatusBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Got a media status broadcast intent from the MRP: " + intent);
//                processMediaStatusBundle(intent.getExtras());
            }
        };
        mMediaStatusBroadcastIntentFilter = new IntentFilter(ACTION_RECEIVE_MEDIA_STATUS_UPDATE);

        intent = new Intent(ACTION_RECEIVE_MEDIA_STATUS_UPDATE);
        intent.setComponent(getCallingActivity());
        mMediaStatusUpdateIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mMediaResultHandler = new ResultBundleHandler() {

            @Override
            public void handleResult(Bundle bundle) {
//                processMediaStatusBundle(bundle);
            }
        };
        requestQueue = Volley.newRequestQueue(this);
    }

//    private void processMediaStatusBundle(Bundle statusBundle) {
//        Log.d(TAG, "processMediaStatusBundle()");
//        String itemId = statusBundle.getString(MediaControlIntent.EXTRA_ITEM_ID);
//        Log.d(TAG, "itemId = " + itemId);
//
//        String title = null;
//        String artist = null;
//        Uri imageUrl = null;
//
//        // Extract item metadata, if available.
//        if (statusBundle.containsKey(MediaControlIntent.EXTRA_ITEM_METADATA)) {
//            Bundle metadataBundle = (Bundle) statusBundle.getParcelable(
//                    MediaControlIntent.EXTRA_ITEM_METADATA);
//
//            title = metadataBundle.getString(MediaItemMetadata.KEY_TITLE);
//            artist = metadataBundle.getString(MediaItemMetadata.KEY_ARTIST);
//            if (metadataBundle.containsKey(MediaItemMetadata.KEY_ARTWORK_URI)) {
//                imageUrl = Uri.parse(metadataBundle.getString(MediaItemMetadata.KEY_ARTWORK_URI));
//            }
//        } else {
//            Log.d(TAG, "status bundle had no metadata!");
//        }
//
//        // Extract the item status, if available.
//        if ((itemId != null) && statusBundle.containsKey(MediaControlIntent.EXTRA_ITEM_STATUS)) {
//            Bundle itemStatusBundle = (Bundle) statusBundle.getParcelable(MediaControlIntent.EXTRA_ITEM_STATUS);
//            MediaItemStatus itemStatus = MediaItemStatus.fromBundle(itemStatusBundle);
//
//            int playbackState = itemStatus.getPlaybackState();
//            Log.d(TAG, "playbackState=" + playbackState);
//
//            if ((playbackState == MediaItemStatus.PLAYBACK_STATE_CANCELED)
//                    || (playbackState == MediaItemStatus.PLAYBACK_STATE_INVALIDATED)
//                    || (playbackState == MediaItemStatus.PLAYBACK_STATE_ERROR)
//                    || (playbackState == MediaItemStatus.PLAYBACK_STATE_FINISHED)) {
////                clearCurrentMediaItem();
//                mStreamAdvancing = false;
//            } else if ((playbackState == MediaItemStatus.PLAYBACK_STATE_PAUSED)
//                    || (playbackState == MediaItemStatus.PLAYBACK_STATE_PLAYING)
//                    || (playbackState == MediaItemStatus.PLAYBACK_STATE_BUFFERING)) {
//
//                int playerState = PLAYER_STATE_NONE;
//                if (playbackState == MediaItemStatus.PLAYBACK_STATE_PAUSED) {
//                    playerState = PLAYER_STATE_PAUSED;
//                } else if (playbackState == MediaItemStatus.PLAYBACK_STATE_PLAYING) {
//                    playerState = PLAYER_STATE_PLAYING;
//                } else if (playbackState == MediaItemStatus.PLAYBACK_STATE_BUFFERING) {
//                    playerState = PLAYER_STATE_BUFFERING;
//                }
//
//                setPlayerState(playerState);
//                mCurrentItemId = itemId;
//                setCurrentMediaMetadata(title, artist, imageUrl);
//                updateButtonStates();
//
//                mStreamDuration = itemStatus.getContentDuration();
//                mLastKnownStreamPosition = itemStatus.getContentPosition();
//                mStreamPositionTimestamp = itemStatus.getTimestamp();
//
//                Log.d(TAG, "stream position now: " + mLastKnownStreamPosition);
//
//                // Only refresh playback position if stream is moving.
//                mStreamAdvancing = (playbackState == MediaItemStatus.PLAYBACK_STATE_PLAYING);
//                if (mStreamAdvancing) {
//                    refreshPlaybackPosition(mLastKnownStreamPosition, mStreamDuration);
//                }
//            } else {
//                Log.d(TAG, "Unexpected playback state: " + playbackState);
//            }
//
//            Bundle extras = itemStatus.getExtras();
//            if (extras != null) {
//                if (extras.containsKey(MediaItemStatus.EXTRA_HTTP_STATUS_CODE)) {
//                    int httpStatus = extras.getInt(MediaItemStatus.EXTRA_HTTP_STATUS_CODE);
//                    Log.d(TAG, "HTTP status: " + httpStatus);
//                }
//                if (extras.containsKey(MediaItemStatus.EXTRA_HTTP_RESPONSE_HEADERS)) {
//                    Bundle headers = extras.getBundle(MediaItemStatus.EXTRA_HTTP_RESPONSE_HEADERS);
//                    Log.d(TAG, "HTTP headers: " + headers);
//                }
//            }
//        }
//    }

    private void processSessionStatusBundle(Bundle statusBundle) {
        Log.d(TAG, "processSessionStatusBundle()");

        String sessionId = statusBundle.getString(MediaControlIntent.EXTRA_SESSION_ID);
        MediaSessionStatus status = MediaSessionStatus.fromBundle(
                statusBundle.getBundle(MediaControlIntent.EXTRA_SESSION_STATUS));
        int sessionState = status.getSessionState();

        Log.d(TAG, "got a session status update for session " + sessionId + ", state = "
                + sessionState + ", mSessionId=" + mSessionId);

        if (mSessionId == null) {
            return;
        }

        if (!mSessionId.equals(sessionId)) {
            // Got status on a session other than the one we're tracking. Ignore it.
            Log.d(TAG, "Received status for unknown session: " + sessionId);
            return;
        }

        switch (sessionState) {
            case MediaSessionStatus.SESSION_STATE_ACTIVE:
                Log.d(TAG, "session " + sessionId + " is ACTIVE");
                mSessionActive = true;
                syncStatus();
                break;

            case MediaSessionStatus.SESSION_STATE_ENDED:
                Log.d(TAG, "session " + sessionId + " is ENDED");
                mSessionId = null;
                mSessionActive = false;
//                clearCurrentMediaItem();
                break;

            case MediaSessionStatus.SESSION_STATE_INVALIDATED:
                Log.d(TAG, "session " + sessionId + " is INVALIDATED");
                mSessionId = null;
                mSessionActive = false;
//                clearCurrentMediaItem();
                break;

            default:
                Log.d(TAG, "Received unexpected session state: " + sessionState);
                break;
        }
    }

    private void loadListTask(){
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
            }
        };
    }

    private Response.ErrorListener responseError(){
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                i("TV Response error:" + error.getMessage());
            }
        };
    }

    private void setListViewData(ArrayList<Channel> channels){
        adapterListChannels = new AdapterListChannels(this, R.layout.item_channel, channels);
        listView.setAdapter(adapterListChannels);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (errorCode != ConnectionResult.SUCCESS) {
            GooglePlayServicesUtil.getErrorDialog(errorCode, this, REQUEST_GMS_ERROR).show();
        }

        loadListTask();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AdapterListChannels adapterListChannels1 = (AdapterListChannels)parent.getAdapter();
                Channel item = adapterListChannels1.getItem(position);
                playLiveStream(item.convertToMediaInfo());
            }
        });
    }

    MediaInfo tvMediaInfo;

    private void playLiveStream(MediaInfo mediaInfo){
//        loadInProgress(true);
        tvMediaInfo = mediaInfo;
        jsonUTF8Request = new JsonUTF8Request(
                Request.Method.GET,
                mediaInfo.getContentId(),
                null,
                responseListener1(),
                responseError1());
        jsonUTF8Request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(jsonUTF8Request);
    }

    private Response.Listener<JSONObject> responseListener1(){
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                i("TV MediaInfo Response:" + response.toString());
                MediaTVItem mediaTVItem = new MediaTVItem(tvMediaInfo, response);
                sendIntentToRoute(mediaTVItem.getMediaInfo());
//                loadInProgress(false);
            }
        };
    }

    private Response.ErrorListener responseError1(){
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
                }
//                loadInProgress(false);
            }
        };
    }

    @Override
    protected void onRouteSelected(MediaRouter.RouteInfo route) {
        mCurrentRoute = route;
        CastDevice device = CastDevice.getFromBundle(route.getExtras());
        setSelectedDevice(device);
    }

    @Override
    protected void onRouteUnselected(MediaRouter.RouteInfo route) {
        setSelectedDevice(null);
        mSessionActive = false;
        mSessionId = null;
    }

    @Override
    protected void onConnected(Bundle bundle) {
        i("startSession");
        startSession();
    }

    private void sendIntentToRoute(MediaInfo media){
        MediaMetadata metadata = media.getMetadata();
        Log.d("TAG", "Casting " + metadata.getString(MediaMetadata.KEY_TITLE) + " (" + media.getContentType() + ")");

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
        Log.d(TAG, "sending intent to route: " + intent + ", session: " + sessionId);
        if ((mCurrentRoute == null) || !mCurrentRoute.supportsControlRequest(intent)) {
            Log.d(TAG, "route is null or doesn't support this request");
            return;
        }

        mCurrentRoute.sendControlRequest(intent, new MediaRouter.ControlRequestCallback() {
            @Override
            public void onResult(Bundle data) {
                Log.d(TAG, "got onResult for " + intent.getAction() + " with bundle " + data);
                if (data != null) {
                    if (resultHandler != null) {
                        resultHandler.handleResult(data);
                    }
                } else {
                    Log.w(TAG, "got onResult with a null bundle");
                }
            }

            @Override
            public void onError(String message, Bundle data) {
//                showErrorDialog(message != null ? message : getString(R.string.mrp_request_failed));
                //todo
            }
        });
    }

    // Session control.

    private void startSession() {
        Intent intent = new Intent(MediaControlIntent.ACTION_START_SESSION);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_STATUS_UPDATE_RECEIVER,
                mSessionStatusUpdateIntent);
        intent.putExtra(CastMediaControlIntent.EXTRA_CAST_APPLICATION_ID,
                getReceiverApplicationId());
        intent.putExtra(CastMediaControlIntent.EXTRA_CAST_RELAUNCH_APPLICATION, false);
        intent.putExtra(CastMediaControlIntent.EXTRA_DEBUG_LOGGING_ENABLED, true);
        intent.putExtra(CastMediaControlIntent.EXTRA_CAST_STOP_APPLICATION_WHEN_SESSION_ENDS, false);
        sendIntentToRoute(intent, new ResultBundleHandler() {
            @Override
            public void handleResult(Bundle bundle) {
                mSessionId = bundle.getString(MediaControlIntent.EXTRA_SESSION_ID);
                Log.d(TAG, "Got a session ID of: " + mSessionId);
            }
        });
    }

    private void syncStatus() {
        Log.d(TAG, "Invoking SYNC_STATUS request");
        Intent intent = new Intent(CastMediaControlIntent.ACTION_SYNC_STATUS);
        intent.addCategory(CastMediaControlIntent.categoryForRemotePlayback());
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        intent.putExtra(MediaControlIntent.EXTRA_ITEM_STATUS_UPDATE_RECEIVER,
                mMediaStatusUpdateIntent);
        sendIntentToRoute(intent, mMediaResultHandler);
    }

    private void requestSessionStatus() {
        if (mSessionId == null) {
            return;
        }

        Intent intent = new Intent(MediaControlIntent.ACTION_GET_SESSION_STATUS);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        sendIntentToRoute(intent, null);
    }

    private void endSession() {
        if (mSessionId == null) {
            return;
        }

        Intent intent = new Intent(MediaControlIntent.ACTION_END_SESSION);
        intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, mSessionId);
        sendIntentToRoute(intent, new ResultBundleHandler() {
            @Override
            public void handleResult(final Bundle bundle) {
                MediaSessionStatus status = MediaSessionStatus.fromBundle(
                        bundle.getBundle(MediaControlIntent.EXTRA_SESSION_STATUS));
                int sessionState = status.getSessionState();
                Log.d(TAG, "session state after ending session: " + sessionState);
//                clearCurrentMediaItem();
            }
        });
        mSessionId = null;
    }
}
