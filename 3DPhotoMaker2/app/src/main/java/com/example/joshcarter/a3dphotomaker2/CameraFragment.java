package com.example.joshcarter.a3dphotomaker2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.round;

/**
 * Created by JoshCarter on 20/02/2018.
 */

public class CameraFragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    private int PIC_COUNTER = 0;
    private Uri fileLeft;
    private Uri fileRight;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final String TAG = "Camera2BasicFragment";

    // Camera state: Showing camera preview.
    private static final int STATE_PREVIEW = 0;
    //Camera state: Waiting for the focus to be locked.
    private static final int STATE_WAITING_LOCK = 1;
    //Camera state: Waiting for the exposure to be precapture state.
    private static final int STATE_WAITING_PRECAPTURE = 2;
    //Camera state: Waiting for the exposure state to be something other than precapture.
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    //Camera state: Picture was taken.
    private static final int STATE_PICTURE_TAKEN = 4;
    //Max preview width that is guaranteed by Camera2 API
    private static final int MAX_PREVIEW_WIDTH = 1920;
    //Max preview height that is guaranteed by Camera2 API
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    public int i=1;


    //{@link TextureView.SurfaceTextureListener} handles several lifecycle events on a {@link TextureView}.
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
            //Log.d("catWidth",texture.toString());
            //Log.d("catWidth",Integer.toString(width));
            //Log.d("catHeight",Integer.toString(height));
            //Log.d("catTester","3.1");
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                configureTransform(width, height);
            //Log.d("catTester","3.2");
            //Log.d("cat3.2height",Integer.toString(height));
            //Log.d("cat3.2Width",Integer.toString(width));
            //Log.d("catWidth",texture.toString());
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            //Log.d("catWidth",texture.toString());
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
            //Log.d("Tester","3.3");
        }

    };

    //ID of the current {@link CameraDevice}
    private String mCameraId;
    //{@link AutoFitTextureView} for camera preview.
    private AutoFitView mTextureView;
    //{@link CameraCaptureSession } for camera preview.
    private CameraCaptureSession mCaptureSession;
    //A reference to the opened {@link CameraDevice}.
    private CameraDevice mCameraDevice;
    //The {@link android.util.Size} of camera preview.
    private Size mPreviewSize;

    //{@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is
            // opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
            //Log.d("camera",cameraDevice.toString());
            //Log.d("Tester","4.1");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            //Log.d("camera",cameraDevice.toString());
            //Log.d("Tester","4.2");
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
            //Log.d("camera",cameraDevice.toString());
            //Log.d("Tester","4.3");
        }

    };

    //An additional thread for running tasks that shouldn't block the UI.
    private HandlerThread mBackgroundThread;
    //{@link Handler} for running tasks in the background.
    private Handler mBackgroundHandler;
    //{@link ImageReader} that handles still image capture.
    private ImageReader mImageReader;
    //This is the output file for our picture.
    private File mFileL,mFileR;

    //This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
    //still image is ready to be saved.
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {

            //Log.d("reader",reader.toString());
            //Log.d("Counter", Integer.toString(PIC_COUNTER));
            if(PIC_COUNTER==0) {
                //Log.d("FileOutputLeft",mFileL.toString());
                mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFileL));
            }else if(PIC_COUNTER==1){
                //Log.d("FileOutputRight",mFileR.toString());
                mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFileR));
            }
            //Log.d("Tester","5");
        }

    };

    //{@link CaptureRequest.Builder} for the camera preview
    private CaptureRequest.Builder mPreviewRequestBuilder;
    //{@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
    private CaptureRequest mPreviewRequest;
    //The current state of camera state for taking pictures.
    private int mState = STATE_PREVIEW;
    //A {@link Semaphore} to prevent the app from exiting before closing the camera.
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    //Whether the current camera device supports Flash or not.
    private boolean mFlashSupported;
    //Orientation of the camera sensor
    private int mSensorOrientation;

    //A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    //Log.d("Break","break");
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    //Log.d("test","1");
                    if (afState == null) {
                        captureStillPicture();
                      //  Log.d("test","1.1");
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        //Log.d("test","1.2");

                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                          //  Log.d("test","1.2.1");
                        } else {
                            //Log.d("test","1.2.2");
                            runPrecaptureSequence();
                        }
                        //Log.d("Tester","6");
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    //Log.d("test","2");
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                      //  Log.d("Tester","6");
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    //Log.d("test","3");
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                      //  Log.d("Tester","6");
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            //Log.d("test","4");
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            //Log.d("test","5");
            process(result);
        }
    };

    // Shows a {@link Toast}
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
            //Log.d("Tester", "7");
        }
    }

    //choose the smallest one that is at least as large as the respective texture view size, and
    // that is at most as large as the respective max size, and whose aspect ratio matches with
    // the specified value. If such size doesn't exist, choose the largest one that is at most as
    // large as the respective max size, and whose aspect ratio matches with the specified value.
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        //Log.d("aspectRatio",aspectRatio.toString());
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        //Log.d("aspectRationHeight",Integer.toString(h));
        //Log.d("aspectRationWidth",Integer.toString(w));

        //Log.d("choices",choices.toString());
        for (Size option : choices) {
            //Log.d("Option",option.toString());
            //Log.d("optionWidth",Integer.toString(option.getWidth()));
            //Log.d("optionHeight",Integer.toString(option.getHeight()));
            //Log.d("opW*h/w",Integer.toString(round(option.getWidth() * h / w)));


            //Log.d("maxWidth",Integer.toString(maxWidth));
            //Log.d("maxHeight",Integer.toString(maxHeight));
            //Log.d("textureViewHeight",Integer.toString(textureViewHeight));
            //Log.d("textureViewWidth",Integer.toString(textureViewWidth));

            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == round(option.getWidth() * h / w)) {
                //Log.d("test","6");
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    //Log.d("test","6.1");
                    bigEnough.add(option);
                } else {
                    //Log.d("test","6.2");
                    notBigEnough.add(option);
                }
            }
            //Log.d("Tester", "8");
        }

        //Log.d("bigEnough",Integer.toString(bigEnough.size()));
        //Log.d("notBigEnough",Integer.toString(bigEnough.size()));

        //Log.d("bigEnough",bigEnough.toString());
        //Log.d("notbigEnough",notBigEnough.toString());

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            //Log.d("bigHello","What's up");
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            //Log.d("bigHello","What's up2");
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            //Log.e(TAG, "Couldn't find any suitable preview size");
            //Log.d("bigchoices0",choices[0].toString());
            return choices[0];


        }

    }

    public static CameraFragment newInstance() {
        //Log.d("test","7");
        return new CameraFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Log.d("Tester","9");
        //Log.d("container",container.toString());
        //Log.d("test",inflater.toString());
        if(savedInstanceState!=null) {
            //Log.d("onSavedINstance", savedInstanceState.toString());
        }
        return inflater.inflate(R.layout.fragment_layout, container, false);

    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        view.findViewById(R.id.picture).setOnClickListener(this);
        view.findViewById(R.id.info).setOnClickListener(this);
        view.findViewById(R.id.restart).setOnClickListener(this);
        mTextureView = (AutoFitView) view.findViewById(R.id.texture);
        //Log.d("Tester","10");
        if(savedInstanceState!=null) {
            //Log.d("onSavedInstance2", savedInstanceState.toString());
        }
    }


//////////


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //mFile = new File(getActivity().getExternalFilesDir(null), "pic.jpg");

        if(savedInstanceState!=null) {
            //Log.d("SavedInstanceState3", savedInstanceState.toString());
        }

        mFileL = getOutputMediaFile("Left");
        mFileR = getOutputMediaFile("Right");

        //fileLeft = Uri.fromFile(mFile);

        //Log.d("tester",mFileL.toString());
        //Log.d("tester2",fileLeft.toString());
        //Log.d("Tester","11");
    }

    private static File getOutputMediaFile(String LorR){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "3DPhotoMaker2");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                //Log.d("3DPhotoMaker2", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        if(LorR=="Left") {
            return new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        }else{
            return new File(mediaStorageDir.getPath() + File.separator +
                    "IMG2_" + timeStamp + ".jpg");
        }
    }

    ////////////

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            //Log.d("mTextureViewWidth",Integer.toString(mTextureView.getWidth()));
            //Log.d("mTextureViewHeight",Integer.toString(mTextureView.getHeight()));
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            //Log.d("Tester","12.1");
            //openCamera(1920,1080);
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            //Log.d("Tester","12.2");
        }
        //Log.d("Tester","12");
        i = 2;
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
        //Log.d("Tester","13");
    }

    @Override
    public void onStop(){
        super.onStop();
        //Log.d("Tester","13.2");
    }

    public void onDestroy(){
        super.onDestroy();
        //Log.d("Tester","13.3");
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
            //Log.d("Test","8");
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            //Log.d("Test","8.2");
        }
        //Log.d("Tester","14");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        //Log.d("test",  Integer.toString(requestCode));
        //Log.d("test",  permissions.toString());
        //Log.d("test",  grantResults.toString());
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            //Log.d("test",  "9");
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                //Log.d("test",  "9.2");
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            //Log.d("test",  "9.3");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        //Log.d("Tester","15");
    }

    //Sets up member variables related to camera.
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        //Log.d("test.",  Integer.toString(width));
        //Log.d("test",Integer.toString(height));
        Activity activity = getActivity();
        //Log.d("activity",activity.toString());
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            //Log.d("test","10");
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                //Log.d("test","10.2");
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    //Log.d("test","10.1");
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //Log.d("map",map.toString());
                if (map == null) {
                    //Log.d("test","10.3");
                    continue;
                }

                // For still image captures, we use the largest available size.
                // Why is maxImages = 2?
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                        //Log.d("largestWidth",Integer.toString(largest.getWidth()));
                        //Log.d("largestHeight",Integer.toString(largest.getHeight()));
                //Log.d("test",mImageReader.toString());
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();

                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

                // swapped dimensions is true if vertical, false if not.
                boolean swappedDimensions = false;
                //Log.d("swappedRotationCheck",Integer.toString(displayRotation));
                //Log.d("swappedsufRot0",Integer.toString(Surface.ROTATION_0));
                //Log.d("swappedsufRot180",Integer.toString(Surface.ROTATION_180));
                //Log.d("swappedsufRot90",Integer.toString(Surface.ROTATION_90));
                //Log.d("swappedsufRot270",Integer.toString(Surface.ROTATION_270));
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                        //Log.d("swapdisplayRot0",Integer.toString(displayRotation));
                        //Log.d("swapsufRot2,0",Integer.toString(Surface.ROTATION_0));
                        //Log.d("swappedDimensions","false0");
                        // Initial?
                        //break; //
                    case Surface.ROTATION_180:
                        //Log.d("swapdisplayRot180",Integer.toString(displayRotation));
                        //Log.d("swapsufRot2,180",Integer.toString(Surface.ROTATION_180));
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                            //Log.d("swappedDimensions","true, 90||270");
                            // verticle
                        }
                        //Log.d("swappedDimensions","false_180");
                        break;
                    case Surface.ROTATION_90:
                        //Log.d("swapdisplayRot90",Integer.toString(displayRotation));
                        //Log.d("swapsufRot2,90",Integer.toString(Surface.ROTATION_90));
                        //Log.d("swappedDimensions","false90");
                        // rotated left
                        //break; //
                    case Surface.ROTATION_270:
                        //Log.d("swapdisplayRot270",Integer.toString(displayRotation));
                        //Log.d("swapsufRot2,270",Integer.toString(Surface.ROTATION_270));
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                            //Log.d("swappedDimensions","true, 0||180");
                        }
                        //Log.d("swappedDimensions","false, 270");
                        break;
                    default:
                        //Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                        //Log.d("swappedDimensions","none");
                }

                //Log.d("swappedDimensions",Boolean.toString(swappedDimensions));


                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);

                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;
                //Log.d("Height",Integer.toString(maxPreviewHeight));
                //Log.d("Width",Integer.toString(maxPreviewWidth));
                //Log.d("displaySize",displaySize.toString());

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                    //Log.d("swaprotatedPreviewWidth",Integer.toString(rotatedPreviewWidth));
                    //Log.d("swaprotatedPreviewHeight",Integer.toString(rotatedPreviewHeight));
                    //Log.d("swapmaxPreviewWidth",Integer.toString(maxPreviewWidth));
                    //Log.d("swapmaxPreviewHeight",Integer.toString(maxPreviewHeight));
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                    //Log.d("test","10");
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                    //Log.d("test","11");
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                //Log.d("rotatedPreviewWidth",Integer.toString(rotatedPreviewWidth));
                //Log.d("rotatedPreviewHeight",Integer.toString(rotatedPreviewHeight));
                //Log.d("maxPreviewWidth",Integer.toString(maxPreviewWidth));
                //Log.d("maxPreviewHeight",Integer.toString(maxPreviewHeight));
                //Log.d("largest",largest.toString());

                //Log.d("mpreviewSize",mPreviewSize.toString());

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                //Log.d("orientation",Integer.toString(orientation));
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                            //Log.d("mPreviewSizeWidthL",Integer.toString(mPreviewSize.getWidth()));
                            //Log.d("mPreviewSizeHeightL",Integer.toString(mPreviewSize.getHeight()));
                } else {
                    mTextureView.setAspectRatio(

                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                    //Log.d("mPreviewSizeWidthV",Integer.toString(mPreviewSize.getWidth()));
                    //Log.d("mPreviewSizeHeightV",Integer.toString(mPreviewSize.getHeight()));
                }

                //View view_instance = (View)findViewById(R.id.nutrition_bar_filled);
                //LayoutParams params=view_instance.getLayoutParams();
                //params.width=newOne;
                //view_instance.setLayoutParams(params);

                //ViewGroup.LayoutParams params=mTextureView.getLayoutParams();
                //params.width=30;
                //mTextureView.setLayoutParams(params);

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                //Log.d("test",cameraId.toString());
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            //Log.d("test","12");
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            //Log.d("test","13");
        }
        //Log.d("Tester","16");
    }

    //Opens the camera specified by {@link Camera2BasicFragment#mCameraId}.
    private void openCamera(int width, int height) {
        //Log.d("test14",Integer.toString(width));
        //Log.d("test15",Integer.toString(height));
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            //Log.d("catWow","wow0");
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        //Log.d("test16",activity.toString());
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
            //Log.d("catWow","wow");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
        //Log.d("Tester","17");
    }

    //Closes the current {@link CameraDevice}.
    private void closeCamera() {
        //Log.d("test","17");
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
                //Log.d("Tester","18.1");
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
                //Log.d("Tester","18.2");
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
                //Log.d("Tester","18.3");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
            //Log.d("test","18");
        }
       // SendImages();
        //Log.d("Tester","18");
    }

    //Starts a background thread and its {@link Handler}.
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        //Log.d("Tester","19");
    }

    //Stops the background thread and its {@link Handler}.
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Log.d("Tester","20");
    }

    //Creates a new {@link CameraCaptureSession} for camera preview.
    private void createCameraPreviewSession() {
        //Log.d("mPreviewWidth",Integer.toString(mPreviewSize.getWidth()));
        //Log.d("mPreviewHeight",Integer.toString(mPreviewSize.getHeight()));
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            //int Width = (int)round(MAX_PREVIEW_HEIGHT*(((double)mPreviewSize.getWidth())/((double)mPreviewSize.getHeight())));
            //int Height = (int)round(MAX_PREVIEW_WIDTH*(((double)mPreviewSize.getHeight())/((double)mPreviewSize.getWidth())));

            // We configure the size of default buffer to be the size of camera preview we want.
            //Log.d("orientation",Integer.toString(this.getResources().getConfiguration().orientation));
            //Log.d("lolTextureWidth",Integer.toString(mTextureView.getWidth()));
            //Log.d("lolTextureHeight",Integer.toString(mTextureView.getHeight()));
            //Log.d("lolPreviewWidth",Integer.toString(mPreviewSize.getWidth()));
            //Log.d("lolPreviewHeight",Integer.toString(mPreviewSize.getHeight()));
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            //texture.setDefaultBufferSize(Width, MAX_PREVIEW_HEIGHT);
            //texture.setDefaultBufferSize(MAX_PREVIEW_WIDTH, Height);

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);
            //Log.d("test19",surface.toString());

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mPreviewRequestBuilder.addTarget(surface);
            //Log.d("test20",mPreviewRequestBuilder.toString());

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                //Log.d("test","21");
                                return;

                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                //Log.d("test","22");
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                //Log.d("test23",mPreviewRequest.toString());
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                                //Log.d("test24",mCaptureSession.toString());
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //Log.d("Tester","21");
    }

    //Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
    //This method should be called after the camera preview size is determined in
    //setUpCameraOutputs and also the size of `mTextureView` is fixed.
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        //Log.d("test25",Integer.toString(viewHeight));
        //Log.d("test26",Integer.toString(viewWidth));
        //Log.d("test27",activity.toString());
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            //Log.d("test","27");
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        //Log.d("test28",Integer.toString(rotation));
        Matrix matrix = new Matrix();
        //Log.d("test29",matrix.toShortString());
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        //Log.d("test30",viewRect.toShortString());
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        //Log.d("test31",bufferRect.toShortString());

        //RectF viewRectTest = new RectF(0, 0, viewHeight, viewWidth);
        //RectF bufferRectTest = new RectF(0, 0, mPreviewSize.getWidth(), mPreviewSize.getHeight());

        float centerX = viewRect.centerX();
        //Log.d("test32",Float.toString(centerX));

        float centerY = viewRect.centerY();
        //Log.d("test33",Float.toString(centerY));
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            //Log.d("test34",Float.toString(scale));
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            //Log.d("test35",matrix.toShortString());
            //Log.d("Tester","22.2 - rot = 90 || 270");
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
            //Log.d("Tester","22.1 - rot = 180");
            //Log.d("test36",matrix.toShortString());
        }
        mTextureView.setTransform(matrix);
        //Log.d("Tester","22");
    }

    //Initiate a still image capture.
    private void takePicture() {
        lockFocus();
        //Log.d("Tester","23");
    }

    //Lock the focus as the first step for a still image capture.
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            //Log.d("wow1",mPreviewRequestBuilder.build().toString());

            //Log.d("wow2",mCaptureCallback.toString());
            //Log.d("wow3",mBackgroundHandler.toString());
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //Log.d("Tester","24");
    }

    //Run the precapture sequence for capturing a still image. This method should be called when
    //we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //Log.d("Tester","25");
    }

    //Capture a still picture. This method should be called when we get a response in
    //{@link #mCaptureCallback} from both {@link #lockFocus()}.
    private void captureStillPicture() {
        //Log.d("test","37");
        try {
            final Activity activity = getActivity();
            //Log.d("test39",activity.toString());
            if (null == activity || null == mCameraDevice) {
                //Log.d("test","40");
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());



            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);


            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));
            //Log.d("test42",Integer.toString(rotation));
            //Log.d("test41",captureBuilder.toString());

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    //showToast("Saved: " + mFile);
                    showToast("Saved!");
                    Log.d(TAG, mFileL.toString());
                    unlockFocus();
                    PIC_COUNTER++;
                    if(PIC_COUNTER==2){
                        PIC_COUNTER=0;
                        SendImages();
                    }
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);

            //Log.d("test43",mCaptureSession.toString());


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //Log.d("Tester","26");
    }

    //Retrieves the JPEG orientation from the specified screen rotation.
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        //Log.d("test44",Integer.toString(rotation));
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    //Unlock the focus. This method should be called when still image capture sequence is
    //finished.
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            //Log.d("test","45");
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //Log.d("Tester","27");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.picture: {
                takePicture();
                break;
            }
            case R.id.info: {
                Activity activity = getActivity();
                if (null != activity) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.info_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                break;
            }
            case R.id.restart: {

                getActivity().recreate();
            }
        }
        //Log.d("Tester","28");

    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        /*if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        } */
        //Log.d("Tester","29");
    }

    //Saves a JPEG {@link Image} into the specified {@link File}.
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
            //Log.d("Tester","30");
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            //Log.d("test46",buffer.toString());
            byte[] bytes = new byte[buffer.remaining()];
            //Log.d("test47",bytes.toString());
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                //Log.d("Wow","Before");
                //Log.d("Wow",mFile.toString());
                output = new FileOutputStream(mFile);
                //Log.d("Wow","After");
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //Log.d("Tester","30");
        }
    }

    //Compares two {@code Size}s based on their areas.
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            //Log.d("Tester",Long.toString(Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    //(long) rhs.getWidth() * rhs.getHeight())));
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    //Shows an error message dialog.
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            //Log.d("Tester","32");
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                            //Log.d("Tester","32");
                        }
                    })
                    .create();

        }
    }

    //Shows OK/Cancel confirmation dialog about camera permission.
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                        //Log.d("Tester","33");
                                    }
                                }
                            })
                    .create();
        }

    }

    public void SendImages(){
        //Log.d("3DPhotoMaker","hello3");
        fileLeft = Uri.fromFile(mFileL);
        fileRight = Uri.fromFile(mFileR);
        Intent sendPhotoIntent = new Intent(getActivity(), CombinePhotos.class);
        sendPhotoIntent.putExtra("photoLeft",fileLeft);
        sendPhotoIntent.putExtra("photoRight",fileRight);
        //Log.d("3DPhotoMaker","hello2");
        startActivity(sendPhotoIntent);
    }


}

