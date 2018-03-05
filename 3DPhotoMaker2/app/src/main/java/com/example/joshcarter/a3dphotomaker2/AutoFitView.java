package com.example.joshcarter.a3dphotomaker2;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

/**
 * Created by JoshCarter on 20/02/2018.
 */

public class AutoFitView extends TextureView {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public AutoFitView(Context context) {
        this(context, null);
    }

    public AutoFitView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int i=0;
        //Log.d("WidthMeasureSpecwidth",Integer.toString(width));
        //Log.d("HeightMeasureSpecheight",Integer.toString(height));
        //Log.d("WidthRatio",Integer.toString(mRatioWidth));
        //Log.d("HeightRatio",Integer.toString(mRatioHeight));


        /*
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            Log.d("mRatioWidth",Integer.toString(mRatioWidth));
            Log.d("mRatioHeight",Integer.toString(mRatioHeight));
            Log.d("ratioWidth",Integer.toString(width));
            Log.d("ratioHeight",Integer.toString(height));
            if (width < height * mRatioWidth / mRatioHeight) {
                //setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
                Log.d("newWidth1",Integer.toString(height * mRatioWidth / mRatioHeight));
                Log.d("newHeight1",Integer.toString(height));
                setMeasuredDimension(height * mRatioWidth /( mRatioHeight), height);
            } else {
                Log.d("newWidth2",Integer.toString(width));
                Log.d("newHeight2",Integer.toString(width * mRatioHeight / mRatioWidth));
                //setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
                //setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
                setMeasuredDimension(width, height);
            }
        } */

        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width > height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);

            }
        }


    }
}

