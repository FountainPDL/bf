package com.fountainpdl.blockfront;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.Arrays;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renderer: first-person / third-person view toggle with an animated voxel
 * player character, look/move/jump, two weapons (pistol/rifle) with a
 * visible first-person viewmodel and recoil, and an optional Demolition
 * mode layered on top of free-roam Sandbox.
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
    private static final float TPP_DISTANCE = 6f;
    private static final float TPP_PIVOT_HEIGHT = 1.8f;
    private static final float TPP_VERTICAL_OFFSET = 1.5f;

    private static final float GRAVITY = -0.02f;
    private static final float JUMP_VELOCITY = 0.3f;

    private static final int WEAPON_PISTOL = 0;
    private static final int WEAPON_RIFLE = 1;
    private static final int[] WEAPON_MAG_SIZE = {12, 30};
    private static final long[] WEAPON_RELOAD_MS = {900, 1500};
    private static final String[] WEAPON_NAMES = {"PISTOL", "RIFLE"};
    private static final long RECOIL_DURATION_MS = 160;

    private static final long HIT_FLASH_MS = 150;

    private static final int DEMOLITION_TARGET_COUNT = 15;
    private static final long DEMOLITION_DURATION_MS = 60000;

    private static final float[] GROUND_COLOR = {0.18f, 0.45f, 0.22f, 1f};
    private static final float[] TARGET_COLOR = {0.85f, 0.16f, 0.1f, 1f};
    private static final float[] SKIN_COLOR = {0.85f, 0.7f, 0.55f, 1f};
    private static final float[] SHIRT_COLOR = {0.83f, 0.69f, 0.22f, 1f};
    private static final float[] PANTS_COLOR = {0.12f, 0.12f, 0.14f, 1f};
    private static final float[] METAL_DARK = {0.16f, 0.16f, 0.18f, 1f};
    private static final float[] METAL_MID = {0.32f, 0.32f, 0.35f, 1f};
    private static final float[] GRIP_COLOR = {0.08f, 0.08f, 0.08f, 1f};

    private static final float LEG_HIP_Y = 0.9f;
    private static final float LEG_SCALE_LEN = 0.5f;
    private static final float LEG_FOOT_OFFSET = 0.85f;
    private static final float TORSO_OY = 0.9f;
    private static final float ARM_SHOULDER_Y = 1.7f;
    private static final float ARM_SCALE_LEN = 0.44f;
    private static final float ARM_HAND_OFFSET = 0.75f;
    private static final float HEAD_RADIUS = 0.28f;
    private static final float MAX_LEG_SWING_DEG = 35f;
    private static final float MAX_ARM_SWING_DEG = 30f;
    private static final float WALK_CYCLE_RATE = 0.25f;
    private static final float IDLE_BOB_AMOUNT = 0.04f;
    private static final float IDLE_BOB_RATE = 0.05f;

    private int program;
    private VoxelCube cube;
    private VoxelSphere sphere;

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] vpMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] characterMatrix = new float[16];
    private final float[] partMatrix = new float[16];
    private final float[] viewmodelMatrix = new float[16];
    private final float[] viewmodelPartMatrix = new float[16];
    private final float[] viewmodelMvp = new float[16];

    private float playerX = 0f, playerZ = 10f;
    private float playerY = 0f;
    private float verticalVelocity = 0f;
    private boolean grounded = true;

    private volatile float yaw = 0f;
    private volatile float pitch = 0f;

    private volatile float moveDx = 0f, moveDz = 0f;
    private volatile boolean thirdPerson = false;
    private volatile boolean jumpRequested = false;

    private int currentWeapon = WEAPON_RIFLE;
    private final int[] ammoPerWeapon = {WEAPON_MAG_SIZE[WEAPON_PISTOL], WEAPON_MAG_SIZE[WEAPON_RIFLE]};
    private final boolean[] reloadingPerWeapon = new boolean[2];
    private volatile long lastFireUptimeMs = 0;
    private volatile long lastHitUptimeMs = 0;

    private float walkCyclePhase = 0f;
    private float animTime = 0f;

    private final float lookSensitivityMultiplier;

    private final boolean demolitionMode;
    private boolean[][] targetBlocks;
    private volatile int targetsRemaining = 0;
    private long demolitionStartUptimeMs = 0;
    private volatile boolean gameWon = false;
    private volatile boolean gameLost = false;
    private volatile long completionTimeMs = 0;

    private final boolean[][] blockExists = new boolean[GRID_SIZE * 2][GRID_SIZE * 2];

    public GameRenderer(Context context, String mode) {
        for (boolean[] row : blockExists) {
            Arrays.fill(row, true);
        }

        SharedPreferences prefs = context.getSharedPreferences(
                SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        int sensitivityPercent = prefs.getInt(
                SettingsActivity.KEY_SENSITIVITY_PERCENT, SettingsActivity.DEFAULT_SENSITIVITY_PERCENT);
        this.lookSensitivityMultiplier = SettingsActivity.percentToMultiplier(sensitivityPercent);

        this.demolitionMode = "demolition".equals(mode);
        if (demolitionMode) {
            initDemolitionTargets();
        }
    }

    private void initDemolitionTargets() {
        targetBlocks = new boolean[GRID_SIZE * 2][GRID_SIZE * 2];
        Random rnd = new Random();
        int placed = 0;
        while (placed < DEMOLITION_TARGET_COUNT) {
            int x = rnd.nextInt(GRID_SIZE * 2);
            int z = rnd.nextInt(GRID_SIZE * 2);
            if (!targetBlocks[x][z]) {
                targetBlocks[x][z] = true;
                placed++;
            }
        }
        targetsRemaining = DEMOLITION_TARGET_COUNT;
        demolitionStartUptimeMs = SystemClock.uptimeMillis();
    }

    public void setMoveInput(float dx, float dy) {
        this.moveDx = dx;
        this.moveDz = dy;
    }

    /** Called from the UI thread by the look-drag surface. */
    public void addLookDelta(float dxPixels, float dyPixels) {
        float s = LOOK_SENSITIVITY * lookSensitivityMultiplier;
        yaw += dxPixels * s;
        pitch -= dyPixels * s;
        if (pitch > PITCH_LIMIT) pitch = PITCH_LIMIT;
        if (pitch < -PITCH_LIMIT) pitch = -PITCH_LIMIT;
    }

    /** Call via glSurfaceView.queueEvent(). */
    public void toggleViewMode() {
        thirdPerson = !thirdPerson;
    }

    /** Safe to call from the UI thread directly — just flips a flag the GL thread reads. */
    public void tryJump() {
        jumpRequested = true;
    }

    /** Call via glSurfaceView.queueEvent(). */
    public void switchWeapon() {
        currentWeapon = (currentWeapon + 1) % 2;
    }

    /** Call via glSurfaceView.queueEvent(). */
    public void manualReload() {
        int w = currentWeapon;
        if (!reloadingPerWeapon[w] && ammoPerWeapon[w] < WEAPON_MAG_SIZE[w]) {
            startReload(w);
        }
    }

    public String getWeaponName() {
        return WEAPON_NAMES[currentWeapon];
    }

    public int getCurrentAmmo() {
        return ammoPerWeapon[currentWeapon];
    }

    public int getMagSize() {
        return WEAPON_MAG_SIZE[currentWeapon];
    }

    public boolean isReloading() {
        return reloadingPerWeapon[currentWeapon];
    }

    public boolean isRecentHit() {
        return SystemClock.uptimeMillis() - lastHitUptimeMs < HIT_FLASH_MS;
    }

    public boolean isDemolitionMode() {
        return demolitionMode;
    }

    public int getTargetsRemaining() {
        return targetsRemaining;
    }

    public long getTimeRemainingMs() {
        if (!demolitionMode) return 0;
        long elapsed = SystemClock.uptimeMillis() - demolitionStartUptimeMs;
        return Math.max(0, DEMOLITION_DURATION_MS - elapsed);
    }

    public boolean isGameWon() {
        return gameWon;
    }

    public boolean isGameLost() {
        return gameLost;
    }

    public long getCompletionTimeMs() {
        return completionTimeMs;
    }

    /** Call via glSurfaceView.queueEvent(). */
    public void tryShoot() {
        int w = currentWeapon;
        if (reloadingPerWeapon[w]) return;
        if (ammoPerWeapon[w] <= 0) {
            startReload(w);
            return;
        }

        ammoPerWeapon[w]--;
        lastFireUptimeMs = SystemClock.uptimeMillis();
        if (ammoPerWeapon[w] == 0) {
            startReload(w);
        }

        float cosPitch = (float) Math.cos(pitch);
        float dirX = (float) Math.sin(yaw) * cosPitch;
        float dirY = (float) Math.sin(pitch);
        float dirZ = (float) -Math.cos(yaw) * cosPitch;

        float originX = playerX;
        float originY = EYE_HEIGHT + playerY;
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
                lastHitUptimeMs = SystemClock.uptimeMillis();

                if (demolitionMode && !gameWon && !gameLost
                        && targetBlocks != null && targetBlocks[ix][iz]) {
                    targetBlocks[ix][iz] = false;
                    targetsRemaining--;
                    if (targetsRemaining <= 0) {
                        gameWon = true;
                        completionTimeMs = SystemClock.uptimeMillis() - demolitionStartUptimeMs;
                    }
                }
                return;
            }
        }
    }

    private void startReload(int w) {
        reloadingPerWeapon[w] = true;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ammoPerWeapon[w] = WEAPON_MAG_SIZE[w];
            reloadingPerWeapon[w] = false;
        }, WEAPON_RELOAD_MS[w]);
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
        sphere = new VoxelSphere();
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

        animTime += 1f;

        if (demolitionMode && !gameWon && !gameLost) {
            if (getTimeRemainingMs() <= 0) {
                gameLost = true;
            }
        }

        if (jumpRequested) {
            jumpRequested = false;
            if (grounded) {
                verticalVelocity = JUMP_VELOCITY;
                grounded = false;
            }
        }
        verticalVelocity += GRAVITY;
        playerY += verticalVelocity;
        if (playerY <= 0f) {
            playerY = 0f;
            verticalVelocity = 0f;
            grounded = true;
        }

        float yawNow = yaw;
        float pitchNow = pitch;

        float moveForwardX = (float) Math.sin(yawNow);
        float moveForwardZ = (float) -Math.cos(yawNow);
        float moveRightX = (float) Math.cos(yawNow);
        float moveRightZ = (float) Math.sin(yawNow);

        float forwardAmount = -moveDz;
        float strafeAmount = moveDx;
        float moveMagnitude = Math.min(1f, (float) Math.sqrt(
                forwardAmount * forwardAmount + strafeAmount * strafeAmount));

        playerX += (moveForwardX * forwardAmount + moveRightX * strafeAmount) * MOVE_SPEED;
        playerZ += (moveForwardZ * forwardAmount + moveRightZ * strafeAmount) * MOVE_SPEED;

        float cosPitch = (float) Math.cos(pitchNow);
        float lookDirX = (float) Math.sin(yawNow) * cosPitch;
        float lookDirY = (float) Math.sin(pitchNow);
        float lookDirZ = (float) -Math.cos(yawNow) * cosPitch;

        boolean tpp = thirdPerson;

        if (tpp) {
            float pivotX = playerX;
            float pivotY = playerY + TPP_PIVOT_HEIGHT;
            float pivotZ = playerZ;

            float eyeX = pivotX - lookDirX * TPP_DISTANCE;
            float eyeY = pivotY - lookDirY * TPP_DISTANCE + TPP_VERTICAL_OFFSET;
            float eyeZ = pivotZ - lookDirZ * TPP_DISTANCE;

            Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, pivotX, pivotY, pivotZ, 0f, 1f, 0f);
        } else {
            float eyeX = playerX;
            float eyeY = EYE_HEIGHT + playerY;
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
                int ix = x + GRID_SIZE;
                int iz = z + GRID_SIZE;
                if (!blockExists[ix][iz]) continue;
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, x * 2f, 0f, z * 2f);
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);
                boolean isTarget = demolitionMode && targetBlocks != null && targetBlocks[ix][iz];
                cube.draw(program, mvpMatrix, isTarget ? TARGET_COLOR : GROUND_COLOR);
            }
        }

        if (tpp) {
            drawCharacter(yawNow, moveMagnitude);
        } else {
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
            drawViewmodel();
        }
    }

    private void drawCharacter(float facingYaw, float moveMagnitude) {
        float bob = (float) Math.sin(animTime * IDLE_BOB_RATE) * IDLE_BOB_AMOUNT;

        Matrix.setIdentityM(characterMatrix, 0);
        Matrix.translateM(characterMatrix, 0, playerX, playerY + bob, playerZ);
        Matrix.rotateM(characterMatrix, 0, (float) Math.toDegrees(facingYaw), 0f, 1f, 0f);

        walkCyclePhase += moveMagnitude * WALK_CYCLE_RATE;
        float swing = (float) Math.sin(walkCyclePhase) * moveMagnitude;
        float legSwingDeg = swing * MAX_LEG_SWING_DEG;
        float armSwingDeg = swing * MAX_ARM_SWING_DEG;

        drawSwingingPart(0.18f, LEG_HIP_Y, 0f, legSwingDeg, 0.15f, LEG_SCALE_LEN, 0.15f, PANTS_COLOR);
        drawSwingingPart(-0.18f, LEG_HIP_Y, 0f, -legSwingDeg, 0.15f, LEG_SCALE_LEN, 0.15f, PANTS_COLOR);
        drawSwingingSpherePart(0.18f, LEG_HIP_Y, 0f, legSwingDeg, LEG_FOOT_OFFSET, 0.16f, PANTS_COLOR);
        drawSwingingSpherePart(-0.18f, LEG_HIP_Y, 0f, -legSwingDeg, LEG_FOOT_OFFSET, 0.16f, PANTS_COLOR);

        drawPart(0f, TORSO_OY, 0f, 0.4f, 0.55f, 0.25f, SHIRT_COLOR);

        drawSwingingPart(0.5f, ARM_SHOULDER_Y, 0f, -armSwingDeg, 0.12f, ARM_SCALE_LEN, 0.12f, SHIRT_COLOR);
        drawSwingingPart(-0.5f, ARM_SHOULDER_Y, 0f, armSwingDeg, 0.12f, ARM_SCALE_LEN, 0.12f, SHIRT_COLOR);
        drawSwingingSpherePart(0.5f, ARM_SHOULDER_Y, 0f, -armSwingDeg, ARM_HAND_OFFSET, 0.14f, SKIN_COLOR);
        drawSwingingSpherePart(-0.5f, ARM_SHOULDER_Y, 0f, armSwingDeg, ARM_HAND_OFFSET, 0.14f, SKIN_COLOR);

        drawSpherePart(0f, ARM_SHOULDER_Y + HEAD_RADIUS + 0.08f, 0f, HEAD_RADIUS, SKIN_COLOR);
    }

    /**
     * Screen-anchored first-person weapon model. Uses an identity "view"
     * (not the real camera) so it sits fixed in the corner of the screen
     * regardless of look direction — standard FPS viewmodel technique.
     * The caller already cleared the depth buffer so this always draws
     * on top of the world.
     */
    private void drawViewmodel() {
        long sinceFire = SystemClock.uptimeMillis() - lastFireUptimeMs;
        float recoilT = sinceFire < RECOIL_DURATION_MS ? 1f - (sinceFire / (float) RECOIL_DURATION_MS) : 0f;
        float recoilKickZ = recoilT * 0.12f;
        float recoilLiftY = recoilT * 0.05f;

        float bobX = (float) Math.sin(animTime * IDLE_BOB_RATE * 0.6f) * 0.01f;
        float bobY = (float) Math.cos(animTime * IDLE_BOB_RATE * 0.6f) * 0.008f;

        Matrix.setIdentityM(viewmodelMatrix, 0);
        Matrix.translateM(viewmodelMatrix, 0,
                0.40f + bobX,
                -0.32f + bobY + recoilLiftY,
                -0.85f + recoilKickZ);
        Matrix.rotateM(viewmodelMatrix, 0, -6f, 0f, 1f, 0f);
        Matrix.rotateM(viewmodelMatrix, 0, 4f, 1f, 0f, 0f);

        if (currentWeapon == WEAPON_PISTOL) {
            drawViewmodelPart(0f, 0f, 0.05f, 0.045f, 0.045f, 0.16f, METAL_MID);
            drawViewmodelPart(0f, -0.08f, -0.04f, 0.035f, 0.09f, 0.045f, GRIP_COLOR);
        } else {
            drawViewmodelPart(0f, 0.02f, 0.18f, 0.03f, 0.03f, 0.30f, METAL_DARK);
            drawViewmodelPart(0f, 0.0f, -0.02f, 0.06f, 0.07f, 0.20f, METAL_MID);
            drawViewmodelPart(0f, -0.01f, -0.20f, 0.045f, 0.05f, 0.14f, METAL_DARK);
            drawViewmodelPart(0f, -0.14f, 0.02f, 0.03f, 0.13f, 0.045f, METAL_DARK);
            drawViewmodelPart(0f, -0.08f, -0.08f, 0.03f, 0.08f, 0.04f, GRIP_COLOR);
        }
    }

    private void drawViewmodelPart(float ox, float oy, float oz, float sx, float sy, float sz, float[] color) {
        System.arraycopy(viewmodelMatrix, 0, viewmodelPartMatrix, 0, 16);
        Matrix.translateM(viewmodelPartMatrix, 0, ox, oy, oz);
        Matrix.scaleM(viewmodelPartMatrix, 0, sx, sy, sz);
        Matrix.multiplyMM(viewmodelMvp, 0, projectionMatrix, 0, viewmodelPartMatrix, 0);
        cube.draw(program, viewmodelMvp, color);
    }

    private void drawPart(float ox, float oy, float oz, float sx, float sy, float sz, float[] color) {
        System.arraycopy(characterMatrix, 0, partMatrix, 0, 16);
        Matrix.translateM(partMatrix, 0, ox, oy, oz);
        Matrix.scaleM(partMatrix, 0, sx, sy, sz);
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, partMatrix, 0);
        cube.draw(program, mvpMatrix, color);
    }

    private void drawSpherePart(float ox, float oy, float oz, float radius, float[] color) {
        System.arraycopy(characterMatrix, 0, partMatrix, 0, 16);
        Matrix.translateM(partMatrix, 0, ox, oy, oz);
        Matrix.scaleM(partMatrix, 0, radius, radius, radius);
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, partMatrix, 0);
        sphere.draw(program, mvpMatrix, color);
    }

    private void drawSwingingPart(float ox, float pivotY, float oz, float swingDegrees,
                                   float sx, float sy, float sz, float[] color) {
        System.arraycopy(characterMatrix, 0, partMatrix, 0, 16);
        Matrix.translateM(partMatrix, 0, ox, pivotY, oz);
        Matrix.rotateM(partMatrix, 0, swingDegrees, 1f, 0f, 0f);
        Matrix.scaleM(partMatrix, 0, sx, -sy, sz);
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, partMatrix, 0);
        cube.draw(program, mvpMatrix, color);
    }

    private void drawSwingingSpherePart(float ox, float pivotY, float oz, float swingDegrees,
                                         float tipOffsetY, float radius, float[] color) {
        System.arraycopy(characterMatrix, 0, partMatrix, 0, 16);
        Matrix.translateM(partMatrix, 0, ox, pivotY, oz);
        Matrix.rotateM(partMatrix, 0, swingDegrees, 1f, 0f, 0f);
        Matrix.translateM(partMatrix, 0, 0f, -tipOffsetY, 0f);
        Matrix.scaleM(partMatrix, 0, radius, radius, radius);
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, partMatrix, 0);
        sphere.draw(program, mvpMatrix, color);
    }

    private int compileShader(int type, String src) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, src);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
