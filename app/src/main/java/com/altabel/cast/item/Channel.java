package com.altabel.cast.item;

import android.net.Uri;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import org.json.JSONException;
import org.json.JSONObject;

import static com.altabel.cast.utils.LoggerUtils.i;

/**
 * Created by nikolai on 23.07.2014.
 */
public class Channel {
    public static final String CHANNELS = "channels";
    private static final String CHANNEL_ID = "channel_id";
    private static final String CHANNEL_URL = "url";
    private static final String CHANNEL_NAME = "name";
    private static final String CHANNEL_IMAGE = "image_url";
    private static final String CHANNEL_DESCRIPTION = "description";
    private String channelId;
    private String name;
    private String imageUrl;
    private String url;
    private String description;
    private String mimeType = "video/mp4";

    public Channel(JSONObject jsonObject) {
        parse(jsonObject);
    }

    public MediaInfo convertToMediaInfo(){
        int metadataType = MediaMetadata.MEDIA_TYPE_MOVIE;
        MediaMetadata metadata = new MediaMetadata(metadataType);

        if (name != null) {
            metadata.putString(MediaMetadata.KEY_TITLE, name);
        }

        if (imageUrl != null) {
            metadata.addImage(new WebImage(getImageUrl()));
        }

        MediaInfo mediaInfo = new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType(mimeType)
                .setMetadata(metadata)
                .build();

        return mediaInfo;
    }

    private void parse(JSONObject jsonChannel){
        try {
            this.setChannelId(jsonChannel.getString(CHANNEL_ID));
            this.setName(jsonChannel.getString(CHANNEL_NAME));
            this.setUrl(jsonChannel.getString(CHANNEL_URL));
            this.setImageUrl(jsonChannel.getString(CHANNEL_IMAGE));
            this.setDescription(jsonChannel.getString(CHANNEL_DESCRIPTION));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        i("Channel name:" + name);
        this.name = name;
    }

    public String getImageStrUrl() {
        return imageUrl;
    }

    public Uri getImageUrl() {
        Uri imageUri = (imageUrl != null) ? Uri.parse(imageUrl) : null;
        return imageUri;
    }

    public void setImageUrl(String imageUrl) {
        i("Channel image url:" + imageUrl);
        this.imageUrl = imageUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        i("Channel url:" + url);
        this.url = url;
    }
}
