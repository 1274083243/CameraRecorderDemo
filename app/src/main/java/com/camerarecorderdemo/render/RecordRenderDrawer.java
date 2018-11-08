package com.camerarecorderdemo.render;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


import com.camerarecorderdemo.encoderc.VideoEncoder;
import com.camerarecorderdemo.util.EGLHelper;
import com.camerarecorderdemo.util.GlesUtil;
import com.camerarecorderdemo.util.StorageUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


/**
 * Created By Chengjunsen on 2018/9/21
 */
public class RecordRenderDrawer  implements Runnable{
    private String TAG="RecordRenderDrawer";
    // 绘制的纹理 ID
    private int mTextureId;
    private VideoEncoder mVideoEncoder;
    private String mVideoPath;
    private Handler mMsgHandler;
    private EGLHelper mEglHelper;
    private EGLSurface mEglSurface;
    private boolean isRecording;
    private EGLContext mEglContext;

    private int av_Position;
    private int af_Position;
    private int s_Texture;
    private int width;
    private int height;
    private int mProgram;
    private float[] verTextData={
            -1,-1,
            1,-1,
            -1,1,
            1,1
    };
    private float[] fragData={
            0,1,
            1,1,
            0,0,
            1,0
    };
    private FloatBuffer vertextBuffer;
    private FloatBuffer fragBuffer;

    public RecordRenderDrawer(Context context) {
        this.mVideoEncoder = null;
        this.mEglHelper = null;
        this.mTextureId = 0;
        this.isRecording = false;
        new Thread(this).start();
    }

    private void initBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(verTextData.length * 4)
                .order(ByteOrder.nativeOrder());
        vertextBuffer=buffer.asFloatBuffer();
        vertextBuffer.put(verTextData);
        vertextBuffer.position(0);
        ByteBuffer buffer1 = ByteBuffer.allocateDirect(fragData.length * 4)
                .order(ByteOrder.nativeOrder());
        fragBuffer=buffer1.asFloatBuffer();
        fragBuffer.put(fragData);
        fragBuffer.position(0);
    }
    public void setInputTextureId(int textureId) {
        this.mTextureId = textureId;
        Log.e(TAG, "setInputTextureId: " + textureId);
    }

    public void create() {
        mEglContext = EGL14.eglGetCurrentContext();
    }

    public void startRecord() {
        Log.e(TAG, "startRecord context : " + mEglContext.toString());
        Message msg = mMsgHandler.obtainMessage(MsgHandler.MSG_START_RECORD, width, height, mEglContext);
        mMsgHandler.sendMessage(msg);
        isRecording = true;
    }

    public void stopRecord() {
        Log.e(TAG, "stopRecord");
        isRecording = false;
        mMsgHandler.sendMessage(mMsgHandler.obtainMessage(MsgHandler.MSG_STOP_RECORD));
    }

    public void quit() {
        mMsgHandler.sendMessage(mMsgHandler.obtainMessage(MsgHandler.MSG_QUIT));
    }

    public void surfaceChangedSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
    public void draw(long timestamp) {
        if (isRecording) {
            Log.e(TAG, "draw: ");
            Message msg = mMsgHandler.obtainMessage(MsgHandler.MSG_FRAME, timestamp);
            mMsgHandler.sendMessage(msg);
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        mMsgHandler = new MsgHandler();
        Looper.loop();
    }

    private class MsgHandler extends Handler {
        public static final int MSG_START_RECORD = 1;
        public static final int MSG_STOP_RECORD = 2;
        public static final int MSG_UPDATE_CONTEXT = 3;
        public static final int MSG_UPDATE_SIZE = 4;
        public static final int MSG_FRAME = 5;
        public static final int MSG_QUIT = 6;

        public MsgHandler() {

        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_RECORD:
                    prepareVideoEncoder((EGLContext) msg.obj, msg.arg1, msg.arg2);
                    break;
                case MSG_STOP_RECORD:
                    stopVideoEncoder();
                    break;
                case MSG_UPDATE_CONTEXT:
                    updateEglContext((EGLContext) msg.obj);
                    break;
                case MSG_UPDATE_SIZE:
                    updateChangedSize(msg.arg1, msg.arg2);
                    break;
                case MSG_FRAME:
                    drawFrame((long)msg.obj);
                    break;
                case MSG_QUIT:
                    quitLooper();
                    break;
                default:
                    break;
            }
        }
    }

    private void prepareVideoEncoder(EGLContext context, int width, int height) {
        try {
            mEglHelper = new EGLHelper();
            mEglHelper.createGL(context);
            mVideoPath = StorageUtil.getVedioPath(true) + "glvideo.mp4";
            Log.e(TAG,"视频地址:"+mVideoPath);
            mVideoEncoder = new VideoEncoder(width, height, new File(mVideoPath));
            mEglSurface = mEglHelper.createWindowSurface(mVideoEncoder.getInputSurface());
            boolean error = mEglHelper.makeCurrent(mEglSurface);
            if (!error) {
                Log.e(TAG, "prepareVideoEncoder: make current error");
            }
            onCreated();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopVideoEncoder() {
        mVideoEncoder.drainEncoder(true);
        if (mEglHelper != null) {
            mEglHelper.destroySurface(mEglSurface);
            mEglHelper.destroyGL();
            mEglSurface = EGL14.EGL_NO_SURFACE;
            mVideoEncoder.release();
            mEglHelper = null;
            mVideoEncoder = null;
        }
    }

    private void updateEglContext(EGLContext context) {
        mEglSurface = EGL14.EGL_NO_SURFACE;
        mEglHelper.destroyGL();
        mEglHelper.createGL(context);
        mEglSurface = mEglHelper.createWindowSurface(mVideoEncoder.getInputSurface());
        boolean error = mEglHelper.makeCurrent(mEglSurface);
        if (!error) {
            Log.e(TAG, "prepareVideoEncoder: make current error");
        }
    }

    private void drawFrame(long timeStamp) {
        Log.e(TAG, "drawFrame: " + timeStamp );
        mEglHelper.makeCurrent(mEglSurface);
        mVideoEncoder.drainEncoder(false);
        onDraw();
        mEglHelper.setPresentationTime(mEglSurface, timeStamp);
        mEglHelper.swapBuffers(mEglSurface);
    }

    private void updateChangedSize(int width, int height) {

    }

    private void quitLooper() {
        Looper.myLooper().quit();
    }

    protected void onCreated() {
        mProgram = GlesUtil.createProgram(getVertexSource(), getFragmentSource());
        av_Position = GLES20.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES20.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES20.glGetUniformLocation(mProgram, "s_Texture");
        initBuffer();
        Log.e(TAG, "onCreated: av_Position " + av_Position);
        Log.e(TAG, "onCreated: af_Position " + af_Position);
        Log.e(TAG, "onCreated: s_Texture " + s_Texture);
        Log.e(TAG, "onCreated: error " + GLES20.glGetError());
    }
    protected void clear(){
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }
    protected void useProgram(){
        GLES20.glUseProgram(mProgram);
    }
    protected void onDraw() {
        clear();
        useProgram();
        GLES20.glViewport(0, 0, width, height);
        GLES20.glEnableVertexAttribArray(av_Position);
        GLES20.glEnableVertexAttribArray(af_Position);
        GLES20.glVertexAttribPointer(av_Position, 2, GLES20.GL_FLOAT, false, 8, vertextBuffer);
        GLES20.glVertexAttribPointer(af_Position, 2, GLES20.GL_FLOAT, false, 8, fragBuffer);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glUniform1i(s_Texture, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(av_Position);
        GLES20.glDisableVertexAttribArray(af_Position);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    protected String getVertexSource() {
        final String source = "attribute vec4 av_Position; " +
                "attribute vec2 af_Position; " +
                "varying vec2 v_texPo; " +
                "void main() { " +
                "    v_texPo = af_Position; " +
                "    gl_Position = av_Position; " +
                "}";
        return source;
    }

    protected String getFragmentSource() {
        final String source = "precision mediump float;\n" +
                "varying vec2 v_texPo;\n" +
                "uniform sampler2D s_Texture;\n" +
                "void main() {\n" +
                "   vec4 tc = texture2D(s_Texture, v_texPo);\n" +
                "   gl_FragColor = texture2D(s_Texture, v_texPo);\n" +
                "}";
        return source;
    }
}
