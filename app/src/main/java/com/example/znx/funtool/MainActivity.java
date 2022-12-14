package com.example.znx.funtool;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.List;

public class MainActivity extends AppCompatActivity{
    private LinearLayout linearLayout;
    private ViewPager viewPager;
    private List<View> views;
    private List<Fragment>list;
    private FragmentPagerAdapter adapter;
    private View.OnClickListener onClickListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        linearLayout = (LinearLayout) findViewById(R.id.drawer_layout);
        RelativeLayout buttonLayout = (RelativeLayout) findViewById(R.id.button_layout);
        Resources resources = (Resources)getBaseContext().getResources();
        Button btnPictureScore = (Button)findViewById(R.id.button_pictureScore);
        Button btnImageCut = (Button)findViewById(R.id.button_imageCut);
        Button btnDynamicScore = (Button)findViewById(R.id.button_dynamic_score);

//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
//            //actionBar.setHomeAsUpIndicator(R.mipmap.ic_return);
//        }

        //setStatusBar();
        btnPictureScore.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intentPictureScore = new Intent(MainActivity.this,PictureScoreActivity.class);
                startActivity(intentPictureScore);
            }
        });
        btnImageCut.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intentImageCut = new Intent(MainActivity.this,ImageCutActivity.class);
                startActivity(intentImageCut);
            }
        });
        btnDynamicScore.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intentDynamicScore = new Intent(MainActivity.this,DynamicScoreActivity.class);
                startActivity(intentDynamicScore);
            }
        });
    }

//    protected void setStatusBar() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            MainActivity.this.getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));//设置状态栏颜色
//            //MainActivity.this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏图标和文字颜色为暗色
//        }
//    }
}
