package com.camerarecorderdemo.render;

import android.content.Context;
import android.opengl.GLES20;

import com.camerarecorderdemo.util.GlesUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class ScreenRender {

        private String TAG="ScreenRender";
        private int program;
        private int av_Position;
        private int af_Position;
        private int s_Texture;

    public void setInputTexture(int inputTexture) {
        this.inputTexture = inputTexture;
    }

    private int inputTexture;
        public ScreenRender(Context context) {
            this.context = context;
        }
        private Context context;
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

        public void onSurfaceCreated() {
            initBuffer();
            createOpenGlProgram();
        }





        public void onSurfaceChanged( int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }


        public void onDrawFrame() {
            clear();
            useProgram();
            GLES20.glEnableVertexAttribArray(av_Position);
            GLES20.glEnableVertexAttribArray(af_Position);
            GLES20.glVertexAttribPointer(av_Position, 2, GLES20.GL_FLOAT, false, 8, vertextBuffer);
            GLES20.glVertexAttribPointer(af_Position, 2, GLES20.GL_FLOAT, false, 8, fragBuffer);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture);
            GLES20.glUniform1i(s_Texture, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glDisableVertexAttribArray(af_Position);
            GLES20.glDisableVertexAttribArray(av_Position);

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
                "   float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
                "   gl_FragColor = texture2D(s_Texture, v_texPo);\n" +
                //"    gl_FragColor = vec4(color, color, color, 1);\n" +
                "}";
        return source;
    }
    }


