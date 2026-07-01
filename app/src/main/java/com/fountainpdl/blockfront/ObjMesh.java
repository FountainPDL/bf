package com.fountainpdl.blockfront;

import android.content.Context;
import android.opengl.GLES20;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * OpenGL ES 2.0 mesh loaded from a compact binary asset file.
 * Binary format (big-endian):
 *   4 bytes — int   vertCount
 *   4 bytes — int   idxCount
 *   vertCount × 5 × 4 bytes — floats: x,y,z,u,v per vertex
 *   idxCount  × 2 bytes     — unsigned shorts (indices)
 */
public class ObjMesh {

    private static final int STRIDE = 5 * 4; // 5 floats × 4 bytes

    private final FloatBuffer vb;
    private final ShortBuffer ib;
    private final int indexCount;

    public ObjMesh(float[] vertices, short[] indices) {
        ByteBuffer bv = ByteBuffer.allocateDirect(vertices.length * 4);
        bv.order(ByteOrder.nativeOrder());
        vb = bv.asFloatBuffer();
        vb.put(vertices).position(0);

        ByteBuffer bi = ByteBuffer.allocateDirect(indices.length * 2);
        bi.order(ByteOrder.nativeOrder());
        ib = bi.asShortBuffer();
        ib.put(indices).position(0);

        indexCount = indices.length;
    }

    /**
     * Load a mesh from a binary asset produced by gen_mesh_data.py.
     * Throws IOException if the asset is missing or corrupt.
     */
    public static ObjMesh fromAsset(Context ctx, String assetName) throws IOException {
        InputStream raw = ctx.getAssets().open(assetName);
        DataInputStream dis = new DataInputStream(new BufferedInputStream(raw, 65536));

        int vertCount = dis.readInt();
        int idxCount  = dis.readInt();

        float[] verts = new float[vertCount * 5];
        for (int i = 0; i < verts.length; i++) {
            verts[i] = dis.readFloat();
        }

        short[] indices = new short[idxCount];
        for (int i = 0; i < idxCount; i++) {
            indices[i] = dis.readShort();
        }
        dis.close();

        return new ObjMesh(verts, indices);
    }

    /** Draw with a textured shader: aPos (vec3), aUV (vec2), uMVP (mat4), uTex (sampler2D). */
    public void drawTextured(int program, float[] mvp) {
        int aPos = GLES20.glGetAttribLocation(program, "aPos");
        int aUV  = GLES20.glGetAttribLocation(program, "aUV");
        int uMVP = GLES20.glGetUniformLocation(program, "uMVP");

        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glEnableVertexAttribArray(aUV);
        vb.position(0);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, STRIDE, vb);
        vb.position(3);
        GLES20.glVertexAttribPointer(aUV,  2, GLES20.GL_FLOAT, false, STRIDE, vb);
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, ib);
        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aUV);
    }

    /** Draw with the flat voxel shader: vPos (vec4), vCol (vec4), uMVP (mat4). */
    public void drawFlat(int program, float[] mvp, float[] color) {
        int aPos = GLES20.glGetAttribLocation(program,  "vPos");
        int aCol = GLES20.glGetAttribLocation(program,  "vCol");
        int uMVP = GLES20.glGetUniformLocation(program, "uMVP");

        GLES20.glEnableVertexAttribArray(aPos);
        vb.position(0);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, STRIDE, vb);
        GLES20.glDisableVertexAttribArray(aCol);
        GLES20.glVertexAttrib4fv(aCol, color, 0);
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, ib);
        GLES20.glDisableVertexAttribArray(aPos);
    }
}

