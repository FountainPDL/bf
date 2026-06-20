package com.fountainpdl.blockfront;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/** A reusable unit cube (1.8 world units per side, base resting at y=0). */
public class VoxelCube {

    public static final float SIZE = 0.9f;

    private static final float[] VERTICES = {
            -SIZE, 0,        -SIZE,   SIZE, 0,        -SIZE,   SIZE, 2 * SIZE, -SIZE,  -SIZE, 2 * SIZE, -SIZE,
            -SIZE, 0,         SIZE,   SIZE, 0,         SIZE,   SIZE, 2 * SIZE,  SIZE,  -SIZE, 2 * SIZE,  SIZE,
    };

    private static final short[] INDICES = {
            0, 1, 2, 0, 2, 3,
            4, 6, 5, 4, 7, 6,
            0, 4, 5, 0, 5, 1,
            3, 2, 6, 3, 6, 7,
            1, 5, 6, 1, 6, 2,
            0, 3, 7, 0, 7, 4
    };

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer indexBuffer;

    public VoxelCube() {
        ByteBuffer vbb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(VERTICES).position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(INDICES.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(INDICES).position(0);
    }

    public void draw(int program, float[] mvpMatrix, float[] color) {
        int posHandle = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        int colorHandle = GLES20.glGetAttribLocation(program, "vColor");
        GLES20.glDisableVertexAttribArray(colorHandle);
        GLES20.glVertexAttrib4fv(colorHandle, color, 0);

        int mvpHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, INDICES.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(posHandle);
    }
}
