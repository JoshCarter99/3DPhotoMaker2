package com.example.joshcarter.a3dphotomaker2;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static java.lang.Math.round;

/*
 * Created by JoshCarter on 21/02/2018.
 */

public class CombinePhotos extends AppCompatActivity{

    public Bitmap picL,picLC, picR,picRC,picLR, alignedPic;
    public Uri fileLeft, fileRight;
    public ImageView comPic;
    public File file;
    public double picWidth, picHeight, picRatio;
    LruCache<String, Bitmap> mMemoryCache;
    BitmapWorkerTask BitmapWorker;
    public ImageButton AutoAlignButton;

    public int newPicHeight, newPicWidth;

    static int orientation;

    String photoKey = "photoKey";
    String photoKeyLeft = "photoLeft";
    String photoLeyRight = "photoRight";
    String photoKeyAligned = "photoKeyAligned";

    public int ALIGN_COUNTER=0;
    public int OrientationOnRightPic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combine);

        // Locking orientation
        /*int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }*/
        //

        //currentOrientation = this.getResources().getConfiguration().orientation;

        comPic = findViewById(R.id.anaglyph);
        AutoAlignButton = findViewById(R.id.autoAlignButton);

        /////////
        RetainFragment retainFragment =
                RetainFragment.findOrCreateRetainFragment(getFragmentManager());
        mMemoryCache = retainFragment.mRetainedCache;


        if (mMemoryCache != null) {
            if(savedInstanceState!=null){
                ALIGN_COUNTER = savedInstanceState.getInt("AlignCounter");
                OrientationOnRightPic = savedInstanceState.getInt("Orientation");
            }

            if (OrientationOnRightPic == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }

            if (ALIGN_COUNTER==0) {
                picLR = mMemoryCache.get(photoKey);
            }else{
                picLR = mMemoryCache.get(photoKeyAligned);
            }
            //picLR = BitmapWorker.getAnswerFromMemoryCache(photoKey);
            comPic.setImageBitmap(picLR);
            picLR = null;
        } else{

            //initialCurrentOrientation = this.getResources().getConfiguration().orientation;

            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            // Use 1/8th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 8;
            mMemoryCache = new LruCache<String, Bitmap>(cacheSize){
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };

            retainFragment.mRetainedCache = mMemoryCache;

            Intent intent = getIntent();

            fileLeft = intent.getParcelableExtra(photoKeyLeft);
            fileRight = intent.getParcelableExtra(photoLeyRight);
            OrientationOnRightPic = intent.getIntExtra("Orientation",0);

            if (OrientationOnRightPic == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }

            try {
                picL = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileLeft);
                picR = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileRight);
            } catch (IOException e) {
                e.printStackTrace();
            }

            picHeight = picL.getHeight();
            picWidth = picL.getWidth();

            /// maybe move this above rotation???
            if(picHeight>1920||picWidth>1920){
                if(picHeight>=picWidth) {
                    picRatio = picWidth / picHeight;
                    newPicHeight = 1920;
                    newPicWidth = (int)round(newPicHeight*picRatio);
                }else{
                    picRatio = picHeight / picWidth;
                    newPicWidth = 1920;
                    newPicHeight = (int)round(newPicWidth*picRatio);
                }
                picL = Bitmap.createScaledBitmap(picL,newPicWidth, newPicHeight, true);
                picR = Bitmap.createScaledBitmap(picR,newPicWidth, newPicHeight, true);
            }


            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            double height = displayMetrics.heightPixels;
            double width = displayMetrics.widthPixels;

            if((picL.getHeight()*width!=picL.getWidth()*height)||(picL.getHeight()*height!=picL.getWidth()*width)){
                if(height>width){
                    double ratio = width/height;
                    int newHeight = (int) round(picL.getWidth()*ratio);
                    picL=Bitmap.createBitmap(picL, 0,picL.getHeight()-newHeight,picL.getWidth(), newHeight);
                    picR=Bitmap.createBitmap(picR, 0,picR.getHeight()-newHeight,picR.getWidth(), newHeight);
                }else{
                    double ratio = height/width;
                    int newHeight = (int) round(picL.getWidth()*ratio);
                    int startPoint = (int) round(((double)(picL.getHeight()-newHeight))/2);
                    picL=Bitmap.createBitmap(picL, 0,startPoint,picL.getWidth(), newHeight);
                    picR=Bitmap.createBitmap(picR, 0,startPoint,picR.getWidth(), newHeight);
                }
            }


            try {
                ExifInterface exif = new ExifInterface(fileLeft.getPath());
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                Log.d("EXIF", "Exif: " + orientation);
                Matrix matrix = new Matrix();
                Log.d("orientation111", Integer.toString(orientation));
                if (orientation == 6) {
                    matrix.postRotate(90);
                } else if (orientation == 3) {
                    matrix.postRotate(180);
                } else if (orientation == 8) {
                    matrix.postRotate(270);
                }
                picL = Bitmap.createBitmap(picL, 0, 0, picL.getWidth(), picL.getHeight(), matrix, true); // rotating bitmap
                picR = Bitmap.createBitmap(picR, 0, 0, picR.getWidth(), picR.getHeight(), matrix, true); // rotating bitmap
            } catch (Exception e) {
            }

            picLR = AutoAlign.combinePhotos(picL, picR,0);

            Log.d("timeStarts","timer3");
        }


        comPic.setImageBitmap(picLR);


        /*if ((orientation==6 && currentOrientation2 ==1) || (orientation == 1 && currentOrientation2 == 2)){
            comPic.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            comPic.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }*/

        //int currentOrientation = getResources().getConfiguration().orientation;
        Log.d(Integer.toString(orientation),Integer.toString(Configuration.ORIENTATION_LANDSCAPE));
        Log.d(Integer.toString(orientation),Integer.toString(Configuration.ORIENTATION_PORTRAIT));
        if (orientation == 1) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

        BitmapWorker = (BitmapWorkerTask) new BitmapWorkerTask().execute(10l);

        BitmapWorker.addMMemoryCache(mMemoryCache);

        BitmapWorker.addAnswerToMemoryCache(photoKey,picLR);
        picLR = null;

    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("AlignCounter", ALIGN_COUNTER);
        outState.putInt("Orientation",OrientationOnRightPic);
    }


    public void backButton(View view){

        Intent backIntent = new Intent(CombinePhotos.this, CameraActivity.class);
        startActivity(backIntent);
    }

    public void saveButton(View view){
        if(ALIGN_COUNTER==0){
            picLR = mMemoryCache.get(photoKey);
        }else{
            picLR = mMemoryCache.get(photoKeyAligned);
        }
        try {
            file = getOutputMediaFile();
            OutputStream fOut = null;
            fOut = new FileOutputStream(file);
            picLR.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
            showToast("Saved!");
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Not Saved");
        }
        picLR = null;
    }

    public void autoAlignButton(View view){
        if(ALIGN_COUNTER==0){
            if(mMemoryCache.get(photoKeyAligned)!=null){
                comPic.setImageBitmap(mMemoryCache.get(photoKeyAligned));
            }else{
                alignedPic = AutoAlign.alignAllCols(picL,picR);
                comPic.setImageBitmap(alignedPic);
                BitmapWorker.addAnswerToMemoryCache(photoKeyAligned,alignedPic);
                alignedPic = null;
            }
            showToast("Aligned");
            AutoAlignButton.setBackgroundResource(R.drawable.aligned_4);
            ALIGN_COUNTER++;
        }else{
            comPic.setImageBitmap(mMemoryCache.get(photoKey));
            AutoAlignButton.setBackgroundResource(R.drawable.align_4);
            ALIGN_COUNTER=0;
        }



    }

    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "3DPhotoMaker2");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d("3DPhotoMaker2", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
    }

    private void showToast(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                }
            });
    }

}
