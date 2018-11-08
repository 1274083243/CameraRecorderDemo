package com.camerarecorderdemo.render;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.camerarecorderdemo.util.CameraUtil;
import com.camerarecorderdemo.util.GlesUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
public class CameraRender implements GLSurfaceView.Renderer {
    private String TAG="CameraRender";
    private int program;
    private int av_Position;
    private int af_Position;
    private int s_Texture;

    private int cameraTexureId;
    private int fboId;
    private int fboTextureId;
    private SurfaceTexture cameraSurfaceTexture;
    private ScreenRender screenRender;
    private RecordRenderDrawer recordRenderDrawer;
    private GLSurfaceView glSurfaceView;
    private float[] matrix = new float[16];
    private boolean hasRotate;
    private int u_Matrix;
    public CameraRender(Context context,GLSurfaceView glSurfaceView) {
        this.context = context;
        this.glSurfaceView=glSurfaceView;
        screenRender=new ScreenRender(context);
        recordRenderDrawer=new RecordRenderDrawer(context);
        Matrix.setIdentityM(matrix, 0);
    }
    private Context context;
    private float[] verTextData={
            -1,-1,
            1,-1,
            -1,1,
            1,1
    };
    private float[] fragData={
            0,0,
            1,0,
            0,1,
            1,1
    };
    private FloatBuffer vertextBuffer;
    private FloatBuffer fragBuffer;
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initBuffer();
        createOpenGlProgram();
        startPreview();
        screenRender.onSurfaceCreated();
        screenRender.setInputTexture(fboTextureId);
        recordRenderDrawer.create();
        recordRenderDrawer.setInputTextureId(fboTextureId);
    }
    private boolean update;
    public void startPreview() {
            if (CameraUtil.getCamera() == null) {
                cameraTexureId=GlesUtil.createCameraTexture();
                cameraSurfaceTexture=new SurfaceTexture(cameraTexureId);
                cameraSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        glSurfaceView.requestRender();
                        update=true;
                    }
                });
                CameraUtil.openCamera();
                CameraUtil.setDisplay(cameraSurfaceTexture);
            }
            CameraUtil.startPreview(context, 1080, 1920);
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        if (!hasRotate){
            hasRotate=true;
            Matrix.rotateM(matrix,0,270,0f,0,1);
        }
        screenRender.onSurfaceChanged(width,height);
        recordRenderDrawer.surfaceChangedSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!update){
            update=false;
            return;
        }
        cameraSurfaceTexture.updateTexImage();
        clear();
        useProgram();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
        GLES20.glEnableVertexAttribArray(av_Position);
        GLES20.glEnableVertexAttribArray(af_Position);
        GLES20.glVertexAttribPointer(av_Position, 2, GLES20.GL_FLOAT, false, 8, vertextBuffer);
        GLES20.glVertexAttribPointer(af_Position, 2, GLES20.GL_FLOAT, false, 8, fragBuffer);
        GLES20.glUniformMatrix4fv(u_Matrix, 1, false, matrix, 0);
        bindTexture(cameraTexureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        unBindTexure();
        GLES20.glDisableVertexAttribArray(av_Position);
        GLES20.glDisableVertexAttribArray(af_Position);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        recordRenderDrawer.draw(cameraSurfaceTexture.getTimestamp());
        screenRender.onDrawFrame();
    }
    private void unBindTexure() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    private void bindTexture(int textureId) {
        GLES20.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(s_Texture, 0);
    }
    protected void clear(){
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }
    protected void useProgram(){
        GLES20.glUseProgram(program);
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
    private void createOpenGlProgram() {
        program = GlesUtil.createProgram(getVertexSource(), getFragmentSource());
        av_Position = GLES20.glGetAttribLocation(program, "av_Position");
        af_Position = GLES20.glGetAttribLocation(program, "af_Position");
        s_Texture = GLES20.glGetUniformLocation(program, "s_Texture");
        u_Matrix=GLES20.glGetUniformLocation(program, "u_Matrix");
        fboTextureId=GlesUtil.createFrameTexture(1080,1920);
        fboId=GlesUtil.createFrameBuffer();
        GlesUtil.bindFrameTexture(fboId,fboTextureId);

    }
    protected String getVertexSource() {
        final String source = "attribute vec4 av_Position; " +
                "attribute vec2 af_Position; " +
                "varying vec2 v_texPo; " +
                "uniform mat4 u_Matrix;"+
                "void main() { " +
                "    v_texPo = af_Position; " +
                "    gl_Position = av_Position*u_Matrix; " +
                "}";
        return source;
    }

    protected String getFragmentSource() {
        final String source = "#extension GL_OES_EGL_image_external : require \n" +
                "precision mediump float; " +
                "varying vec2 v_texPo; " +
                "uniform samplerExternalOES s_Texture; " +
                "void main() { " +
                "   gl_FragColor = texture2D(s_Texture, v_texPo); " +
                "} ";
        return source;
    }
    public void startRecord(){
        recordRenderDrawer.startRecord();
    }

    public void stopRecord() {
        recordRenderDrawer.stopRecord();
    }
}
