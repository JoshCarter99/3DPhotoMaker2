package com.example.joshcarter.a3dphotomaker2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
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

public class CombinePhotos2 extends AppCompatActivity{

    public Bitmap picRC,picLR, alignedPic, combinedPic, manualPic;
    public static Bitmap picL, picR;
    public Uri fileLeft, fileRight;
    public ImageView comPic, InfoLayout;
    public TextView TouchInstructions, VerticalAlignMessage, TouchAlignMessage, ManualAlignMessage, InfoMessage;
    public File file;
    public double picWidth, picHeight, picRatio;
    LruCache<String, Bitmap> mMemoryCache;
    BitmapWorkerTask BitmapWorker;
    public ImageButton AutoAlignButton, TouchAlignButton, ManualAlignButton, SaveButton, BackButton, BackButtonMain, InfoButton;

    public int newPicHeight, newPicWidth;

    static int orientation;

    String photoKey = "photoKey";
    String photoKeyLeft = "photoLeft";
    String photoKeyRight = "photoRight";
    String photoKeyAligned = "photoKeyAligned";
    String photoKeyCombined = "photoKeyCombined";

    public int ALIGN_COUNTER=0;
    public int TOUCH_ALIGN_COUNTER=0;
    public int MANUAL_ALIGN_COUNTER=0;
    // 0 is normal, 1 is autoAlign, 2 is touchAlign, 3 is manualAlign.
    public int CURRENT_PIC=0;
    public static int OrientationOnRightPic;
    public int shiftHor=1;
    public int shiftVer=1;

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
        TouchAlignButton = findViewById(R.id.touchAlignButton);
        ManualAlignButton = findViewById(R.id.manualAlignButton);
        BackButton = findViewById(R.id.backButton);
        BackButtonMain = findViewById(R.id.backButtonMain);
        SaveButton = findViewById(R.id.saveButton);
        TouchInstructions = findViewById(R.id.touchInstructions);
        VerticalAlignMessage = findViewById(R.id.verticalAlignMessage);
        TouchAlignMessage = findViewById(R.id.touchAlignMessage);
        ManualAlignMessage = findViewById(R.id.manualAlignMessage);
        InfoMessage = findViewById(R.id.infoMessage);

        InfoLayout = findViewById(R.id.infoLayout);
        InfoButton = findViewById(R.id.infoButton);

        /////////
        RetainFragment retainFragment =
                RetainFragment.findOrCreateRetainFragment(getFragmentManager());
        mMemoryCache = retainFragment.mRetainedCache;

        Log.d("Hello","bonjour");

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

            if (ALIGN_COUNTER==1) {
                picLR = mMemoryCache.get(photoKeyAligned);
            }else{
                picLR = mMemoryCache.get(photoKey);
            }

            //TouchAlignButton.setVisibility(View.GONE);

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

            BitmapWorker = (BitmapWorkerTask) new BitmapWorkerTask().execute(10l);

            BitmapWorker.addMMemoryCache(mMemoryCache);

            ////

            if(getIntent().getIntExtra("shiftHor",1)!=1){

                combinedPic=null;



                //ManualAlignButton.setBackgroundResource(R.drawable.touch_aligned_5);


                CURRENT_PIC=3;

                //OrientationOnRightPic = getIntent().getIntExtra("Orientation",0);
                Log.d("OrientationOnRightPic",Integer.toString(OrientationOnRightPic));
                if (OrientationOnRightPic == Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }
                else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                }

                shiftHor = getIntent().getIntExtra("shiftHor",1);
                shiftVer = getIntent().getIntExtra("shiftVer",1);
                manualPic = AutoAlign2.combinePhotos(picL,picR, shiftVer, shiftHor);
                comPic.setImageBitmap(manualPic);
                MANUAL_ALIGN_COUNTER=1;


                if(ALIGN_COUNTER!=0){
                    AutoAlignButton.setBackgroundResource(R.drawable.align_5);
                    ALIGN_COUNTER=0;
                }

                if(TOUCH_ALIGN_COUNTER!=0){
                    TouchAlignButton.setBackgroundResource(R.drawable.touch_align_5);
                    TOUCH_ALIGN_COUNTER=0;
                }
            } else {
                Intent intent = getIntent();

                fileLeft = intent.getParcelableExtra(photoKeyLeft);
                fileRight = intent.getParcelableExtra(photoKeyRight);
                OrientationOnRightPic = intent.getIntExtra("Orientation",0);

                Log.d("OrientationOfRightPic", Integer.toString(OrientationOnRightPic));

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

                /*picL = BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.dog);
                picR = BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.dog);*/


                picHeight = picL.getHeight();
                picWidth = picL.getWidth();

                Log.d("picHeight",Double.toString(picHeight));
                Log.d("picWidth",Double.toString(picWidth));

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

                //comPic.setImageBitmap(picL);

                Log.d("picHeightAfterStrech",Integer.toString(picL.getHeight()));
                Log.d("picWidthAfterStrech",Integer.toString(picL.getWidth()));


                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                double height = displayMetrics.heightPixels;
                double width = displayMetrics.widthPixels;

                Log.d("Height",Double.toString(height));
                Log.d("Width",Double.toString(width));

                if((picL.getHeight()*width!=picL.getWidth()*height)||(picL.getHeight()*height!=picL.getWidth()*width)){
                    Log.d("Helllooooo","wow");

                    /*if(picL.getHeight()>picL.getWidth()){
                        if(height>width){
                            double ratio = width/height;
                            int newWidth = (int) round(picL.getHeight()*ratio);
                            Log.d("ratio1", Double.toString(ratio));
                            Log.d("WIDTH0", Integer.toString(picL.getWidth()));
                            Log.d("HEIGHT0", Integer.toString(newWidth));
                            //int startPoint = (int) round(((double)(picL.getHeight()-newHeight))/2);
                            picL=Bitmap.createBitmap(picL, 0,0,newWidth, picL.getHeight());
                            picR=Bitmap.createBitmap(picR, 0,0,newWidth, picR.getHeight());
                        }else{
                            double ratio = height/width;
                            int newWidth = (int) round(picL.getHeight()*ratio);
                            Log.d("ratio1", Double.toString(ratio));
                            Log.d("WIDTH1", Integer.toString(picL.getWidth()));
                            Log.d("HEIGHT1", Integer.toString(newWidth));
                            //int startPoint = (int) round(((double)(picL.getHeight()-newHeight))/2);
                            picL=Bitmap.createBitmap(picL, 0,picL.getWidth()-newWidth, picL.getHeight(),newWidth);
                            picR=Bitmap.createBitmap(picR, 0,picL.getWidth()-newWidth, picL.getHeight(),newWidth);
                        }
                    }else {
                        if(height>width) {
                            double ratio = width / height;
                            int newHeight = (int) round(picL.getWidth() * ratio);
                            Log.d("ratio2", Double.toString(ratio));
                            Log.d("WIDTH2", Integer.toString(picL.getWidth()));
                            Log.d("HEIGHT2", Integer.toString(newHeight));
                            //picL = Bitmap.createBitmap(picL, 0, picL.getHeight() - newHeight, picL.getWidth(), newHeight);
                            //picR = Bitmap.createBitmap(picR, 0, picR.getHeight() - newHeight, picR.getWidth(), newHeight);
                        }else{

                            double ratio = height/width;
                            int newHeight = (int) round(picL.getWidth()*ratio);
                            Log.d("ratio3", Double.toString(ratio));
                            Log.d("WIDTH3", Integer.toString(picL.getWidth()));
                            Log.d("HEIGHT3", Integer.toString(newHeight));
                            int startPoint = (int) round(((double)(picL.getHeight()-newHeight))/2);
                            picL=Bitmap.createBitmap(picL, 0,startPoint,picL.getWidth(), newHeight);
                            picR=Bitmap.createBitmap(picR, 0,startPoint,picR.getWidth(), newHeight);
                        }
                    }*/

                    if(picL.getHeight()>picL.getWidth()){
                        if(height>width){
                            if(picL.getHeight()*width>picL.getWidth()*height){
                                Log.d("WIDTH3", Integer.toString(1));
                                double ratio = height/width;
                                int newHeight = (int) round(picL.getWidth()*ratio);
                                picL=Bitmap.createBitmap(picL, 0,0,picL.getWidth(), newHeight);
                                picR=Bitmap.createBitmap(picR, 0,0,picR.getWidth(), newHeight);

                            }else{
                                // WORKS
                                Log.d("WIDTH3", Integer.toString(2));
                                double ratio = width/height;
                                int newWidth = (int) round(picL.getHeight()*ratio);
                                picL=Bitmap.createBitmap(picL, 0,0,newWidth, picL.getHeight());
                                picR=Bitmap.createBitmap(picR, 0,0,newWidth, picR.getHeight());
                            }
                        }else{
                            if(picL.getHeight()*height>picL.getWidth()*width){
                                Log.d("WIDTH3", Integer.toString(3));
                                double ratio = width/height;
                                int newHeight = (int) round(picL.getWidth()*ratio);
                                picL=Bitmap.createBitmap(picL, 0,0,picL.getWidth(), newHeight);
                                picR=Bitmap.createBitmap(picR, 0,0,picR.getWidth(), newHeight);

                            }else{
                                Log.d("WIDTH3", Integer.toString(4));
                                double ratio = height/width;
                                int newWidth = (int) round(picL.getHeight()*ratio);
                                picL=Bitmap.createBitmap(picL, 0,0,newWidth, picL.getHeight());
                                picR=Bitmap.createBitmap(picR, 0,0,newWidth, picR.getHeight());
                            }
                        }
                    }else {
                        if(height>width) {
                            if(picL.getWidth()*width>picL.getHeight()*height){
                                // WORKS - maybe
                                Log.d("WIDTH3", Integer.toString(5));

                                double ratio = height/width;
                                int newWidth = (int) round(picL.getHeight() * ratio);
                                int startPoint = (int) round(((double)(picL.getWidth()-newWidth))/2);
                                picL = Bitmap.createBitmap(picL, startPoint, 0, newWidth, picL.getHeight());
                                picR = Bitmap.createBitmap(picR, startPoint, 0, newWidth, picR.getHeight());


                            }else{
                                // WORKS
                                Log.d("WIDTH3", Integer.toString(6));
                                double ratio = width/height;
                                int newWidth = (int) round(picL.getWidth() * ratio);
                                picL = Bitmap.createBitmap(picL, 0, picL.getHeight() - newWidth, picL.getWidth(), newWidth);
                                picR = Bitmap.createBitmap(picR, 0, picR.getHeight() - newWidth, picR.getWidth(), newWidth);
                            }
                        }else{
                            if(picL.getWidth()*height>picL.getHeight()*width){
                                // WORKS - maybe
                                Log.d("WIDTH3", Integer.toString(7));

                                double ratio = width/height;
                                int newWidth = (int) round(picL.getHeight() * ratio);
                                picL = Bitmap.createBitmap(picL, picL.getWidth()-newWidth, 0, newWidth, picL.getHeight());
                                picR = Bitmap.createBitmap(picR, picR.getWidth()-newWidth, 0, newWidth, picR.getHeight());


                            }else{
                                // WORKS
                                Log.d("WIDTH3", Integer.toString(8));
                                double ratio = height/width;
                                int newWidth = (int) round(picL.getWidth() * ratio);
                                int startPoint = (int) round(((double)(picL.getHeight()-newWidth))/2);
                                picL = Bitmap.createBitmap(picL, 0, startPoint, picL.getWidth(), newWidth);
                                picR = Bitmap.createBitmap(picR, 0, startPoint, picR.getWidth(), newWidth);
                                //picR = Bitmap.createBitmap(picR, 0, picR.getHeight() - newWidth, picR.getWidth(), newWidth);
                            }
                        }
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

                picLR = AutoAlign2.combinePhotos(picL, picR,0,0);

                Log.d("timeStarts","timer3");

                comPic.setImageBitmap(picLR);

                CURRENT_PIC=0;


                /*if ((orientation==6 && currentOrientation2 ==1) || (orientation == 1 && currentOrientation2 == 2)){
                comPic.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else {
                comPic.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }*/

                //int currentOrientation = getResources().getConfiguration().orientation;
                /*Log.d(Integer.toString(orientation),Integer.toString(Configuration.ORIENTATION_LANDSCAPE));
                Log.d(Integer.toString(orientation),Integer.toString(Configuration.ORIENTATION_PORTRAIT));
                if (orientation == 1) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                }*/

                BitmapWorker.addAnswerToMemoryCache(photoKey,picLR);
                picLR = null;
                //new File(fileLeft.getPath()).delete();
                //new File(fileRight.getPath()).delete();
            }

        }

    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("AlignCounter", ALIGN_COUNTER);
        outState.putInt("Orientation",OrientationOnRightPic);
    }

    public void backButtonMain(View view){
        Intent backIntent = new Intent(this, CameraActivity.class);
        startActivity(backIntent);
    }


    public void backButton(View view){

        Log.d("current pic",Integer.toString(CURRENT_PIC));

        AutoAlignButton.setVisibility(View.VISIBLE);
        TouchAlignButton.setVisibility(View.VISIBLE);
        SaveButton.setVisibility(View.VISIBLE);
        ManualAlignButton.setVisibility(View.VISIBLE);
        BackButtonMain.setVisibility(View.VISIBLE);

        BackButton.setVisibility(View.GONE);
        TouchInstructions.setVisibility(View.GONE);


        switch (CURRENT_PIC){
            case 3:
                Log.d("Hello",Integer.toString(3));
                comPic.setImageBitmap(manualPic);
                break;
            case 2:
                Log.d("Hello",Integer.toString(2));
                comPic.setImageBitmap(combinedPic);
                break;
            case 1:
                Log.d("Hello",Integer.toString(1));
                comPic.setImageBitmap(mMemoryCache.get(photoKeyAligned));
                break;
            default:
                Log.d("Hello",Integer.toString(0));
                comPic.setImageBitmap(mMemoryCache.get(photoKey));
                break;
        }

        comPic.setOnTouchListener(null);
    }

    public void saveButton(View view){

        switch (CURRENT_PIC){
            case 3:
                picLR=manualPic;
                break;
            case 2:
                picLR=combinedPic;
                break;
            case 1:
                picLR=mMemoryCache.get(photoKeyAligned);
                break;
            default:
                picLR=mMemoryCache.get(photoKey);
                break;
        }

        /*
        if(ALIGN_COUNTER==1){
            picLR = mMemoryCache.get(photoKeyAligned);
        }else if(TOUCH_ALIGN_COUNTER==1){
            picLR = combinedPic;
        }else if(MANUAL_ALIGN_COUNTER==1){
            picLR = manualPic;
        }else{
            picLR = mMemoryCache.get(photoKey);
        }*/

        try {
            file = getOutputMediaFile();
            OutputStream fOut = null;
            fOut = new FileOutputStream(file);
            picLR.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
            showToast("Saved!");
            //sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Not Saved");
        }
        picLR = null;
    }

    public void infoButton(View view){

        InfoLayout.setVisibility(View.VISIBLE);
        VerticalAlignMessage.setVisibility(View.VISIBLE);
        TouchAlignMessage.setVisibility(View.VISIBLE);
        ManualAlignMessage.setVisibility(View.VISIBLE);
        InfoMessage.setVisibility(View.VISIBLE);

        BackButtonMain.setEnabled(false);
        BackButton.setEnabled(false);
        InfoButton.setEnabled(false);
        AutoAlignButton.setEnabled(false);
        TouchAlignButton.setEnabled(false);
        ManualAlignButton.setEnabled(false);

        InfoLayout.setOnTouchListener(removeInfoLayout);
        VerticalAlignMessage.setOnTouchListener(removeInfoLayout);
        TouchAlignMessage.setOnTouchListener(removeInfoLayout);
        ManualAlignMessage.setOnTouchListener(removeInfoLayout);
        InfoMessage.setOnTouchListener(removeInfoLayout);

    }

    public void autoAlignButton(View view){

        if(TOUCH_ALIGN_COUNTER!=0){
            combinedPic=null;
            TouchAlignButton.setBackgroundResource(R.drawable.touch_align_5);
            TOUCH_ALIGN_COUNTER=0;
        }

        if(MANUAL_ALIGN_COUNTER!=0){
            shiftHor=1;
            shiftVer=1;
            //ManualAlignButton.setBackgroundResource(R.drawable.save_4);
            MANUAL_ALIGN_COUNTER=0;
        }


        if(ALIGN_COUNTER==0){
            if(mMemoryCache.get(photoKeyAligned)!=null){
                comPic.setImageBitmap(mMemoryCache.get(photoKeyAligned));
                CURRENT_PIC=1;
                ALIGN_COUNTER++;
                AutoAlignButton.setBackgroundResource(R.drawable.aligned_4);
            }else{
                showToast("Aligning...");
                alignedPic = AutoAlign2.alignAllCols(picL,picR);
                if(alignedPic!=null) {
                    showToast("Aligned");
                    comPic.setImageBitmap(alignedPic);
                    BitmapWorker.addAnswerToMemoryCache(photoKeyAligned, alignedPic);
                    alignedPic = null;
                    ALIGN_COUNTER++;
                    CURRENT_PIC=1;
                    Log.d("current pic 2", Integer.toString(CURRENT_PIC));
                    AutoAlignButton.setBackgroundResource(R.drawable.aligned_4);
                }else{
                    showToast("Alignment Failed");
                    ManualAlignDialog();
                }
            }


        }else{
            if(mMemoryCache.get(photoKey)!=null) {
                comPic.setImageBitmap(mMemoryCache.get(photoKey));
            }else{
                picLR = AutoAlign2.combinePhotos(picL,picR,0,0);
                comPic.setImageBitmap(picLR);
                BitmapWorker.addAnswerToMemoryCache(photoKey,picLR);
                picLR = null;
            }
            AutoAlignButton.setBackgroundResource(R.drawable.align_5);
            ALIGN_COUNTER=0;
            CURRENT_PIC=0;
        }
    }

    public void touchAlign(View view){



        if(MANUAL_ALIGN_COUNTER!=0){
            //
            //ManualAlignButton.setBackgroundResource(R.drawable.save_4);
            shiftHor=1;
            shiftVer=1;
            MANUAL_ALIGN_COUNTER=0;
        }

        if(TOUCH_ALIGN_COUNTER==0){
            AutoAlignButton.setVisibility(View.GONE);
            TouchAlignButton.setVisibility(View.GONE);
            SaveButton.setVisibility(View.GONE);
            ManualAlignButton.setVisibility(View.GONE);
            BackButtonMain.setVisibility(View.GONE);
            InfoButton.setVisibility(View.GONE);

            TouchInstructions.setVisibility(View.VISIBLE);
            BackButton.setVisibility(View.VISIBLE);



            //set compic to picR?
            comPic.setImageBitmap(picR);

            comPic.setOnTouchListener(handleTouch);

            //TOUCH_ALIGN_COUNTER++;
        }else{
            combinedPic=null;

            if(mMemoryCache.get(photoKey)!=null) {
                comPic.setImageBitmap(mMemoryCache.get(photoKey));
            }else{
                picLR = AutoAlign2.combinePhotos(picL,picR,0,0);
                comPic.setImageBitmap(picLR);
                BitmapWorker.addAnswerToMemoryCache(photoKey,picLR);
                picLR = null;
            }

            TouchAlignButton.setBackgroundResource(R.drawable.touch_align_5);
            TOUCH_ALIGN_COUNTER=0;
            CURRENT_PIC=0;
        }
    }



    private View.OnTouchListener handleTouch = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            comPic.setOnTouchListener(null);

            showToast("Aligning...");

            int xCorner = (int) event.getX();
            int yCorner = (int) event.getY();

            int buffer = (int) round((double)comPic.getMeasuredWidth()/4)+10;

            int screenHeight = comPic.getMeasuredHeight();
            int screenWidth = comPic.getMeasuredWidth();

            if(screenWidth - xCorner <buffer){
                xCorner = screenWidth - buffer;
            } else if( xCorner < buffer){
                xCorner = buffer;
            }

            if(screenHeight - yCorner <buffer){
                yCorner = screenHeight - buffer;
            } else if( yCorner < buffer){
                yCorner = buffer;
            }

            double xCentre = round(((double)comPic.getMeasuredWidth()/2)-xCorner);
            double yCentre = round(((double)comPic.getMeasuredHeight()/2)-yCorner);

            int xCentreScaled = (int)round((double)picL.getWidth()*xCentre/screenWidth);
            int yCentreScaled = (int)round((double)picL.getHeight()*yCentre/screenHeight);


            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i("TAG", "touched down");
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i("TAG", "moving: (" + xCentre + ", " + yCentre + ")");
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("TAG", "touched up");
                    break;
            }


            combinedPic = AutoAlign2.alignAllColsVer(picL,picR, xCentreScaled, yCentreScaled);

            if(combinedPic!=null){
                comPic.setImageBitmap(combinedPic);
                TouchAlignButton.setBackgroundResource(R.drawable.touch_aligned_5);
                showToast("Aligned");
                CURRENT_PIC=2;
                TOUCH_ALIGN_COUNTER++;
                if(ALIGN_COUNTER!=0){
                    AutoAlignButton.setBackgroundResource(R.drawable.align_5);
                    ALIGN_COUNTER=0;
                }
            }else{
                showToast("Alignment Failed");
                TOUCH_ALIGN_COUNTER=0;
                ManualAlignDialog();
                //comPic.setImageBitmap(mMemoryCache.get(photoKey));
            }

            Log.d("hello","hello");

            TouchInstructions.setVisibility(View.GONE);
            BackButton.setVisibility(View.GONE);

            SaveButton.setVisibility(View.VISIBLE);
            ManualAlignButton.setVisibility(View.VISIBLE);
            AutoAlignButton.setVisibility(View.VISIBLE);
            TouchAlignButton.setVisibility(View.VISIBLE);
            BackButtonMain.setVisibility(View.VISIBLE);
            InfoButton.setVisibility(View.VISIBLE);

            return true;

        }
    };

    private View.OnTouchListener removeInfoLayout = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            InfoMessage.playSoundEffect(SoundEffectConstants.CLICK);

            InfoLayout.setOnTouchListener(null);
            VerticalAlignMessage.setOnTouchListener(null);
            TouchAlignMessage.setOnTouchListener(null);
            ManualAlignMessage.setOnTouchListener(null);
            InfoMessage.setOnTouchListener(null);

            InfoLayout.setVisibility(View.GONE);
            VerticalAlignMessage.setVisibility(View.GONE);
            TouchAlignMessage.setVisibility(View.GONE);
            ManualAlignMessage.setVisibility(View.GONE);
            InfoMessage.setVisibility(View.GONE);

            BackButtonMain.setEnabled(true);
            BackButton.setEnabled(true);
            InfoButton.setEnabled(true);
            AutoAlignButton.setEnabled(true);
            TouchAlignButton.setEnabled(true);
            ManualAlignButton.setEnabled(true);

            return true;

        }
    };

    private Dialog ManualAlignDialog(){
        Log.d("hello","hello");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.alignment_failed_message)
                .setMessage(R.string.manual_align_message)
                .setPositiveButton(R.string.manual_align_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //TOUCH_ALIGN_COUNTER=0;
                        //ALIGN_COUNTER=0;
                        ManualAlign();
                    }
                })
                .setNegativeButton(R.string.manual_align_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        switch (CURRENT_PIC){
                            case 3:
                                Log.d("Hello",Integer.toString(3));
                                comPic.setImageBitmap(manualPic);
                                break;
                            case 2:
                                Log.d("Hello",Integer.toString(2));
                                comPic.setImageBitmap(combinedPic);
                                break;
                            case 1:
                                Log.d("Hello",Integer.toString(1));
                                comPic.setImageBitmap(mMemoryCache.get(photoKeyAligned));
                                break;
                            default:
                                Log.d("Hello",Integer.toString(0));
                                comPic.setImageBitmap(mMemoryCache.get(photoKey));
                                break;
                        }
                    }
                })
                .show();
        return builder.create();
    }

    private void ManualAlign(){
        //MANUAL_ALIGN_COUNTER=1;
        //combinedPic=null;
        Intent manualAlignIntent = new Intent(this, ManualAlign.class);
        //manualAlignIntent.putExtra(photoKeyLeft, fileLeft);
        //manualAlignIntent.putExtra(photoKeyRight,fileRight);
        //manualAlignIntent.putExtra("Orientation",OrientationOnRightPic);
        if (CURRENT_PIC==3){
            manualAlignIntent.putExtra("shiftHor",shiftHor);
            manualAlignIntent.putExtra("shiftVer",shiftVer);
        }
        startActivity(manualAlignIntent);
        //comPic.setImageBitmap(mMemoryCache.get(photoKey));
    }

    public void manualAlignButton(View view){
        ManualAlign();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("Pause","1");
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.d("Stop","1");

        switch (CURRENT_PIC){
            case 3:
                comPic.setImageBitmap(manualPic);
                break;
            case 2:
                comPic.setImageBitmap(combinedPic);
                break;
            case 1:
                comPic.setImageBitmap(mMemoryCache.get(photoKeyAligned));
                break;
            default:
                comPic.setImageBitmap(mMemoryCache.get(photoKey));
                break;
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // go back to main.
            Intent backIntent = new Intent(this, CameraActivity.class);
            startActivity(backIntent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }




    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "3D Camera");

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