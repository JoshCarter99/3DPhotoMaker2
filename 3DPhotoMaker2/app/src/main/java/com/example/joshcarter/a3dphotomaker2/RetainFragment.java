package com.example.joshcarter.a3dphotomaker2;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.FragmentManager;
import android.util.LruCache;

/**
 * Created by JoshCarter on 23/02/2018.
 */

public class RetainFragment extends Fragment {
    private static final String TAG = "RetainFragment";
    public LruCache<String, Bitmap> mRetainedCache;
    public int i;

    public RetainFragment() {}

    public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
        RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new RetainFragment();
            fm.beginTransaction().add(fragment, TAG).commit();
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }


}
