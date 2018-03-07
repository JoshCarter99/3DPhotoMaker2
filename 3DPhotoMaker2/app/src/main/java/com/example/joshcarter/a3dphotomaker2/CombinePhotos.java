package com.example.joshcarter.a3dphotomaker2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static java.lang.Math.round;

/**
 * Created by JoshCarter on 21/02/2018.
 */

public class CombinePhotos extends AppCompatActivity{

    public Bitmap picL,picLC, picR,picRC,picLR;
    public Uri fileLeft, fileRight;
    public ImageView comPic;
    public File file;
    public double picWidth, picHeight, picRatio;
    LruCache<String, Bitmap> mMemoryCache;
    BitmapWorkerTask BitmapWorker;
    public byte[] bytesL, bytesR;

    public int newPicHeight, newPicWidth, currentOrientation;

    static int orientation;

    String photoKey = "photoBitmap";

    public File mFileL, mFileR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combine);

        currentOrientation = this.getResources().getConfiguration().orientation;
        Log.d("currentOrientation",Integer.toString(currentOrientation));

        comPic = (ImageView) findViewById(R.id.anaglyph);

        /////////
        RetainFragment retainFragment =
                RetainFragment.findOrCreateRetainFragment(getFragmentManager());
        mMemoryCache = retainFragment.mRetainedCache;


        if (mMemoryCache != null) {
            Log.d("Hello","hello");
            picLR=mMemoryCache.get(photoKey);
            //picLR = BitmapWorker.getAnswerFromMemoryCache(photoKey);
            Log.d("mMemoryCache2",picLR.toString());

            comPic.setImageBitmap(picLR);
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

            Log.d("Hello", "Bonjour");
            Intent intent = getIntent();

            //picL = intent.getParcelableExtra("photoLeft");
            //picR = intent.getParcelableExtra("photoRight");

            //bytesL = intent.getByteArrayExtra("photoLeft");
            //bytesR = intent.getByteArrayExtra("photoRight");

            fileLeft = intent.getParcelableExtra("photoLeft");
            //Log.d("Hello", fileLeft.toString());
            fileRight = intent.getParcelableExtra("photoRight");


            //picL = BitmapFactory.decodeByteArray(bytesL, 0, bytesL.length, null);
            //picR = BitmapFactory.decodeByteArray(bytesR, 0, bytesR.length, null);


            try {
                picL = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileLeft);
                Log.d("Hello", "Bonjour2");
                picR = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileRight);
            } catch (IOException e) {
                e.printStackTrace();
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





            picHeight = picL.getHeight();
            Log.d("Height",Double.toString(picHeight));
            picWidth = picL.getWidth();
            Log.d("Width",Double.toString(picWidth));

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

            Log.d("timeStarts","timer2");

            picLR = combinePhotos(picL, picR);
            Log.d("picLR1",picLR.toString());

            Log.d("timeStarts","timer3");

            Log.d("hello2","hey");



        }
        /////////




        Log.d("hello","hey");
        comPic.setImageBitmap(picLR);

        Log.d("orientation",Integer.toString(orientation));
        Log.d("currentOrientation",Integer.toString(currentOrientation));

        if ((orientation==6 && currentOrientation ==1) || (orientation == 1 && currentOrientation == 2)){
            comPic.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            comPic.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

        BitmapWorker = (BitmapWorkerTask) new BitmapWorkerTask().execute(10l);

        BitmapWorker.addMMemoryCache(mMemoryCache);

        BitmapWorker.addAnswerToMemoryCache(photoKey,picLR);
        Log.d("hello1","hey");



    }

    public Bitmap combinePhotos(Bitmap picL, Bitmap picR){

        int[] pixelColorL= new int[picL.getWidth()];
        int[] pixelColorR= new int[picR.getWidth()];
        picLC = picL.copy(picL.getConfig(),true);
        picRC = picR.copy(picR.getConfig(),true);

        Log.d("getPixelsLWidth",Integer.toString(picL.getWidth()));
        Log.d("getPixelsLHeight",Integer.toString(picL.getHeight()));
        Log.d("getPixelsRWidth",Integer.toString(picR.getWidth()));
        Log.d("getPixelsRHeight",Integer.toString(picR.getHeight()));
        for (int i=0; i<picL.getHeight();i++){
            picL.getPixels(pixelColorL,0,picL.getWidth(),0,i,picL.getWidth(),1);

            picR.getPixels(pixelColorR,0,picR.getWidth(),0,i,picR.getWidth(),1);

            for (int j=0; j<picL.getWidth();j++){
                pixelColorL[j] = pixelColorL[j] & 0xFFFF0000 | pixelColorR[j] & 0x0000FFFF ;
            }
            picLC.setPixels(pixelColorL,0,picL.getWidth(),0,i,picL.getWidth(),1);
        }

        return picLC;
    }

    public void backButton(View view){

        Log.d("Hello", "bonjourno");
        //Log.e("3DPhotoMaker2", fileLeft.toString());
        Intent backIntent = new Intent(CombinePhotos.this, CameraActivity.class);
        startActivity(backIntent);
    }

    public void saveButton(View view){
        try {
            file = getOutputMediaFile();
            OutputStream fOut = null;
            fOut = new FileOutputStream(file);
            picLR.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
            //MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

        } catch (Exception e) {
            e.printStackTrace();
        }

        // I dont know what this does...
        /*Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(file);
        mediaScanIntent.setData(uri);
        this.sendBroadcast(mediaScanIntent);*/

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

}
