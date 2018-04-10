package com.example.joshcarter.a3dphotomaker2;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

/**
 * Created by JoshCarter on 27/02/2018.
 */

public class BitmapWorkerTask extends AsyncTask<Long, Integer, Long> {

    private LruCache<String, Bitmap> mMemoryCache;


    public void addMMemoryCache(LruCache<String, Bitmap> cache){
        mMemoryCache = cache;
    }

    public void addAnswerToMemoryCache(String key, Bitmap photoBitmap) {

        mMemoryCache.put(key, photoBitmap);

    }


    protected Long doInBackground(Long... params) {

        // Sounds like I should be combining the photo here using: execute.

        long result = 0;

        return result;


    }

}

