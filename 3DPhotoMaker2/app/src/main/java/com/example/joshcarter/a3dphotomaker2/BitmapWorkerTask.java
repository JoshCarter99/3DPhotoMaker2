package com.example.joshcarter.a3dphotomaker2;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

/**
 * Created by JoshCarter on 27/02/2018.
 */

public class BitmapWorkerTask extends AsyncTask<Long, Integer, Long> {

    public long answer = 0;
    private LruCache<String, Bitmap> mMemoryCache;
    public Bitmap photoBitmap;


    public void addMMemoryCache(LruCache<String, Bitmap> cache){
        mMemoryCache = cache;
    }

    public void addAnswerToMemoryCache(String key, Bitmap photoBitmap) {

        Log.d("bitmap",photoBitmap.toString());
        Log.d("mMemoryCache",mMemoryCache.toString());
        Log.d("hello","bonjour");
        mMemoryCache.put(key, photoBitmap);

    }

    public Bitmap getAnswerFromMemoryCache(String key){
        return mMemoryCache.get(key);
    }


    /*
    public Bitmap getAnswerFromMemCache(String key) {
        Log.d("hello","bonjour");
        return mMemoryCache.get(key);

    }*/

    protected Long doInBackground(Long... params) {
        long result = 0;
        /*for (long i = 0; i < params[0]; i++) {
            for (long j = 0; j < 100000; j++) {
                result += 1;
            }
            if (isCancelled()) break;
            Log.d("i",Long.toString(i));
        }
        Log.d("result1",Long.toString(result));*/

        return result;


    }

    /*
    protected void onPostExecute(Long result) {
        //showDialog("Downloaded " + result + " bytes");
        answer=result;
        Log.d("result2",Long.toString(result));
        Log.d("finished","finsihed");

    }

    public long answer(){
        Log.d("result3",Long.toString(answer));
        return answer;
    }
*/

}

