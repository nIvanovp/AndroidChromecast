package com.altabel.cast.app;

import android.app.Application;
import android.graphics.Bitmap;
import com.nostra13.universalimageloader.cache.disc.impl.TotalSizeLimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;

/**
 * Created by nikolai on 23.07.2014.
 */
public class ApplicationChromeCast extends Application {
    public static com.nostra13.universalimageloader.core.ImageLoader nostraImageLoader;

    @Override
    public void onCreate() {
        super.onCreate();
        File cacheDir = StorageUtils.getCacheDirectory(getApplicationContext());
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .memoryCacheExtraOptions(150, 200) // width, height
//                .memoryCache(new LruMemoryCache(2 * 1024 * 1024))
//                .memoryCacheSize(2 * 1024 * 1024)
                .discCacheExtraOptions(150, 200, Bitmap.CompressFormat.PNG, 75, null)
                .tasksProcessingOrder(QueueProcessingType.LIFO)
//                .discCache(new UnlimitedDiscCache(cacheDir)) // default
                .discCache(new TotalSizeLimitedDiscCache(cacheDir, 20*1024*1024))
//                .discCacheSize(50 * 1024 * 1024)
//                .discCacheFileCount(100)
                .threadPoolSize(5)
                .threadPriority(Thread.MIN_PRIORITY + 2)
                .denyCacheImageMultipleSizesInMemory()
                .imageDownloader(new BaseImageDownloader(getApplicationContext(), 5 * 1000, 30 * 1000))
                .discCacheFileNameGenerator(new HashCodeFileNameGenerator()) // default
                .build();
        nostraImageLoader = com.nostra13.universalimageloader.core.ImageLoader.getInstance();
        nostraImageLoader.init(config);
    }
}
