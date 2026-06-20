package com.fountainpdl.blockfront;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Phase 3 renderer: first-person / third-person view toggle with a simple
 * blocky voxel player character, on top of Phase 2's look/move/shoot.
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
    private static final float MOVE_SPEED = 0.15f;
    private static final float LOOK_SENSITIVITY = 0.0025f;
    private static final float PITCH_LIMIT = (float) Math.toRadians(85);
    private static final float SHOOT_STEP = 0.1f;
    private static final float SHOOT_MAX_DIST = 40f;

    private static final float EYE_HEIGHT = 4f;
    private static final float PLAYER_FEET_Y = 0f;
    private static final float TPP_DISTANCE = 6f;
    private static final float TPP_PIVOT_HEIGHT = 1.8f;
    private static final float TPP_VERTICAL_OFFSET = 1.5f;

    private static final float[] GROUND_COLOR = {0.18f, 0.45f, 0.22f, 1f};
    private static final float[] SKIN_COLOR = {0.85f, 0.7f, 0.55f, 1f};
    private static final float[] SHIRT_COLOR = {0.83f, 0.69f, 0.22f, 1f};
    private static final float[] PANTS_COLOR = {0.12f, 0.12f, 0.14f, 1f};

    private int program;
    private VoxelCube cube;

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] vpMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] characterMatrix = new float[16];
    private final float[] partMatrix = new float[16];

    private float playerX = 0f, playerZ = 10f;
    private volatile float yaw = 0f;
    private volatile float pitch = 0f;

    private volatile float moveDx = 0f, moveDz = 0f;
    private volatile boolean thirdPerson = false;

    private final boolean[][] blockExists = new boolean[GRID_SIZE * 2][GRID_SIZE * 2];

    public GameRenderer(Context context) {
        for (boolean[] row : blockExists) {
            Arrays.fill(row, true);
        }
    }

    public void setMoveInput(float dx, float dy) {
        this.moveDx = dx;
        this.moveDz = dy;
    }

    /** Called from the UI thread by the look-drag surface. */
    public void addLookDelta(float dxPixels, float dyPixels) {
        yaw += dxPixels * LOOK_SENSITIVITY;
        pitch -= dyPixels * LOOK_SENSITIVITY;
        if (pitch > PITCH_LIMIT) pitch = PITCH_LIMIT;
        if (pitch < -PITCH_LIMIT) pitch = -PITCH_LIMIT;
    }

    /** Call via glSurfaceView.queueEvent(). */
    public void toggleViewMode() {
        thirdPerson = !thirdPerson;
    }

    /** Call via glSurfaceView.queueEvent(). */
    public void tryShoot() {
        float cosPitch = (float) Math.cos(pitch);
        float dirX = (float) Math.sin(yaw) * cosPitch;
        float dirY = (float) Math.sin(pitch);
        float dirZ = (float) -Math.cos(yaw) * cosPitch;

        float originX = playerX;
        float originY = EYE_HEIGHT;
        float originZ = playerZ;

        for (float t = 0.5f; t < SHOOT_MAX_DIST; t += SHOOT_STEP) {
            float px = originX + dirX * t;
            float py = originY + dirY * t;
            float pz = originZ + dirZ * t;

            if (py < 0f || py > 2f * VoxelCube.SIZE) continue;

            int gx = Math.round(px / 2f);
            int gz = Math.round(pz / 2f);
            if (gx < -GRID_SIZE || gx >= GRID_SIZE || gz < -GRID_SIZE || gz >= GRID_SIZE) continue;

            int ix = gx + GRID_SIZE;
            int iz = gz + GRID_SIZE;
            if (!blockExists[ix][iz]) continue;

            float localX = px - gx * 2f;
            float localZ = pz - gz * 2f;
            if (Math.abs(localX) < VoxelCube.SIZE && Math.abs(localZ) < VoxelCube.SIZE) {
                blockExists[ix][iz] = false;
                return;
            }
        }
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

        float yawNow = yaw;
        float pitchNow = pitch;

        float moveForwardX = (float) Math.sin(yawNow);
        float moveForwardZ = (float) -Math.cos(yawNow);
        float moveRightX = (float) Math.cos(yawNow);
        float moveRightZ = (float) Math.sin(yawNow);

        float forwardAmount = -moveDz;
        float strafeAmount = moveDx;

        playerX += (moveForwardX * forwardAmount + moveRightX * strafeAmount) * MOVE_SPEED;
        playerZ += (moveForwardZ * forwardAmount + moveRightZ * strafeAmount) * MOVE_SPEED;

        float cosPitch = (float) Math.cos(pitchNow);
        float lookDirX = (float) Math.sin(yawNow) * cosPitch;
        float lookDirY = (float) Math.sin(pitchNow);
        float lookDirZ = (float) -Math.cos(yawNow) * cosPitch;

        boolean tpp = thirdPerson;

        if (tpp) {
            float pivotX = playerX;
            float pivotY = PLAYER_FEET_Y + TPP_PIVOT_HEIGHT;
            float pivotZ = playerZ;

            float eyeX = pivotX - lookDirX * TPP_DISTANCE;
            float eyeY = pivotY - lookDirY * TPP_DISTANCE + TPP_VERTICAL_OFFSET;
            float eyeZ = pivotZ - lookDirZ * TPP_DISTANCE;

            Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, pivotX, pivotY, pivotZ, 0f, 1f, 0f);
        } else {
            float eyeX = playerX;
            float eyeY = EYE_HEIGHT;
            float eyeZ = playerZ;

            Matrix.setLookAtM(viewMatrix, 0,
                    eyeX, eyeY, eyeZ,
                    eyeX + lookDirX, eyeY + lookDirY, eyeZ + lookDirZ,
                    0f, 1f, 0f);
        }

        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        GLES20.glUseProgram(program);

        for (int x = -GRID_SIZE; x < GRID_SIZE; x++) {
            for (int z = -GRID_SIZE; z < GRID_SIZE; z++) {
                if (!blockExists[x + GRID_SIZE][z + GRID_SIZE]) continue;
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, x * 2f, 0f, z * 2f);
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);
                cube.draw(program, mvpMatrix, GROUND_COLOR);
            }
        }

        if (tpp) {
            drawCharacter(yawNow);
        }
    }

    private void drawCharacter(float facingYaw) {
        Matrix.setIdentityM(characterMatrix, 0);
        Matrix.translateM(characterMatrix, 0, playerX, PLAYER_FEET_Y, playerZ);
        Matrix.rotateM(characterMatrix, 0, (float) Math.toDegrees(facingYaw), 0f, 1f, 0f);

        drawPart(0.18f, 0f, 0f, 0.15f, 0.5f, 0.15f, PANTS_COLOR);
        drawPart(-0.18f, 0f, 0f, 0.15f, 0.5f, 0.15f, PANTS_COLOR);
        drawPart(0f, 0.9f, 0f, 0.4f, 0.55f, 0.25f, SHIRT_COLOR);
        drawPart(0.5f, 0.9f, 0f, 0.12f, 0.5f, 0.12f, SHIRT_COLOR);
        drawPart(-0.5f, 0.9f, 0f, 0.12f, 0.5f, 0.12f, SHIRT_COLOR);
        drawPart(0f, 1.89f, 0f, 0.3f, 0.3f, 0.3f, SKIN_COLOR);
    }

    private void drawPart(float ox, float oy, float oz, float sx, float sy, float sz, float[] color) {
        System.arraycopy(characterMatrix, 0, partMatrix, 0, 16);
        Matrix.translateM(partMatrix, 0, ox, oy, oz);
        Matrix.scaleM(partMatrix, 0, sx, sy, sz);
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, partMatrix, 0);
        cube.draw(program, mvpMatrix, color);
    }

    private int compileShader(int type, String src) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, src);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
