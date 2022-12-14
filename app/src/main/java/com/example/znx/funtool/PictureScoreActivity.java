package com.example.znx.funtool;

import android.Manifest;
import android.support.v7.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PathMeasure;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.content.Context;
import android.widget.Toast;

import com.huawei.hiai.vision.common.ConnectionCallback;
import com.huawei.hiai.vision.common.VisionBase;
import com.huawei.hiai.vision.image.detector.AestheticsScoreDetector;
import com.huawei.hiai.vision.visionkit.common.Frame;
import com.huawei.hiai.vision.visionkit.image.detector.AEModelConfiguration;
import com.huawei.hiai.vision.visionkit.image.detector.AestheticsScore;
import com.huawei.hiai.vision.visionkit.image.detector.aestheticsmodel.AEConstants;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class PictureScoreActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE =100;
    private static final int REQUEST_IMAGE_SELECT = 400;
    private static final String LOG_TAG= "Picture Score";
    private static final int PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE = 1000;
    private static final int MY_ADD_CASE_CALL_PHONE = 6; //??????????????????
    private ProgressDialog dialog;
    private Button btnCamera;
    private Button btnSelectPicture;
    private ImageView image;
    private Uri fileUri;
    private File photoFile;
    private Bitmap bmp;
    private Bitmap tempbmp;
    private String analyseResult;
    private GridView gridView;
    private ArrayList<HashMap<String, Object>> imageItem;
    private SimpleAdapter simpleAdapter;     //?????????
    private String []form;
    private int []to;
    private TextView highestScore;
    private TextView imageName;
    private double score;
    private String imgname;
    private int number;
    private int counter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_score);
        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnSelectPicture = (Button) findViewById(R.id.btnSelectPicture);
        //image = (ImageView) findViewById(R.id.image);
        //analyseResult = (TextView) findViewById(R.id.analyse_result);
        gridView = (GridView)findViewById(R.id.gridview);
        highestScore = (TextView)findViewById(R.id.highest_score);
        imageName = (TextView)findViewById(R.id.image_name);
        counter = 0;

        /*
         * ????????????????????????????????????
         * ?????????????????????
         * SimpleAdapter??????imageItem???????????? R.layout.grid_item?????????
         */
        //????????????????????????
//        bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.grid_add);
//        analyseResult = new String("?????????");
        form = new String[]{"image", "text"};
        to = new int[]{R.id.image, R.id.analyse_result};
        imageItem = new ArrayList<HashMap<String, Object>>();
//        HashMap<String, Object> map = new HashMap<String, Object>();
//        map.put("image", bmp);
//        map.put("text", analyseResult);
//        imageItem.add(map);
//        simpleAdapter = new SimpleAdapter(this, imageItem, R.layout.grid_item, form, to);
//        gridView.setAdapter(simpleAdapter);
//        simpleAdapter = new SimpleAdapter(this,
//                imageItem, R.layout.grid_item,
//                new String[] { "itemImage"}, new int[] { R.id.image});


        /*
         * HashMap??????bmp?????????GridView????????????,????????????????????????ID????????? ???
         * map.put("itemImage", R.drawable.img);
         * ????????????:
         *              1.???????????????BaseAdapter??????
         *              2.ViewBinder()????????????
         *  ?????? http://blog.csdn.net/admin_/article/details/7257901
         */
//        simpleAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
//            @Override
//            public boolean setViewValue(View view, Object data,
//                                        String textRepresentation) {
//                // TODO Auto-generated method stub
//                if(view instanceof ImageView && data instanceof Bitmap){
//                    ImageView i = (ImageView)view;
//                    i.setImageBitmap((Bitmap) data);
//                    return true;
//                }
//                return false;
//            }
//        });
//        gridView.setAdapter(simpleAdapter);

        /*
         * ??????GridView????????????
         * ??????:??????????????????????????? ?????????????????????import android.view.View;
         */
//        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
//            {
////                if( imageItem.size() == 10) { //????????????????????????
////                    Toast.makeText(MainActivity.this, "?????????9?????????", Toast.LENGTH_SHORT).show();
////                }
//                if(position == 0) { //?????????????????????+ 0??????0?????????
//                    Toast.makeText(PictureScoreActivity.this, "????????????", Toast.LENGTH_SHORT).show();
//                    //????????????
//                    Intent intent = new Intent(Intent.ACTION_PICK,
//                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                    startActivityForResult(intent, REQUEST_IMAGE_SELECT);
//                    //??????onResume()????????????
//                }
////                else {
////                    dialog(position);
////                    //Toast.makeText(MainActivity.this, "?????????"+(position + 1)+" ?????????",
////                    //      Toast.LENGTH_SHORT).show();
////                }
//            }
//        });

    //To connect HiAI Engine service using VisionBase
        VisionBase.init(PictureScoreActivity.this, new ConnectionCallback() {
            @Override
            public void onServiceConnect() {
                Log.i(LOG_TAG, "onServiceConnect");
            }

            @Override
            public void onServiceDisconnect() {
                Log.i(LOG_TAG, "onServiceDisconnect");
            }
        });

        requetsPermissions();

        //?????????
        Toolbar toolbar = (Toolbar) findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.mipmap.ic_return);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        btnCamera.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                if(ContextCompat.checkSelfPermission(PictureScoreActivity.this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(PictureScoreActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(PictureScoreActivity.this, new String[]
                            {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_ADD_CASE_CALL_PHONE);
                }
                else {
                    try {
                        //???????????????????????????
                        takePhoto();
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btnSelectPicture.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
        });
    }

        private void initPrediction() {
            btnCamera.setEnabled(false);
            btnSelectPicture.setEnabled(false);
            highestScore.setText("");
            imageName.setText("");
            //analyseResult.setText("");
        }


    //???????????????????????????
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

    //?????????
//    public boolean onCreateOptionsMenu(Menu menu){
//        getMenuInflater().inflate(R.menu.toobar, menu);
//        return true;
//    }

    //????????????
    public static void returnHome(Context context){
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    //?????????????????????
    private void takePhoto() throws  IOException{
        initPrediction();
        Log.d(LOG_TAG, "get uri");
        Intent i = new Intent();
        i.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        photoFile = createFileIfNeed("PictureScore_IMG_"+ timeStamp + ".jpg");
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M){
            fileUri = Uri.fromFile(photoFile);
        }else{
            //7.0??????????????????????????????????????????Uri????????????????????????FileProvider???????????????????????????MIUI?????????????????????size???0?????????
            fileUri = FileProvider.getUriForFile(PictureScoreActivity.this, "com.example.znx.funtool.fileProvider", photoFile);
            Log.d(LOG_TAG,"end get uri = " +  fileUri);
        }
        i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        Log.d(LOG_TAG,"store successful");
        startActivityForResult(i, REQUEST_IMAGE_CAPTURE);
    }

    //????????????
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

    private void requetsPermissions(){
        try{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(permission != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE},PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch (requestCode){
            case PERMISSION_REQUEST_CODE_READ_EXTERNAL_STORAGE:
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED){
                    Log.i(LOG_TAG,"User grant the permission of READ_EXTERNAL_STORAGE");
                }else{
                    Toast.makeText(this,"Please grant the permission", Toast.LENGTH_SHORT).show();
                    Log.i(LOG_TAG,"User did not grant the permission of READ_EXTERNAL_STORAGE");
                    this.finish();
                }
                break;
             default:
                 break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT)
                && resultCode == RESULT_OK){
            String imgPath;
            if(requestCode == REQUEST_IMAGE_CAPTURE){
                imgPath = photoFile.getAbsolutePath();
            }else{
                fileUri = data.getData();
                String [] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = PictureScoreActivity.this.getContentResolver().query(fileUri, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgPath = cursor.getString(columnIndex);
                cursor.close();
            }
            Log.d(LOG_TAG, "imgPath = " + imgPath);
//            if(bmp != null && !bmp.isRecycled()){
//                bmp = null;
//            }
            //??????????????????????????????????????????
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;
            int width = screenWidth * 4 / 3;
            int height = screenHeight * 4 / 3;
            tempbmp = CompactBitmap.decodeSampledBitmapByPath(imgPath, width, height);
            Log.d(LOG_TAG,"bitmap = "+ imgPath);
//            image.setImageBitmap(tempbmp);
//            //??????
//            HashMap<String, Object> map = new HashMap<String, Object>();
//            map.put("itemImage", tempbmp);
//            imageItem.add(map);
//            simpleAdapter = new SimpleAdapter(this,
//                    imageItem, R.layout.grid_item,
//                    new String[] { "itemImage"}, new int[] { R.id.image});
//            simpleAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
//                @Override
//                public boolean setViewValue(View view, Object data,
//                                            String textRepresentation) {
//                    // TODO Auto-generated method stub
//                    if(view instanceof ImageView && data instanceof Bitmap){
//                        ImageView i = (ImageView)view;
//                        i.setImageBitmap((Bitmap) data);
//                        return true;
//                    }
//                    return false;
//                }
//            });
//            gridView.setAdapter(simpleAdapter);
//            simpleAdapter.notifyDataSetChanged();
            counter++;
            if(counter == 1){
                number = 1;
            }
            dialog = ProgressDialog.show(PictureScoreActivity.this,"Predicting...","Wait for one second...",true);
            ASTask cnnTask = new ASTask();
            cnnTask.execute(imgPath);
        }
        btnSelectPicture.setEnabled(true);
        btnCamera.setEnabled(true);
        imageName.setText("?????????????????????????????????" + imgname);
        //analyseResult.setText("");
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getImageName(String path){
        int length = path.length();
        String name = "";
        int number = path.lastIndexOf("/");
        for(int i = number + 1; i < length; i++){
            name += path.charAt(i);
        }
        return name;
    }

    public class ASTask extends AsyncTask<String, Object, String> {
       // private List aestheticsScores;
        private Bitmap picture;
        AestheticsScoreDetector aestheticsScoreDetector;

        @Override
        protected void onPostExecute(String s) {
            //analyseResult.setText("?????????" + s);
            imageName.setText("?????????????????????" + number + "????????????" + imgname);
            highestScore.setText("????????????" + score);
            Log.d(LOG_TAG,"score:" + s);
            analyseResult = new String("?????????" + s);
            dialog.dismiss();
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("image", tempbmp);
            map.put("text", analyseResult);
            imageItem.add(map);
            simpleAdapter = new SimpleAdapter(PictureScoreActivity.this, imageItem, R.layout.grid_item, form, to);
            simpleAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Object data,
                                            String textRepresentation) {
                    // TODO Auto-generated method stub
                    if(view instanceof ImageView && data instanceof Bitmap){
                        ImageView i = (ImageView)view;
                        i.setImageBitmap((Bitmap) data);
                        return true;
                    }
                    return false;
                }
            });
            gridView.setAdapter(simpleAdapter);
            simpleAdapter.notifyDataSetChanged();
            super.onPostExecute(s);
        }

        @Override
        protected String doInBackground(String... path) {
            aestheticsScoreDetector = new AestheticsScoreDetector(PictureScoreActivity.this);
            AEModelConfiguration aeModelConfiguration = new AEModelConfiguration();
            aeModelConfiguration.getDetectImageConf().setDetectImageMode(AEConstants.AEImageDetectMode.OSP_MODE
            | AEConstants.AEImageDetectMode.HF_MODE);
            aeModelConfiguration.getDetectImageConf().setDetectImageOutputType(AEConstants.AEImageDetectOutputType.OSP_DETAIL);
            aestheticsScoreDetector.setAeModelConfiguration(aeModelConfiguration);
            String imgPath = path[0];
            String resultScore = null;
            //Cursor cursor = PictureScoreActivity.this.getContentResolver().query(uri[0],filePathColumn,null,null,null);
            picture = BitmapFactory.decodeFile(imgPath);
            resultScore = getImageScore(picture);
            aestheticsScoreDetector.release();
            if(counter == 1){
                imgname = getImageName(imgPath);
                score =  Double.parseDouble(resultScore);
            }
            else{
                if(Double.parseDouble(resultScore) > score ){
                    score = Double.parseDouble(resultScore);
                    imgname = getImageName(imgPath);
                    number = counter;
                }
            }
            return resultScore;
        }

        public String getImageScore(Bitmap bitmap) {
            if (bitmap == null) {
                return "bitmap is null";
            }
            Frame frame = new Frame();
            frame.setBitmap(bitmap);
            JSONObject jsonObject = aestheticsScoreDetector.detect(frame, null);
            if (jsonObject == null) {
                Log.e(LOG_TAG, "return JSONObject is null");
                return "return JSONObject is null";
            }
            if (!jsonObject.optString("resultCode").equals("0")) {
                Log.e(LOG_TAG, "return JSONObject is not 0");
                return jsonObject.optString("resultCode");
            }
            AestheticsScore aestheticsScore = aestheticsScoreDetector.convertResult(jsonObject);
            if (null == aestheticsScore) {
                Log.e(LOG_TAG, "return aestheticsScore is null");
                return "return aestheticsScore is null";
            }
            String score = Float.toString(aestheticsScore.getScore());
            return score;
        }
    }
}
