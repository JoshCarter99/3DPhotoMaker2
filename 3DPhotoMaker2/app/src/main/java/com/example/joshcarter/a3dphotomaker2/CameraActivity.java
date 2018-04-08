package com.example.joshcarter.a3dphotomaker2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class CameraActivity extends AppCompatActivity {

    static final int PhotoPermission_Request_Code = 0;
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        checkPermissions();

        // Not sure if I need this... was used to disable button
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PhotoPermission_Request_Code);
        }

        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, CameraFragment.newInstance())
                    .commit();
        }
    }

    protected void onRestart(){
        super.onRestart();
        //getFragmentManager().beginTransaction().detach(this).attach(this).commit();
        Log.d("restart","Recreating");
        recreate();
    }

    //// no back navigation from main.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if(CameraFragment.PIC_COUNTER==1){
                Log.d("Hello","1");
                recreate();
            }else {
                Log.d("Hello","2");
                //finish();
                //System.exit(0);
                //moveTaskToBack(true);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        Log.d("Check","HelloWOW2");
        final String[] requiredPermissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
        };
        final List<String> neededPermissions = new ArrayList<>();
        for (final String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }
        if (!neededPermissions.isEmpty()) {
            requestPermissions(neededPermissions.toArray(new String[]{}),
                    MY_PERMISSIONS_REQUEST_ACCESS_CODE);
        }
    }





}
