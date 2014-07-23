package com.altabel.cast.item;

import android.net.Uri;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.altabel.cast.utils.LoggerUtils.i;

/**
 * Created by nikolai on 25.06.2014.
 */
public class MediaTVItem {
    private static final String CHANNEL_ID = "channel_id";
    private static final String CHANNEL_URL = "url";
    private static final String CHANNEL_STATE = "state";
    private String channelId;
    private String url;
    private String state;
    private String mimeType = "video/mp4";
    private MediaInfo tvMediaInfo;

    public MediaTVItem(MediaInfo tvMediaInfo, JSONObject jsonObject) {
        this.tvMediaInfo = tvMediaInfo;
        parse(jsonObject);
    }

    public MediaInfo getMediaInfo(){
        int metadataType = MediaMetadata.MEDIA_TYPE_MOVIE;
        MediaMetadata tvMetadata = tvMediaInfo.getMetadata();
        MediaMetadata metadata = new MediaMetadata(metadataType);

        metadata.putString(MediaMetadata.KEY_TITLE, tvMetadata.getString(MediaMetadata.KEY_TITLE));

        List<WebImage> images = tvMetadata.getImages();
        if ((images != null) && !images.isEmpty()) {
            Uri imageUrl = images.get(0).getUrl();
            if (imageUrl != null)
                metadata.addImage(new WebImage(imageUrl));
        }

        MediaInfo mediaInfo = new MediaInfo.Builder(getUrl())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(mimeType)
                .setMetadata(metadata)
                .build();

        return mediaInfo;
    }

    private void parse(JSONObject jsonObject){
        try {
            this.setChannelId(jsonObject.getString(CHANNEL_ID));
            this.setUrl(jsonObject.getString(CHANNEL_URL));
            this.setState(jsonObject.getString(CHANNEL_STATE));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        i("TV MediaInfo channel id:" + channelId);
        this.channelId = channelId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        i("TV MediaInfo url:" + url);
        this.url = url;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        i("TV MediaInfo state:" + state);
        this.state = state;
    }
}
