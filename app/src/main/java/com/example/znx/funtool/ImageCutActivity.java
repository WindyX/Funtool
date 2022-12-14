package com.example.znx.funtool;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.huawei.hiai.vision.common.ConnectionCallback;
import com.huawei.hiai.vision.common.VisionBase;
import com.huawei.hiai.vision.image.segmentation.ImageSegmentation;
import com.huawei.hiai.vision.visionkit.common.Frame;
import com.huawei.hiai.vision.visionkit.image.ImageResult;
import com.huawei.hiai.vision.visionkit.image.segmentation.SegmentationConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageCutActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE =100;
    private static final int REQUEST_BACKGROUND_SELECT =200;
    private static final int REQUEST_IMAGE_SELECT = 400;
    private static final String LOG_TAG= "Image Cut";
    private static final int MY_ADD_CASE_CALL_PHONE = 6; //摄像头请求码
    private static final int MSG_SERVICE_CONNECTED = 1;
    private static final int MSG_SERVICE_UNCONNECTED = 2;
    private static final int MSG_PORTRAIT = 1;
    private static final int  MSG_CHANGE_BACKGROUND = 3;
    private static final int MSG_SHOW_RESULT = 11;
    private static final int MSG_SHOW_BACKGROUND = 10;

    private Button btn_OpenCamera;
    private Button btn_SelectPicture;
    private Button btn_SelectBackground;
    private ImageView imageView;
    private DragFrameLayout dragFrameLayout;

    private int displayImageMaxWidth;
    private int displayImageMaxHeight;
    private int actualDisplayWidth;
    private int actualDisplayHeight;
    private int handled_min_x;
    private int handled_max_x;
    private int handled_min_y;
    private int handled_max_y;

    private Uri fileUri;
    private File photoFile;
    private Handler mMyHandler = null;
    private MyHandlerThread mMyHandlerThread = null;
    private ImageSegmentation isEngine;

    private Bitmap originalBitmap;
    private Bitmap handledBitmap;
    private Bitmap backgroundBitmap;

    private void initPrediction(){
        btn_OpenCamera.setEnabled(false);
        btn_SelectPicture.setEnabled(false);
        btn_SelectBackground.setEnabled(false);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_cut);

        // 计算图片显示最大长宽
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        displayImageMaxWidth = screenWidth * 4 / 5;
        displayImageMaxHeight = screenHeight * 4 / 5;

        mMyHandlerThread = new MyHandlerThread();
        mMyHandlerThread.start();
        mMyHandler = new Handler(mMyHandlerThread.getLooper(), mMyHandlerThread);
        imageView = (ImageView)findViewById(R.id.image);
        dragFrameLayout= (DragFrameLayout) findViewById(R.id.drag_framelayout);
        btn_OpenCamera = (Button)findViewById(R.id.btnCamera);
        btn_SelectPicture = (Button)findViewById(R.id.btnSelectPicture);
        btn_SelectBackground = (Button)findViewById(R.id.btnSelectBackground);

        //导航栏
        Toolbar toolbar = (Toolbar) findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.mipmap.ic_return);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        btn_OpenCamera.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                if(ContextCompat.checkSelfPermission(ImageCutActivity.this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(ImageCutActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ImageCutActivity.this, new String[]
                            {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_ADD_CASE_CALL_PHONE);
                }
                else {
                    try {
                        //有权限，打开摄像头
                        takePhoto();
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btn_SelectPicture.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
        });

        btn_SelectBackground.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_BACKGROUND_SELECT);
            }
        });

        // 初始化布局大小
        dragFrameLayout.getLayoutParams().width = displayImageMaxWidth;
        dragFrameLayout.getLayoutParams().height = displayImageMaxHeight;
        dragFrameLayout.addDragChildView(imageView);
        dragFrameLayout.setOnDragDropListener(new DragFrameLayout.OnDragDropListener() {
            @Override
            public void onDragDrop(boolean captured) {
                // captured=true：按下屏幕（不放开）
                // captured=false：松开屏幕
                if (captured) {
                    Log.d(LOG_TAG, "captured=true");
                } else {
                    Log.d(LOG_TAG, "captured=false");
                }
            }
        });

        //To connect HiAI Engine service using VisionBase
        VisionBase.init(ImageCutActivity.this, new ConnectionCallback() {
            @Override
            public void onServiceConnect() {
                mHandler.sendEmptyMessage(MSG_SERVICE_CONNECTED);
            }

            @Override
            public void onServiceDisconnect() {
                mHandler.sendEmptyMessage(MSG_SERVICE_UNCONNECTED);
            }
        });
        requestPermissions();
    }

    /**
     *
     * @param origin 原始图片
     * @param handled 抠图后的剪影图
     * @return 实际抠图得到的图像
     */
    private Bitmap setHandledBitmap(Bitmap origin, Bitmap handled) {
        if (handled == null)
            return null;

        boolean hasInit = false;
        // 初始化
        // result：最终返回抠好的图像，使用剪影初始化
        // cut：用于裁剪
        Bitmap result = handled.createScaledBitmap(handled, actualDisplayWidth,actualDisplayHeight, false);
        Bitmap cut = origin.createScaledBitmap(origin, actualDisplayWidth, actualDisplayHeight, false);

        for (int x = 0; x < result.getWidth(); x++) {
            for (int y = 0; y < result.getHeight(); y++) {
                int color = result.getPixel(x, y);
                // 剪影背景，对应最终抠图的透明背景
                if (color == Color.BLACK) {
                    color = Color.argb(0, 255, 255, 255);
                    cut.setPixel(x, y, color);
                } else {
                    // 初始化抠图边界
                    if (!hasInit) {
                        hasInit = true;
                        handled_min_x = x;
                        handled_max_x = x;
                        handled_min_y = y;
                        handled_max_y = y;
                    } else {    // 设置抠图部分的边界
                        handled_min_x = Math.min(handled_min_x, x);
                        handled_max_x = Math.max(handled_max_x, x);
                        handled_min_y = Math.min(handled_min_y, y);
                        handled_max_y = Math.max(handled_max_y, y);
                    }
                }
            }
        }

        if (handled_min_x < handled_max_x && handled_min_y < handled_max_y) {
            // 将抠图部分从原图中裁剪出来
            result = Bitmap.createBitmap(cut, handled_min_x, handled_min_y,
                    (handled_max_x - handled_min_x), (handled_max_y - handled_min_y), null, false);
        } else {
            Log.e(LOG_TAG,
                    "location error! min_x=" + handled_min_x + ", max_x=" + handled_max_x
                            + ", min_y=" + handled_min_y + ", max_y=" + handled_max_y);
        }

        return result;
    }

    private void drawImage(Bitmap image) {
        // 绘制透明背景
        Bitmap transparentBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(transparentBitmap);
        // 将抠好的图画到透明画布上
        canvas.drawBitmap(image, 0, 0, null);
        imageView.setImageBitmap(transparentBitmap);
    }

    private class MyHandlerThread extends HandlerThread implements Handler.Callback{
        public MyHandlerThread(){
            super("MyHandler");
        }

        public MyHandlerThread(String name){
            super(name);
        }

        @Override
        public boolean handleMessage(Message arg0){
            Frame frame = new Frame();
            frame.setBitmap(originalBitmap);
            switch (arg0.what){
                case MSG_PORTRAIT: //scene detect
                    long s4 = System.currentTimeMillis();
                    SegmentationConfiguration sc = new SegmentationConfiguration();
                    sc.setSegmentationType(SegmentationConfiguration.TYPE_PORTRAIT);
                    isEngine.setSegmentationConfiguration(sc);
                    ImageResult srt = isEngine.doSegmentation(frame,null);
                    if(srt == null){
                        return false;
                    }
                    if(handledBitmap != null){
                        handledBitmap.recycle();
                    }
                    handledBitmap = srt.getBitmap();
                    // Record handle time
                    long end4 = System.currentTimeMillis();
                    Log.e(LOG_TAG,"portraitsegmentation needs time:" + (end4 - s4));
                    // 获取抠好的图像
                    handledBitmap = setHandledBitmap(originalBitmap, handledBitmap);
                    mHandler.sendEmptyMessage(MSG_SHOW_RESULT);
                    break;
//                case MSG_CHANGE_BACKGROUND:
//                    mHandler.sendEmptyMessage(MSG_SHOW_BACKGROUND);
                default:
                    break;
            }
            return false;
        }
    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_SERVICE_CONNECTED:
                    Log.d(LOG_TAG, "bind ok");
                    Toast.makeText(ImageCutActivity.this, "bind success", Toast.LENGTH_SHORT).show();
                    isEngine = new ImageSegmentation(ImageCutActivity.this);
                    break;
                case MSG_SERVICE_UNCONNECTED:
                    Toast.makeText(ImageCutActivity.this, "disconnect", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_SHOW_RESULT:
                    dragFrameLayout.setBackground(new BitmapDrawable(backgroundBitmap));
                    drawImage(handledBitmap);
                    btn_OpenCamera.setEnabled(true);
                    btn_SelectPicture.setEnabled(true);
                    btn_SelectBackground.setEnabled(true);
                    break;
                case MSG_SHOW_BACKGROUND:
                    dragFrameLayout.setBackground(new BitmapDrawable(backgroundBitmap));
                    drawImage(handledBitmap);
                    btn_OpenCamera.setEnabled(true);
                    btn_SelectPicture.setEnabled(true);
                    btn_SelectBackground.setEnabled(true);
            }
        }
    };

    //点击返回键和查找键
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
//                case R.id.find:
//                    break;
            case android.R.id.home:
                returnHome(this);
                break;
            default:
        }
        return true;
    }

    //返回首页
    public static void returnHome(Context context){
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    //调用系统照相机
    private void takePhoto() throws  IOException{
        initPrediction();
        Log.d(LOG_TAG, "get uri");
        Intent i = new Intent();
        i.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        photoFile = createFileIfNeed("ImageCut_IMG_"+ timeStamp + ".jpg");
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M){
            fileUri = Uri.fromFile(photoFile);
        }else{
            //7.0调用系统相机拍照不再允许使用Uri方式，应该替换为FileProvider，并且这样可以解决MIUI系统上拍照返回size为0的情况
            fileUri = FileProvider.getUriForFile(ImageCutActivity.this, "com.example.znx.funtool.fileProvider", photoFile);
            Log.d(LOG_TAG,"end get uri = " +  fileUri);
        }
        i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        Log.d(LOG_TAG,"store successful");
        startActivityForResult(i, REQUEST_IMAGE_CAPTURE);
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

    private void requestPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int permission = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[] {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    }, 0x0010);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT
                || requestCode == REQUEST_BACKGROUND_SELECT)
                && resultCode == RESULT_OK){
            String imgPath;
            if(requestCode == REQUEST_IMAGE_CAPTURE){
                imgPath = photoFile.getAbsolutePath();
            }else{
                fileUri = data.getData();
                String [] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = ImageCutActivity.this.getContentResolver().query(fileUri, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgPath = cursor.getString(columnIndex);
                cursor.close();
            }
            if(requestCode == REQUEST_BACKGROUND_SELECT){
                if(originalBitmap == null){
                    Toast.makeText(ImageCutActivity.this,"请选择一张照片！",Toast.LENGTH_SHORT).show();
                }
                else{
                    if(backgroundBitmap != null && !backgroundBitmap.isRecycled()){
                        backgroundBitmap = null;
                    }
                    // 设置背景
                    backgroundBitmap = CompactBitmap.decodeSampledBitmapByPath(imgPath, displayImageMaxWidth, displayImageMaxHeight);
//                    mMyHandler.sendEmptyMessage(MSG_CHANGE_BACKGROUND);
                    mHandler.sendEmptyMessage(MSG_SHOW_BACKGROUND);
                }
            }else{
                Log.d(LOG_TAG, "imgPath = " + imgPath);
                if(originalBitmap != null && !originalBitmap.isRecycled()){
                    originalBitmap = null;
                }
                originalBitmap = BitmapFactory.decodeFile(imgPath);

                // 计算图像实际显示大小
                // 取比例最大值，保证在最大长宽范围内可显示
                if ((originalBitmap.getWidth() * displayImageMaxHeight) > (originalBitmap.getHeight() * displayImageMaxWidth)) {
                    actualDisplayWidth = displayImageMaxWidth;
                    actualDisplayHeight = originalBitmap.getHeight() * displayImageMaxWidth / originalBitmap.getWidth();
                } else {
                    actualDisplayHeight = displayImageMaxHeight;
                    actualDisplayWidth = originalBitmap.getWidth() * displayImageMaxHeight / originalBitmap.getHeight();
                }

                // 读取用于显示的原图
                Bitmap displayImage = CompactBitmap.decodeSampledBitmapByPath(imgPath, actualDisplayWidth, actualDisplayHeight);
                imageView.setImageBitmap(displayImage);
                // 初始化背景为灰色
                backgroundBitmap = Bitmap.createBitmap(actualDisplayWidth, actualDisplayHeight, Bitmap.Config.ARGB_8888);
                backgroundBitmap.eraseColor(Color.GRAY);

                mMyHandler.sendEmptyMessage(MSG_PORTRAIT);
                Log.d(LOG_TAG,"originalBitmap = "+ imgPath);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDestroy(){
        isEngine.release();
        mMyHandlerThread.quit();
        super.onDestroy();
    }
}
