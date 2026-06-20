package com.fountainpdl.blockfront;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

/** A reusable unit UV-sphere (radius 1, centered at local origin). */
public class VoxelSphere {

    private static final int LAT_SEGMENTS = 8;
    private static final int LON_SEGMENTS = 12;

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    public VoxelSphere() {
        int vertCount = (LAT_SEGMENTS + 1) * (LON_SEGMENTS + 1);
        float[] vertices = new float[vertCount * 3];

        int vi = 0;
        for (int lat = 0; lat <= LAT_SEGMENTS; lat++) {
            float theta = (float) (Math.PI * lat / LAT_SEGMENTS);
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);

            for (int lon = 0; lon <= LON_SEGMENTS; lon++) {
                float phi = (float) (2 * Math.PI * lon / LON_SEGMENTS);
                float sinPhi = (float) Math.sin(phi);
                float cosPhi = (float) Math.cos(phi);

                vertices[vi++] = cosPhi * sinTheta;
                vertices[vi++] = cosTheta;
                vertices[vi++] = sinPhi * sinTheta;
            }
        }

        ArrayList<Short> indices = new ArrayList<>();
        for (int lat = 0; lat < LAT_SEGMENTS; lat++) {
            for (int lon = 0; lon < LON_SEGMENTS; lon++) {
                int first = lat * (LON_SEGMENTS + 1) + lon;
                int second = first + LON_SEGMENTS + 1;

                indices.add((short) first);
                indices.add((short) second);
                indices.add((short) (first + 1));

                indices.add((short) second);
                indices.add((short) (second + 1));
                indices.add((short) (first + 1));
            }
        }

        indexCount = indices.size();
        short[] indexArray = new short[indexCount];
        for (int i = 0; i < indexCount; i++) indexArray[i] = indices.get(i);

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(indexArray.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(indexArray).position(0);
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

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(posHandle);
    }
}
