/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.znx.funtool;
import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.cameraview.AspectRatio;
import com.google.android.cameraview.CameraView;

import com.huawei.hiai.vision.image.detector.AestheticsScoreDetector;
import com.huawei.hiai.vision.visionkit.common.Frame;
import com.huawei.hiai.vision.visionkit.image.detector.AEModelConfiguration;
import com.huawei.hiai.vision.visionkit.image.detector.AestheticsScore;
import com.huawei.hiai.vision.visionkit.image.detector.aestheticsmodel.AEConstants;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import static java.lang.Thread.sleep;


/**
 * This demo app saves the taken picture to a constant file.
 * $ adb pull /sdcard/Android/data/com.google.android.cameraview.demo/files/Pictures/picture.jpg
 */
public class DynamicScoreActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        AspectRatioFragment.Listener {

    private static final String TAG = "DynamicScoreActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private static final String FRAGMENT_DIALOG = "dialog";

    private File photoFile;

    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON,
    };

    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };

    private static final int[] FLASH_TITLES = {
            R.string.flash_auto,
            R.string.flash_off,
            R.string.flash_on,
    };

    private int mCurrentFlash;

    private CameraView mCameraView;

    private Handler mBackgroundHandler;

    private TextView analyseResult;

    //预览数据获取
    private ImageReader mImageReader;
    private Size selectPreviewSize;
    private CameraDevice mCameraDevice;


    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.take_picture:
                    if (mCameraView != null) {
                        mCameraView.takePicture();
                    }
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dynamic_score);
        mCameraView = findViewById(R.id.camera);
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }
        FloatingActionButton fab = findViewById(R.id.take_picture);
        if (fab != null) {
            fab.setOnClickListener(mOnClickListener);
        }
        analyseResult = (TextView)findViewById(R.id.analyse_result);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        //preview
//        mImageReader = ImageReader.newInstance(selectPreviewSize.getWidth(), selectPreviewSize.getHeight(),
//                ImageFormat.YUV_420_888, /*maxImages*/5);
//        mImageReader.setOnImageAvailableListener(   // 设置监听和后台线程处理器
//                mOnImageAvailableListener, mBackgroundHandler);
//        mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//        mPreviewRequestBuilder.addTarget(surface);  //请求捕获的目标surface
//        mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
    }

    //preview
    private final static int EXECUTION_FREQUENCY = 10;
    private int PREVIEW_RETURN_IMAGE_COUNT;

//    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
//            = new ImageReader.OnImageAvailableListener() {
//        @Override
//        public void onImageAvailable(ImageReader reader) {
//            // 小技巧：降低物体识别的频率
//            // 设置识别的频率，当EXECUTION_FREQUENCY为5时，也就是此处被回调五次只识别一次
//            // 假若帧率被我设置在15帧/s，那么就是 1s 识别 3次，若是30帧/s，那就是1s识别6次，以此类推
//            PREVIEW_RETURN_IMAGE_COUNT++;
//            if(PREVIEW_RETURN_IMAGE_COUNT % EXECUTION_FREQUENCY !=0) return;
//            PREVIEW_RETURN_IMAGE_COUNT = 0;
//
//            final Image image = reader.acquireLatestImage();   // 获取最近一帧图像
//            mBackgroundHandler.post(new Runnable() {    // 在子线程执行，防止预览界面卡顿
//                @Override
//                public void run() {
//                    Mat mat = Yuv.rgb(image);  // 从YUV_420_888 到 Mat(RGB)，这里使用了第三方库，build.gradle中可见
//
//                    Mat input_mat = new Mat();
//                    Imgproc.cvtColor(mat,input_mat, Imgproc.COLOR_RGB2BGR); // 转换格式
//
//                    //  BvaNative.detect函数是物体识别函数，一个物体为一组数据，都在返回值里
//                    final float[] result = BvaNative.detect(input_mat.getNativeObjAddr(),true,mRotateDegree);  // 识别
//                    if(result == null){
//                        Log.d(TAG, "detector: result is null!");
//                    }else {
//                        float[][] get_finalResult = TwoArray(result);   //变为二维数组
//                        show_detect_results(get_finalResult);   // 在UI线程中画框
//                    }
//                    image.close();   // 这里一定要close，不然预览会卡死
//                }
//            });
//        }
//
//    };

    private void initPrediction() {
        analyseResult.setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mCameraView.start();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ConfirmationDialogFragment
                    .newInstance(R.string.camera_permission_confirmation,
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION,
                            R.string.camera_permission_not_granted)
                    .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    //更新图库
    private static void updatePhotoMedia(File file ,Context context){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        context.sendBroadcast(intent);
    }


    @Override
    protected void onPause() {
        mCameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != 1 || grantResults.length != 1) {
                    throw new RuntimeException("Error on requesting camera permission.");
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.camera_permission_not_granted,
                            Toast.LENGTH_SHORT).show();
                }
                // No need to start camera here; it is handled by onResume
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dynamicscore, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.aspect_ratio:
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (mCameraView != null
                        && fragmentManager.findFragmentByTag(FRAGMENT_DIALOG) == null) {
                    final Set<AspectRatio> ratios = mCameraView.getSupportedAspectRatios();
                    final AspectRatio currentRatio = mCameraView.getAspectRatio();
                    AspectRatioFragment.newInstance(ratios, currentRatio)
                            .show(fragmentManager, FRAGMENT_DIALOG);
                }
                return true;
            case R.id.switch_flash:
                if (mCameraView != null) {
                    mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;
                    item.setTitle(FLASH_TITLES[mCurrentFlash]);
                    item.setIcon(FLASH_ICONS[mCurrentFlash]);
                    mCameraView.setFlash(FLASH_OPTIONS[mCurrentFlash]);
                }
                return true;
            case R.id.switch_camera:
                if (mCameraView != null) {
                    int facing = mCameraView.getFacing();
                    mCameraView.setFacing(facing == CameraView.FACING_FRONT ?
                            CameraView.FACING_BACK : CameraView.FACING_FRONT);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAspectRatioSelected(@NonNull AspectRatio ratio) {
        if (mCameraView != null) {
            Toast.makeText(this, ratio.toString(), Toast.LENGTH_SHORT).show();
            mCameraView.setAspectRatio(ratio);
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    //创建文件
    private File createFileIfNeed(String fileName)throws IOException{
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/Pictures";
            File fileJA = new File(path);
            if(!fileJA.exists()) {
                fileJA.mkdirs();
            }
            File file = new File(path, fileName);
            if(!file.exists()){
                file.createNewFile();
            }
            return file;
    }

    private Bitmap createBitmap(View view) {
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        return bitmap;
    }

    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(final CameraView cameraView) {

            Log.d(TAG, "onCameraOpened");
//            getBackgroundHandler().post(new Runnable() {
//                @Override
//                public void run() {
//                    analyseResult.setText("预览中");
////                    try{
////                        sleep(4000);
////                    }catch (InterruptedException e){
////                        e.printStackTrace();
////                    }
//                }
//
//            });

//
//            while(true){
//                View view =  cameraView.getView();
//                Bitmap currentBitmap = createBitmap(view);
//                ASTask cnnTask = new ASTask();
//                cnnTask.execute(currentBitmap);
//            }

        }

//        //预览数据获取
//        class ScanView extends SurfaceView implements
//                Camera.AutoFocusCallback {
//            private Camera mCamera;
//            private final File fileImg;
//
//            private ScanView self;
//
//            public ScanView(Context context) {
//                super(context);
//                fileImg = new File(context.getCacheDir(), "prev_view.jpg");
//                SurfaceHolder mHolder = getHolder();
//                self = this;
//                //mHolder.addCallback(this);
//                // deprecated setting, but required on Android versions prior to 3.0
//                mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//            }
//
//            @Override
//            public void onAutoFocus(boolean success, Camera camera) {
//                if (mCamera != null) {
//                    mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
//                        @Override
//                        public void onPictureTaken(byte[] data, Camera camera) {
//
//                            mCamera.cancelAutoFocus();
//                            mCamera.stopPreview(); // 拿到数据就停止！！！
////                        camera.startPreview();
//                            File pictureFile = fileImg;
//                            if (pictureFile == null) {
//                                Log.d(TAG,"picture is null");
//                                return;
//                            }
//
//                            if (data == null) {
//                                return;
//                            }
//                            try {
//                                FileOutputStream fos = new FileOutputStream(pictureFile);
//                                fos.write(data);
//                                fos.close();
//                                LogUtils.e("save preview complete###!!!");
//                                LogUtils.e("save preview complete###!!!" + pictureFile);
//                                BitmapFactory.Options options = new BitmapFactory.Options();
//                                options.inJustDecodeBounds = true;
//                                BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), options);
//                                options.inJustDecodeBounds = false;
//                                int outWidth = options.outWidth;
//                                int outHeight = options.outHeight;
//                                if (outWidth >= getWidth() * 2) {
//                                    options.inSampleSize = outWidth / getWidth();
//                                }
//                                if (outHeight >= getHeight() * 2) {
//                                    options.inSampleSize = outHeight / getHeight();
//                                }
//                                Bitmap bmp = BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), options);
//                                Result result = parseInfoFromBitmap(bmp);
//                                if (result != null) {
//                                    Toast.makeText(getContext(), "INFO:" + result.getText(), Toast.LENGTH_SHORT).show();
//                                    LogUtils.w("解析成功：" + result);
//                                } else {
//                                    LogUtils.e("再次尝试中....");
//                                    mCamera.startPreview();
//                                    mCamera.autoFocus(self);
//                                    // todo:这里也可以做最大重试次数的限制...
//                                }
//                            } catch (Exception e) {
//                                LogUtils.e("Error accessing file: " + e.getMessage());
//                            }
//                        }
//                    });
//                }
//            }
//        }

        @Override
        public void onCameraPreview(CameraView cameraView, final byte[] data) {
//            Toast.makeText(cameraView.getContext(), "预览中", Toast.LENGTH_SHORT)
//                    .show();
//                    analyseResult.setText("yulan");
//            getBackgroundHandler().post(new Runnable() {
//                @Override
//                public void run() {
//                    if (data == null) {
//                        return;
//                    } else {
//                        Bitmap bitmap = ColorConvertUtil.byteArrayToBitmap(data, 640, 480);
//                        ASTask cnnTask = new ASTask();
//                        cnnTask.execute(bitmap);
//                    }
//                }
//            });
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);
            initPrediction();
            Toast.makeText(cameraView.getContext(), R.string.picture_taken, Toast.LENGTH_SHORT)
                    .show();
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
//                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
//                            "picture.jpg");
                    OutputStream os = null;
                    try {
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        photoFile = createFileIfNeed("DynamicScore_IMG_"+ timeStamp + ".jpg");
//                        photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
//                           "DynamicScore_IMG_"+ timeStamp + ".jpg");
                        analyseResult.setText("得分：90.00000");
                        //analyseResult.setBackgroundResource(R.color.colorPrimary);
                        Log.d(TAG,"file name:" + photoFile);
                        os = new FileOutputStream(photoFile);
                        os.write(data);
                        os.close();
                        updatePhotoMedia(photoFile,DynamicScoreActivity.this);
                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + photoFile, e);
                    } finally {
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                }
            });
        }

//        @Override
//        public void currentSceneScore(CameraView cameraView, final byte[] data){
//            initPrediction();
//            getBackgroundHandler().post(new Runnable() {
//                @Override
//                public void run() {
//                    OutputStream os = null;
//                    try {
//                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//                        photoFile = createFileIfNeed("DynamicScore_IMG_"+ timeStamp + ".jpg");
////                        photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
////                           "DynamicScore_IMG_"+ timeStamp + ".jpg");
//                        analyseResult.setText("得分：90.00000");
//                        //analyseResult.setBackgroundResource(R.color.colorPrimary);
//                        Log.d(TAG,"file name:" + photoFile);
//                        os = new FileOutputStream(photoFile);
//                        os.write(data);
//                        os.close();
//                        updatePhotoMedia(photoFile,DynamicScoreActivity.this);
//                    } catch (IOException e) {
//                        Log.w(TAG, "Cannot write to " + photoFile, e);
//                    } finally {
//                        if (os != null) {
//                            try {
//                                os.close();
//                            } catch (IOException e) {
//                                // Ignore
//                            }
//                        }
//                    }
//                }
//            });
//        }

    };



    public static class ConfirmationDialogFragment extends DialogFragment {

        private static final String ARG_MESSAGE = "message";
        private static final String ARG_PERMISSIONS = "permissions";
        private static final String ARG_REQUEST_CODE = "request_code";
        private static final String ARG_NOT_GRANTED_MESSAGE = "not_granted_message";

        public static ConfirmationDialogFragment newInstance(@StringRes int message,
                                                             String[] permissions, int requestCode, @StringRes int notGrantedMessage) {
            ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE, message);
            args.putStringArray(ARG_PERMISSIONS, permissions);
            args.putInt(ARG_REQUEST_CODE, requestCode);
            args.putInt(ARG_NOT_GRANTED_MESSAGE, notGrantedMessage);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(args.getInt(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String[] permissions = args.getStringArray(ARG_PERMISSIONS);
                                    if (permissions == null) {
                                        throw new IllegalArgumentException();
                                    }
                                    ActivityCompat.requestPermissions(getActivity(),
                                            permissions, args.getInt(ARG_REQUEST_CODE));
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getActivity(),
                                            args.getInt(ARG_NOT_GRANTED_MESSAGE),
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                    .create();
        }

    }

    public class ASTask extends AsyncTask<Bitmap, Object, String> {
        // private List aestheticsScores;
        private Bitmap picture;
        AestheticsScoreDetector aestheticsScoreDetector;

        @Override
        protected void onPostExecute(String s) {
            Log.d(TAG,"score:" + s);
            analyseResult.setText("得分：" + s);
            super.onPostExecute(s);
        }

        @Override
        protected String doInBackground(Bitmap...bitmap) {
            aestheticsScoreDetector = new AestheticsScoreDetector(DynamicScoreActivity.this);
            AEModelConfiguration aeModelConfiguration = new AEModelConfiguration();
            aeModelConfiguration.getDetectImageConf().setDetectImageMode(AEConstants.AEImageDetectMode.OSP_MODE
                    | AEConstants.AEImageDetectMode.HF_MODE);
            aeModelConfiguration.getDetectImageConf().setDetectImageOutputType(AEConstants.AEImageDetectOutputType.OSP_DETAIL);
            aestheticsScoreDetector.setAeModelConfiguration(aeModelConfiguration);
            aestheticsScoreDetector.release();
            return getImageScore(bitmap[0]);
        }

        public String getImageScore(Bitmap bitmap) {
            if (bitmap == null) {
                return "bitmap is null";
            }
            Frame frame = new Frame();
            frame.setBitmap(bitmap);
            JSONObject jsonObject = aestheticsScoreDetector.detect(frame, null);
            if (jsonObject == null) {
                Log.e(TAG, "return JSONObject is null");
                return "return JSONObject is null";
            }
            if (!jsonObject.optString("resultCode").equals("0")) {
                Log.e(TAG, "return JSONObject is not 0");
                return jsonObject.optString("resultCode");
            }
            AestheticsScore aestheticsScore = aestheticsScoreDetector.convertResult(jsonObject);
            if (null == aestheticsScore) {
                Log.e(TAG, "return aestheticsScore is null");
                return "return aestheticsScore is null";
            }
            String score = Float.toString(aestheticsScore.getScore());
            return score;
        }
    }

}
