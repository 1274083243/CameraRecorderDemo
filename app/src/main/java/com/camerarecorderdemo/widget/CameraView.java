package com.camerarecorderdemo.widget;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.camerarecorderdemo.render.CameraRender;

public class CameraView extends GLSurfaceView {
    public CameraRender getCameraRender() {
        return cameraRender;
    }

    private CameraRender cameraRender;
    public CameraView(Context context) {
        this(context,null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        cameraRender=new CameraRender(context,this);
        setRenderer(cameraRender);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
