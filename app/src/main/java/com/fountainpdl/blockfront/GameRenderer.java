package com.fountainpdl.blockfront;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renderer: first-person / third-person view toggle with an animated voxel
 * player character, jetpack flight, two weapons with a visible viewmodel
 * (also held in TPP), simple AI bots, biome-patched maps, and three
 * objective modes — Demolition, Battle Royale (shrinking zone + bots),
 * and Free-For-All (timed bot deathmatch) — on top of free-roam Sandbox.
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

    private static final int GRID_SIZE_NORMAL = 12;
    private static final int GRID_SIZE_BIG = 20;
    private static final float FOV_DEGREES = 75f;
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
    private static final float INITIAL_HOP_VELOCITY = 0.16f;
    private static final float JETPACK_ACCEL = 0.032f;
    private static final float MAX_RISE_VELOCITY = 0.42f;
    private static final float MAX_FALL_VELOCITY = -0.6f;
    private static final float FUEL_MAX = 100f;
    private static final float FUEL_DRAIN_PER_FRAME = 1.2f;
    private static final float FUEL_REGEN_PER_FRAME = 0.5f;
    private static final float MIN_FUEL_FOR_HOP = 10f;

    private static final int WEAPON_PISTOL = 0;
    private static final int WEAPON_RIFLE = 1;
    private static final int[] WEAPON_MAG_SIZE = {12, 30};
    private static final long[] WEAPON_RELOAD_MS = {900, 1500};
    private static final String[] WEAPON_NAMES = {"PISTOL", "RIFLE"};
    private static final long RECOIL_DURATION_MS = 160;

    private static final long HIT_FLASH_MS = 150;

    private static final int DEMOLITION_TARGET_COUNT = 15;
    private static final long DEMOLITION_DURATION_MS = 60000;

    private static final long BR_DURATION_MS = 130000;
    private static final float BR_FINAL_RADIUS = 3f;
    private static final float BR_ZONE_DAMAGE_PER_SEC = 14f;
    private static final int BOT_COUNT_BR = 7;

    private static final long FFA_DURATION_MS = 120000;
    private static final int BOT_COUNT_FFA = 5;

    private static final float PLAYER_MAX_HEALTH = 100f;

    private static final float BOT_MAX_HEALTH = 100f;
    private static final float BOT_DETECT_RANGE = 14f;
    private static final float BOT_ENGAGE_RANGE = 9f;
    private static final float BOT_MOVE_SPEED = 0.09f;
    private static final long BOT_SHOT_INTERVAL_MS = 1400;
    private static final float BOT_HIT_CHANCE = 0.35f;
    private static final float BOT_DAMAGE = 9f;
    private static final float BOT_HIT_RADIUS = 0.4f;
    private static final float BOT_HEIGHT = 2.5f;
    private static final float BOT_WANDER_RADIUS = 10f;

    private static final int NUM_BIOME_SEEDS = 10;
    private static final float BIOME_PATCH_RADIUS = 16f;
    private static final float BIOME_BLEND = 0.5f;

    private static final float[][] MAP_GROUND_COLORS = {
            {0.18f, 0.45f, 0.22f, 1f}, // grasslands
            {0.76f, 0.66f, 0.42f, 1f}, // desert
            {0.86f, 0.88f, 0.92f, 1f}, // snow
    };
    private static final float[][] MAP_SKY_COLORS = {
            {0.45f, 0.65f, 0.85f, 1f}, // grasslands
            {0.85f, 0.66f, 0.36f, 1f}, // desert
            {0.75f, 0.82f, 0.90f, 1f}, // snow
    };
    private static final float[][] BIOME_TINTS = {
            {0.10f, 0.32f, 0.14f, 1f}, // forest
            {0.42f, 0.38f, 0.30f, 1f}, // rocky
            {0.22f, 0.50f, 0.55f, 1f}, // wetland
    };

    private static final float[] TARGET_COLOR = {0.85f, 0.16f, 0.1f, 1f};
    private static final float[] ZONE_DANGER_COLOR = {0.30f, 0.10f, 0.26f, 1f};
    private static final float[] SKIN_COLOR = {0.85f, 0.7f, 0.55f, 1f};
    private static final float[] SHIRT_COLOR = {0.83f, 0.69f, 0.22f, 1f};
    private static final float[] ENEMY_SHIRT_COLOR = {0.72f, 0.16f, 0.13f, 1f};
    private static final float[] PANTS_COLOR = {0.12f, 0.12f, 0.14f, 1f};
    private static final float[] METAL_DARK = {0.16f, 0.16f, 0.18f, 1f};
    private static final float[] METAL_MID = {0.32f, 0.32f, 0.35f, 1f};
    private static final float[] GRIP_COLOR = {0.08f, 0.08f, 0.08f, 1f};
    private static final float[] FLAME_COLOR = {1f, 0.55f, 0.12f, 1f};

    private static final float LEG_HIP_Y = 0.9f;
    private static final float LEG_SCALE_LEN = 0.5f;
    private static final float LEG_FOOT_OFFSET = 0.74f; // was 0.85 — that sank feet below y=0
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
    private final float[] weaponBaseMatrix = new float[16];
    private final float[] weaponTempMatrix = new float[16];
    private final float[] weaponMvpTemp = new float[16];

    private float playerX, playerZ;
    private float playerY = 0f;
    private float verticalVelocity = 0f;
    private boolean grounded = true;

    private volatile float yaw = 0f;
    private volatile float pitch = 0f;

    private volatile float moveDx = 0f, moveDz = 0f;
    private volatile boolean thirdPerson = false;
    private volatile boolean thrustHeld = false;
    private float fuel = FUEL_MAX;

    private int currentWeapon = WEAPON_RIFLE;
    private final int[] ammoPerWeapon = {WEAPON_MAG_SIZE[WEAPON_PISTOL], WEAPON_MAG_SIZE[WEAPON_RIFLE]};
    private final boolean[] reloadingPerWeapon = new boolean[2];
    private volatile long lastFireUptimeMs = 0;
    private volatile long lastHitUptimeMs = 0;

    private float walkCyclePhase = 0f;
    private float animTime = 0f;

    private final float lookSensitivityMultiplier;

    private final int gridSize;
    private final int mapIndex;
    private final boolean[][] blockExists;
    private final float[][][] cellBaseColor;

    private final boolean demolitionMode;
    private boolean[][] targetBlocks;
    private volatile int targetsRemaining = 0;
    private long demolitionStartUptimeMs = 0;
    private volatile boolean gameWon = false;
    private volatile boolean gameLost = false;
    private volatile long completionTimeMs = 0;

    private final boolean battleRoyaleMode;
    private final boolean ffaMode;
    private final boolean botsPresent;
    private final List<Bot> bots = new ArrayList<>();
    private final Random random = new Random();
    private volatile int killCount = 0;

    private final float brInitialRadius;
    private float currentZoneRadius;
    private long modeStartUptimeMs = 0;
    private volatile boolean brSurvived = false;
    private volatile boolean ffaTimeUp = false;
    private volatile boolean ffaAllEliminated = false;

    private float playerHealth = PLAYER_MAX_HEALTH;
    private volatile boolean playerEliminated = false;
    private volatile long survivalMs = 0;

    public GameRenderer(Context context, String mode, String mapId) {
        this.demolitionMode = "demolition".equals(mode);
        this.battleRoyaleMode = "battleroyale".equals(mode);
        this.ffaMode = "ffa".equals(mode);
        this.botsPresent = battleRoyaleMode || ffaMode;
        this.gridSize = botsPresent ? GRID_SIZE_BIG : GRID_SIZE_NORMAL;
        this.mapIndex = mapIndexFor(mapId);
        this.brInitialRadius = gridSize * 2f * 0.9f;
        this.currentZoneRadius = brInitialRadius;

        this.blockExists = new boolean[gridSize * 2][gridSize * 2];
        for (boolean[] row : blockExists) {
            Arrays.fill(row, true);
        }

        this.cellBaseColor = botsPresent ? buildBiomeColors() : null;

        SharedPreferences prefs = context.getSharedPreferences(
                SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        int sensitivityPercent = prefs.getInt(
                SettingsActivity.KEY_SENSITIVITY_PERCENT, SettingsActivity.DEFAULT_SENSITIVITY_PERCENT);
        this.lookSensitivityMultiplier = SettingsActivity.percentToMultiplier(sensitivityPercent);

        if (demolitionMode) {
            initDemolitionTargets();
            playerX = 0f;
            playerZ = 10f;
        } else if (botsPresent) {
            modeStartUptimeMs = SystemClock.uptimeMillis();
            float spawnSpan = (battleRoyaleMode ? brInitialRadius : gridSize * 2f) * 0.65f;
            float[] spawn = randomOffCenterPoint(spawnSpan);
            playerX = spawn[0];
            playerZ = spawn[1];
            int botCount = battleRoyaleMode ? BOT_COUNT_BR : BOT_COUNT_FFA;
            for (int i = 0; i < botCount; i++) {
                float[] p = randomOffCenterPoint(spawnSpan);
                bots.add(new Bot(p[0], p[1], BOT_MAX_HEALTH));
            }
        } else {
            playerX = 0f;
            playerZ = 10f;
        }
    }

    private float[] randomOffCenterPoint(float maxRadius) {
        float angle = random.nextFloat() * (float) (Math.PI * 2);
        float dist = maxRadius * (0.35f + random.nextFloat() * 0.65f);
        return new float[]{(float) Math.cos(angle) * dist, (float) Math.sin(angle) * dist};
    }

    private static int mapIndexFor(String mapId) {
        if ("desert".equals(mapId)) return 1;
        if ("snow".equals(mapId)) return 2;
        return 0;
    }

    /** Voronoi-style biome patches, precomputed once — no per-frame cost. */
    private float[][][] buildBiomeColors() {
        float[][][] colors = new float[gridSize * 2][gridSize * 2][];
        Random biomeRnd = new Random();
        float[] seedX = new float[NUM_BIOME_SEEDS];
        float[] seedZ = new float[NUM_BIOME_SEEDS];
        float[][] seedTint = new float[NUM_BIOME_SEEDS][];
        float extent = gridSize * 2f;
        for (int i = 0; i < NUM_BIOME_SEEDS; i++) {
            seedX[i] = (biomeRnd.nextFloat() * 2f - 1f) * extent;
            seedZ[i] = (biomeRnd.nextFloat() * 2f - 1f) * extent;
            seedTint[i] = BIOME_TINTS[biomeRnd.nextInt(BIOME_TINTS.length)];
        }
        float[] base = MAP_GROUND_COLORS[mapIndex];
        for (int x = -gridSize; x < gridSize; x++) {
            for (int z = -gridSize; z < gridSize; z++) {
                float wx = x * 2f, wz = z * 2f;
                int nearest = 0;
                float bestDist = Float.MAX_VALUE;
                for (int i = 0; i < NUM_BIOME_SEEDS; i++) {
                    float dx = wx - seedX[i], dz = wz - seedZ[i];
                    float d = dx * dx + dz * dz;
                    if (d < bestDist) {
                        bestDist = d;
                        nearest = i;
                    }
                }
                float distToSeed = (float) Math.sqrt(bestDist);
                float[] color;
                if (distToSeed > BIOME_PATCH_RADIUS) {
                    color = base;
                } else {
                    float[] tint = seedTint[nearest];
                    color = new float[]{
                            base[0] * (1 - BIOME_BLEND) + tint[0] * BIOME_BLEND,
                            base[1] * (1 - BIOME_BLEND) + tint[1] * BIOME_BLEND,
                            base[2] * (1 - BIOME_BLEND) + tint[2] * BIOME_BLEND,
                            1f
                    };
                }
                colors[x + gridSize][z + gridSize] = color;
            }
        }
        return colors;
    }

    private void initDemolitionTargets() {
        targetBlocks = new boolean[gridSize * 2][gridSize * 2];
        Random rnd = new Random();
        int placed = 0;
        while (placed < DEMOLITION_TARGET_COUNT) {
            int x = rnd.nextInt(gridSize * 2);
            int z = rnd.nextInt(gridSize * 2);
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

    public void addLookDelta(float dxPixels, float dyPixels) {
        float s = LOOK_SENSITIVITY * lookSensitivityMultiplier;
        yaw += dxPixels * s;
        pitch -= dyPixels * s;
        if (pitch > PITCH_LIMIT) pitch = PITCH_LIMIT;
        if (pitch < -PITCH_LIMIT) pitch = -PITCH_LIMIT;
    }

    public void toggleViewMode() {
        thirdPerson = !thirdPerson;
    }

    public void onThrustPress() {
        thrustHeld = true;
        if (grounded && fuel > MIN_FUEL_FOR_HOP) {
            verticalVelocity = INITIAL_HOP_VELOCITY;
            grounded = false;
        }
    }

    public void onThrustRelease() {
        thrustHeld = false;
    }

    public float getFuelPercent() {
        return fuel / FUEL_MAX;
    }

    public void switchWeapon() {
        currentWeapon = (currentWeapon + 1) % 2;
    }

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

    public boolean isBattleRoyaleMode() {
        return battleRoyaleMode;
    }

    public boolean isFfaMode() {
        return ffaMode;
    }

    public float getPlayerHealthPercent() {
        return Math.max(0f, playerHealth / PLAYER_MAX_HEALTH);
    }

    public boolean isPlayerEliminated() {
        return playerEliminated;
    }

    public boolean isBrSurvived() {
        return brSurvived;
    }

    public boolean isFfaTimeUp() {
        return ffaTimeUp;
    }

    public boolean isFfaAllEliminated() {
        return ffaAllEliminated;
    }

    public long getSurvivalMs() {
        return survivalMs;
    }

    public int getKillCount() {
        return killCount;
    }

    public int getBotsRemaining() {
        int n = 0;
        for (Bot b : bots) if (b.alive) n++;
        return n;
    }

    public long getModeTimeRemainingMs() {
        long total = battleRoyaleMode ? BR_DURATION_MS : (ffaMode ? FFA_DURATION_MS : 0);
        if (total == 0) return 0;
        long elapsed = SystemClock.uptimeMillis() - modeStartUptimeMs;
        return Math.max(0, total - elapsed);
    }

    public boolean isInDangerZone() {
        if (!battleRoyaleMode) return false;
        float dist = (float) Math.sqrt(playerX * playerX + playerZ * playerZ);
        return dist > currentZoneRadius;
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

            if (botsPresent) {
                for (Bot bot : bots) {
                    if (!bot.alive) continue;
                    float bdx = px - bot.x;
                    float bdz = pz - bot.z;
                    float bdist = (float) Math.sqrt(bdx * bdx + bdz * bdz);
                    if (bdist < BOT_HIT_RADIUS && py > 0f && py < BOT_HEIGHT) {
                        bot.alive = false;
                        killCount++;
                        lastHitUptimeMs = SystemClock.uptimeMillis();
                        return;
                    }
                }
            }

            if (py < 0f || py > 2f * VoxelCube.SIZE) continue;

            int gx = Math.round(px / 2f);
            int gz = Math.round(pz / 2f);
            if (gx < -gridSize || gx >= gridSize || gz < -gridSize || gz >= gridSize) continue;

            int ix = gx + gridSize;
            int iz = gz + gridSize;
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
        float[] sky = MAP_SKY_COLORS[mapIndex];
        GLES20.glClearColor(sky[0], sky[1], sky[2], sky[3]);
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
        Matrix.perspectiveM(projectionMatrix, 0, FOV_DEGREES, aspect, 0.5f, 260f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        animTime += 1f;

        if (demolitionMode && !gameWon && !gameLost && getTimeRemainingMs() <= 0) {
            gameLost = true;
        }

        boolean thrusting = thrustHeld && fuel > 0f;
        if (thrusting) {
            verticalVelocity += JETPACK_ACCEL;
            fuel = Math.max(0f, fuel - FUEL_DRAIN_PER_FRAME);
        } else {
            fuel = Math.min(FUEL_MAX, fuel + FUEL_REGEN_PER_FRAME);
        }
        verticalVelocity += GRAVITY;
        if (verticalVelocity > MAX_RISE_VELOCITY) verticalVelocity = MAX_RISE_VELOCITY;
        if (verticalVelocity < MAX_FALL_VELOCITY) verticalVelocity = MAX_FALL_VELOCITY;
        playerY += verticalVelocity;
        if (playerY <= 0f) {
            playerY = 0f;
            verticalVelocity = 0f;
            grounded = true;
        } else {
            grounded = false;
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

        if (battleRoyaleMode && !playerEliminated && !brSurvived) {
            long elapsed = SystemClock.uptimeMillis() - modeStartUptimeMs;
            float t = Math.min(1f, elapsed / (float) BR_DURATION_MS);
            currentZoneRadius = brInitialRadius + (BR_FINAL_RADIUS - brInitialRadius) * t;

            if (elapsed >= BR_DURATION_MS) {
                brSurvived = true;
            } else if (isInDangerZone()) {
                damagePlayer(BR_ZONE_DAMAGE_PER_SEC / 60f, elapsed);
            }
        }

        if (ffaMode && !playerEliminated && !ffaTimeUp && !ffaAllEliminated) {
            long elapsed = SystemClock.uptimeMillis() - modeStartUptimeMs;
            if (elapsed >= FFA_DURATION_MS) {
                ffaTimeUp = true;
            } else if (getBotsRemaining() == 0) {
                ffaAllEliminated = true;
            }
        }

        if (botsPresent) {
            updateBots();
        }

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

        float[] uniformGroundColor = MAP_GROUND_COLORS[mapIndex];

        for (int x = -gridSize; x < gridSize; x++) {
            for (int z = -gridSize; z < gridSize; z++) {
                int ix = x + gridSize;
                int iz = z + gridSize;
                if (!blockExists[ix][iz]) continue;

                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, x * 2f, 0f, z * 2f);
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

                float[] color = cellBaseColor != null ? cellBaseColor[ix][iz] : uniformGroundColor;
                if (demolitionMode && targetBlocks != null && targetBlocks[ix][iz]) {
                    color = TARGET_COLOR;
                } else if (battleRoyaleMode) {
                    float wx = x * 2f, wz = z * 2f;
                    float dist = (float) Math.sqrt(wx * wx + wz * wz);
                    if (dist > currentZoneRadius) color = ZONE_DANGER_COLOR;
                }
                cube.draw(program, mvpMatrix, color);
            }
        }

        if (botsPresent) {
            for (Bot bot : bots) {
                if (!bot.alive) continue;
                drawCharacterModel(bot.x, 0f, bot.z, bot.yaw, bot.walkPhase, ENEMY_SHIRT_COLOR, false);
            }
        }

        if (tpp) {
            drawCharacterModel(playerX, playerY, playerZ, yawNow, walkCyclePhase, SHIRT_COLOR, true);
            walkCyclePhase += moveMagnitude * WALK_CYCLE_RATE * (moveMagnitude > 0.05f ? 1f : 0f);
        } else {
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
            drawViewmodel();
        }
    }

    private void damagePlayer(float amount, long elapsedForSurvivalStat) {
        playerHealth -= amount;
        if (playerHealth <= 0f) {
            playerHealth = 0f;
            playerEliminated = true;
            survivalMs = elapsedForSurvivalStat;
        }
    }

    private float distFromCenter(float x, float z) {
        return (float) Math.sqrt(x * x + z * z);
    }

    private void updateBots() {
        long now = SystemClock.uptimeMillis();
        long elapsed = now - modeStartUptimeMs;

        for (Bot bot : bots) {
            if (!bot.alive) continue;

            boolean inDanger = battleRoyaleMode && distFromCenter(bot.x, bot.z) > currentZoneRadius;
            float dx = playerX - bot.x;
            float dz = playerZ - bot.z;
            float distToPlayer = (float) Math.sqrt(dx * dx + dz * dz);

            float moveX = 0f, moveZ = 0f;
            boolean moving = false;

            if (inDanger) {
                float len = distFromCenter(bot.x, bot.z);
                if (len > 0.01f) {
                    moveX = -bot.x / len;
                    moveZ = -bot.z / len;
                    moving = true;
                }
                bot.health -= BR_ZONE_DAMAGE_PER_SEC / 60f;
                if (bot.health <= 0f) {
                    bot.alive = false;
                    continue;
                }
            } else if (!playerEliminated && distToPlayer < BOT_DETECT_RANGE) {
                if (distToPlayer > 0.01f) {
                    moveX = dx / distToPlayer;
                    moveZ = dz / distToPlayer;
                    moving = true;
                }
                if (distToPlayer < BOT_ENGAGE_RANGE && now - bot.lastShotUptimeMs > BOT_SHOT_INTERVAL_MS) {
                    bot.lastShotUptimeMs = now;
                    if (random.nextFloat() < BOT_HIT_CHANCE) {
                        damagePlayer(BOT_DAMAGE, elapsed);
                    }
                }
            } else {
                float wdx = bot.wanderTargetX - bot.x;
                float wdz = bot.wanderTargetZ - bot.z;
                float wlen = (float) Math.sqrt(wdx * wdx + wdz * wdz);
                if (wlen < 1f) {
                    bot.wanderTargetX = bot.x + (random.nextFloat() - 0.5f) * BOT_WANDER_RADIUS * 2f;
                    bot.wanderTargetZ = bot.z + (random.nextFloat() - 0.5f) * BOT_WANDER_RADIUS * 2f;
                } else {
                    moveX = wdx / wlen;
                    moveZ = wdz / wlen;
                    moving = true;
                }
            }

            bot.x += moveX * BOT_MOVE_SPEED;
            bot.z += moveZ * BOT_MOVE_SPEED;
            if (moving) {
                bot.yaw = (float) Math.atan2(moveX, -moveZ);
                bot.walkPhase += WALK_CYCLE_RATE;
            }
        }
    }

    /** Shared by the player (TPP) and every bot — position/orientation/colors differ, geometry doesn't. */
    private void drawCharacterModel(float x, float y, float z, float facingYaw, float modelWalkPhase,
                                     float[] shirtColor, boolean isPlayer) {
        float bob = (float) Math.sin(animTime * IDLE_BOB_RATE) * IDLE_BOB_AMOUNT;

        Matrix.setIdentityM(characterMatrix, 0);
        Matrix.translateM(characterMatrix, 0, x, y + bob, z);
        Matrix.rotateM(characterMatrix, 0, (float) Math.toDegrees(facingYaw), 0f, 1f, 0f);

        float swing = (float) Math.sin(modelWalkPhase);
        float legSwingDeg = swing * MAX_LEG_SWING_DEG;
        float armSwingDeg = swing * MAX_ARM_SWING_DEG;

        drawSwingingPart(0.18f, LEG_HIP_Y, 0f, legSwingDeg, 0.15f, LEG_SCALE_LEN, 0.15f, PANTS_COLOR);
        drawSwingingPart(-0.18f, LEG_HIP_Y, 0f, -legSwingDeg, 0.15f, LEG_SCALE_LEN, 0.15f, PANTS_COLOR);
        drawSwingingSpherePart(0.18f, LEG_HIP_Y, 0f, legSwingDeg, LEG_FOOT_OFFSET, 0.16f, PANTS_COLOR);
        drawSwingingSpherePart(-0.18f, LEG_HIP_Y, 0f, -legSwingDeg, LEG_FOOT_OFFSET, 0.16f, PANTS_COLOR);

        drawPart(0f, TORSO_OY, 0f, 0.4f, 0.55f, 0.25f, shirtColor);

        drawSwingingPart(0.5f, ARM_SHOULDER_Y, 0f, -armSwingDeg, 0.12f, ARM_SCALE_LEN, 0.12f, shirtColor);
        drawSwingingPart(-0.5f, ARM_SHOULDER_Y, 0f, armSwingDeg, 0.12f, ARM_SCALE_LEN, 0.12f, shirtColor);
        drawSwingingSpherePart(0.5f, ARM_SHOULDER_Y, 0f, -armSwingDeg, ARM_HAND_OFFSET, 0.14f, SKIN_COLOR);
        drawSwingingSpherePart(-0.5f, ARM_SHOULDER_Y, 0f, armSwingDeg, ARM_HAND_OFFSET, 0.14f, SKIN_COLOR);

        drawSpherePart(0f, ARM_SHOULDER_Y + HEAD_RADIUS + 0.08f, 0f, HEAD_RADIUS, SKIN_COLOR);

        drawHeldWeapon(-armSwingDeg, isPlayer ? currentWeapon : WEAPON_RIFLE);

        if (isPlayer && thrustHeld && fuel > 0f) {
            drawJetpackFlames();
        }
    }

    private void drawJetpackFlames() {
        float flicker = 0.7f + 0.3f * (float) Math.sin(animTime * 0.9f);
        float[] flame = {FLAME_COLOR[0], FLAME_COLOR[1] * flicker, FLAME_COLOR[2], 1f};
        drawPart(0.18f, -0.12f, 0f, 0.07f, 0.10f * flicker, 0.07f, flame);
        drawPart(-0.18f, -0.12f, 0f, 0.07f, 0.10f * flicker, 0.07f, flame);
    }

    private void drawHeldWeapon(float rightArmSwingDeg, int weaponType) {
        System.arraycopy(characterMatrix, 0, weaponBaseMatrix, 0, 16);
        Matrix.translateM(weaponBaseMatrix, 0, 0.5f, ARM_SHOULDER_Y, 0f);
        Matrix.rotateM(weaponBaseMatrix, 0, rightArmSwingDeg, 1f, 0f, 0f);
        Matrix.translateM(weaponBaseMatrix, 0, 0.02f, -ARM_HAND_OFFSET - 0.03f, 0.05f);
        drawWeaponParts(weaponBaseMatrix, vpMatrix, weaponType);
    }

    private void drawViewmodel() {
        long sinceFire = SystemClock.uptimeMillis() - lastFireUptimeMs;
        float recoilT = sinceFire < RECOIL_DURATION_MS ? 1f - (sinceFire / (float) RECOIL_DURATION_MS) : 0f;
        float recoilKickZ = recoilT * 0.10f;
        float recoilLiftY = recoilT * 0.04f;

        float bobX = (float) Math.sin(animTime * IDLE_BOB_RATE * 0.6f) * 0.01f;
        float bobY = (float) Math.cos(animTime * IDLE_BOB_RATE * 0.6f) * 0.008f;

        Matrix.setIdentityM(viewmodelMatrix, 0);
        Matrix.translateM(viewmodelMatrix, 0,
                0.34f + bobX,
                -0.26f + bobY + recoilLiftY,
                -0.62f + recoilKickZ);
        Matrix.rotateM(viewmodelMatrix, 0, -6f, 0f, 1f, 0f);
        Matrix.rotateM(viewmodelMatrix, 0, 4f, 1f, 0f, 0f);

        drawWeaponParts(viewmodelMatrix, projectionMatrix, currentWeapon);
    }

    private void drawWeaponParts(float[] baseMatrix, float[] vp, int weaponType) {
        if (weaponType == WEAPON_PISTOL) {
            drawWeaponPart(baseMatrix, vp, 0f, 0f, 0.05f, 0.045f, 0.045f, 0.16f, METAL_MID);
            drawWeaponPart(baseMatrix, vp, 0f, -0.08f, -0.04f, 0.035f, 0.09f, 0.045f, GRIP_COLOR);
        } else {
            drawWeaponPart(baseMatrix, vp, 0f, 0.02f, 0.18f, 0.03f, 0.03f, 0.30f, METAL_DARK);
            drawWeaponPart(baseMatrix, vp, 0f, 0.0f, -0.02f, 0.06f, 0.07f, 0.20f, METAL_MID);
            drawWeaponPart(baseMatrix, vp, 0f, -0.01f, -0.20f, 0.045f, 0.05f, 0.14f, METAL_DARK);
            drawWeaponPart(baseMatrix, vp, 0f, -0.14f, 0.02f, 0.03f, 0.13f, 0.045f, METAL_DARK);
            drawWeaponPart(baseMatrix, vp, 0f, -0.08f, -0.08f, 0.03f, 0.08f, 0.04f, GRIP_COLOR);
        }
    }

    private void drawWeaponPart(float[] baseMatrix, float[] vp, float ox, float oy, float oz,
                                 float sx, float sy, float sz, float[] color) {
        System.arraycopy(baseMatrix, 0, weaponTempMatrix, 0, 16);
        Matrix.translateM(weaponTempMatrix, 0, ox, oy, oz);
        Matrix.scaleM(weaponTempMatrix, 0, sx, sy, sz);
        Matrix.multiplyMM(weaponMvpTemp, 0, vp, 0, weaponTempMatrix, 0);
        cube.draw(program, weaponMvpTemp, color);
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

