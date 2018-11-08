package com.camerarecorderdemo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.camerarecorderdemo.render.CameraRender;
import com.camerarecorderdemo.widget.CameraView;

public class MainActivity extends AppCompatActivity {
    private CameraView surface;
    private CameraRender render;
    private Button btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surface=findViewById(R.id.surface);
        btn=findViewById(R.id.btn);
        render=surface.getCameraRender();
    }
    private boolean isStarting;
    public void start(View view) {
        if (isStarting){
            isStarting=false;
            btn.setText("开始录制");
            render.stopRecord();
        }else {
            isStarting=true;
            btn.setText("结束录制");
            render.startRecord();
        }
    }
}
