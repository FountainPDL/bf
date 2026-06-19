package com.fountainpdl.blockfront;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Phase 1 renderer: proves the OpenGL ES pipeline end-to-end.
 * Renders a flat grid of voxel blocks with a joystick-driven flying camera.
 * This is the foundation Phase 2 (chunked world, collision, shooting) builds on.
 */
public class GameRenderer implements GLSurfaceView.Renderer {

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "attribute vec4 vColor;" +
            "varying vec4 fColor;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "  fColor = vColor;" +
            "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
            "varying vec4 fColor;" +
            "void main() {" +
            "  gl_FragColor = fColor;" +
            "}";

    private static final int GRID_SIZE = 12;

    private int program;
    private VoxelCube cube;

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] vpMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    private float camX = 0f, camY = 4f, camZ = 10f;

    private volatile float moveDx = 0f, moveDz = 0f;

    public GameRenderer(Context context) {
        // context reserved for Phase 2 (texture/asset loading)
    }

    public void setMoveInput(float dx, float dy) {
        this.moveDx = dx;
        this.moveDz = dy;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.04f, 0.05f, 0.08f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        cube = new VoxelCube();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float aspect = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 60f, aspect, 0.5f, 200f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        camX += moveDx * 0.15f;
        camZ += moveDz * 0.15f;

        Matrix.setLookAtM(viewMatrix, 0,
                camX, camY, camZ,
                camX, camY - 0.3f, camZ - 5f,
                0f, 1f, 0f);
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        GLES20.glUseProgram(program);

        for (int x = -GRID_SIZE; x < GRID_SIZE; x++) {
            for (int z = -GRID_SIZE; z < GRID_SIZE; z++) {
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, x * 2f, 0f, z * 2f);
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);
                cube.draw(program, mvpMatrix);
            }
        }
    }

    private int compileShader(int type, String src) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, src);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
