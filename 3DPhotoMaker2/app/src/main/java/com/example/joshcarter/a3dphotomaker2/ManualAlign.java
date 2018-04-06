package com.example.joshcarter.a3dphotomaker2;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.IOException;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * Created by JoshCarter on 24/03/2018.
 */

public class ManualAlign extends AppCompatActivity{

    public Bitmap picLC, picRC;
    String photoKeyLeft = "photoLeft";
    String photoKeyRight = "photoRight";
    static int orientation;

    public int shiftHor = 0;
    public int shiftVer = 0;

    public ImageView comPicLeft, comPicRight;

    public ViewGroup.LayoutParams params;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_align);

        comPicLeft = findViewById(R.id.anaglyphLeft);
        comPicRight = findViewById(R.id.anaglyphRight);

        //comPicRight.setImageBitmap(CombinePhotos2.picR);

        picLC = CombinePhotos2.picL.copy(CombinePhotos2.picL.getConfig(),true);
        picRC = CombinePhotos2.picR.copy(CombinePhotos2.picR.getConfig(),true);
        int[] pixelColorL= new int[picLC.getWidth()];
        int[] pixelColorR= new int[picRC.getWidth()];

        for (int i = 0; i < picLC.getHeight(); i++) {
            picLC.getPixels(pixelColorL, 0, picLC.getWidth(), 0, i, picLC.getWidth(), 1);
            picRC.getPixels(pixelColorR, 0, picRC.getWidth(), 0, i, picRC.getWidth(), 1);

            for (int j = 0; j < picLC.getWidth(); j++) {
                pixelColorL[j] = (pixelColorL[j] & 0x00FF0000);
                pixelColorR[j] = (pixelColorR[j] & 0xFF00FFFF);
            }
            picLC.setPixels(pixelColorL, 0, picLC.getWidth(), 0, i, picLC.getWidth(), 1);
            picRC.setPixels(pixelColorR, 0, picRC.getWidth(), 0, i, picRC.getWidth(), 1);
        }

        comPicRight.setImageBitmap(picRC);
        comPicLeft.setImageBitmap(picLC);
        comPicLeft.setImageAlpha(190);

        /*comPicLeft.setLayoutParams(comPicLeft.getHeight(),comPicLeft.getWidth());
        comPicLeft.getHeight();
        comPicLeft.getWidth();*/

        params = comPicLeft.getLayoutParams();
        Log.d("height",Integer.toString(params.height));
        Log.d("width",Integer.toString(params.width));
        params.height = comPicLeft.getHeight();
        params.width = comPicLeft.getWidth();
        comPicLeft.setLayoutParams(params);

        if(getIntent().getIntExtra("shiftHor",1)!=1){
            shiftHor=getIntent().getIntExtra("shiftHor",1);
            shiftVer=getIntent().getIntExtra("shiftVer",1);
            comPicLeft.setX(comPicLeft.getX()-shiftHor);
            comPicLeft.setY(comPicLeft.getY()-shiftVer);
        }

        /*if (CombinePhotos2.orientation == 1) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }*/
    }

    public void leftButton(View view){
        comPicLeft.setX(comPicLeft.getX()-20);
        shiftHor+= 20;
    }

    public void leftButtonSmall(View view){
        comPicLeft.setX(comPicLeft.getX()-5);
        shiftHor+= 5;
    }

    public void rightButton(View view){
        comPicLeft.setX(comPicLeft.getX()+20);
        shiftHor-= 20;
    }

    public void rightButtonSmall(View view){
        comPicLeft.setX(comPicLeft.getX()+5);
        shiftHor-= 5;
    }

    public void downButton(View view){
        comPicLeft.setY(comPicLeft.getY()+20);
        shiftVer-= 20;
    }

    public void downButtonSmall(View view){
        comPicLeft.setY(comPicLeft.getY()+5);
        shiftVer-= 5;
    }

    public void upButton(View view){
        comPicLeft.setY(comPicLeft.getY()-20);
        shiftVer+= 20;
    }

    public void upButtonSmall(View view){
        comPicLeft.setY(comPicLeft.getY()-5);
        shiftVer+= 5;
    }

    public void OkButton(View view){
        Intent manualAlignBackIntent = new Intent(this, CombinePhotos2.class);
        manualAlignBackIntent.putExtra("shiftHor",shiftHor);
        manualAlignBackIntent.putExtra("shiftVer",shiftVer);
        //manualAlignBackIntent.putExtra("Orientation",CombinePhotos2.orientation);
        startActivity(manualAlignBackIntent);
    }

    /*public void backButton(View view){
        Intent backIntent = new Intent(this, CombinePhotos2.class);
        startActivity(backIntent);
    }*/
}
