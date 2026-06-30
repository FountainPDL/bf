package com.fountainpdl.blockfront;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.io.IOException;
import java.io.InputStream;

/** Compiles and manages a UV + sampler2D shader program with an assets texture. */
public class TexturedShader {

    private static final String VERT =
            "uniform mat4 uMVP;" +
            "attribute vec3 aPos;" +
            "attribute vec2 aUV;" +
            "varying vec2 vUV;" +
            "void main(){gl_Position=uMVP*vec4(aPos,1.0);vUV=aUV;}";

    private static final String FRAG_TINTED =
            "precision mediump float;" +
            "uniform sampler2D uTex;" +
            "uniform vec4 uTint;" +
            "varying vec2 vUV;" +
            "void main(){vec4 c=texture2D(uTex,vUV);gl_FragColor=c*uTint;}";

    private final int program;
    private final int[] texId = new int[1];
    private boolean textureLoaded = false;

    public TexturedShader(Context ctx, String assetName) {
        program = buildProgram(VERT, FRAG_TINTED);
        GLES20.glGenTextures(1, texId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        try {
            InputStream is = ctx.getAssets().open(assetName);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            if (bmp != null) {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
                bmp.recycle();
                textureLoaded = true;
            }
        } catch (IOException ignored) {
            // Asset not present yet — renders white until the file is added
        }
    }

    public int getProgram() { return program; }

    public void bind(float[] tint) {
        GLES20.glUseProgram(program);
        if (textureLoaded) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId[0]);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTex"), 0);
        }
        GLES20.glUniform4fv(GLES20.glGetUniformLocation(program, "uTint"), 1, tint, 0);
    }

    private static int buildProgram(String vert, String frag) {
        int vs = compile(GLES20.GL_VERTEX_SHADER, vert);
        int fs = compile(GLES20.GL_FRAGMENT_SHADER, frag);
        int p  = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, vs);
        GLES20.glAttachShader(p, fs);
        GLES20.glLinkProgram(p);
        return p;
    }

    private static int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        return s;
    }
}
