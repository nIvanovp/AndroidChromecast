package com.altabel.cast.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.altabel.cast.R;
import com.altabel.cast.app.ApplicationChromeCast;
import com.altabel.cast.item.Channel;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import java.util.ArrayList;

/**
 * Created by nikolai on 23.07.2014.
 */
public class AdapterListChannels extends ArrayAdapter<Channel> {
    private Context mContext;
    private ArrayList<Channel> mChannels;
    private LayoutInflater mLayoutInflater;
    private int mLayoutResourceId;
    private DisplayImageOptions options;

    public AdapterListChannels(Context context, int resource, ArrayList<Channel> data) {
        super(context, resource, data);
        this.mContext = context;
        this.mLayoutResourceId = resource;
        this.mChannels = data;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        options = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.empty)
                .showImageOnFail(R.drawable.empty)
                .resetViewBeforeLoading(true)
                .cacheOnDisc(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .considerExifParams(true)
                .displayer(new FadeInBitmapDisplayer(300))
                .build();
    }

    @Override
    public int getCount() {
        return mChannels.size();
    }

    @Override
    public Channel getItem(int position) {
        return mChannels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RecordHolder recordHolder;
        if(convertView == null){
            convertView = mLayoutInflater.inflate(mLayoutResourceId, parent, false);
            recordHolder = new RecordHolder();
            recordHolder.nameChannel = (TextView) convertView.findViewById(R.id.textView);
            recordHolder.summaryChannel = (TextView) convertView.findViewById(R.id.textView2);
            recordHolder.imageView = (ImageView)convertView.findViewById(R.id.imageView);
            convertView.setTag(recordHolder);
        }else{
            recordHolder = (RecordHolder)convertView.getTag();
        }

        Channel channel = getItem(position);
        recordHolder.nameChannel.setText(channel.getName());
        recordHolder.summaryChannel.setText("summary");
        recordHolder.imageView.setImageDrawable(null);
        ApplicationChromeCast.nostraImageLoader.displayImage(channel.getImageStrUrl(), recordHolder.imageView, options);
        return convertView;
    }

    static class RecordHolder {
        TextView nameChannel;
        TextView summaryChannel;
        ImageView imageView;
    }
}
