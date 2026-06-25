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

public class GameRenderer implements GLSurfaceView.Renderer {

    // ─── Shaders ───────────────────────────────────────────────────────────────
    private static final String VERT =
            "uniform mat4 uMVP;attribute vec4 vPos;attribute vec4 vCol;varying vec4 fCol;" +
            "void main(){gl_Position=uMVP*vPos;fCol=vCol;}";
    private static final String FRAG =
            "precision mediump float;varying vec4 fCol;void main(){gl_FragColor=fCol;}";

    // ─── Map ───────────────────────────────────────────────────────────────────
    private static final int GRID_NORMAL = 12;
    private static final int GRID_BIG    = 20;
    private static final float FOV       = 75f;

    /** Top of ground blocks = 2 × VoxelCube.SIZE = 1.8. Character feet rest here. */
    private static final float GROUND_Y  = 1.8f;

    private static final float[][] SKY_COLORS = {
            {0.45f,0.65f,0.85f,1f},{0.85f,0.66f,0.36f,1f},{0.75f,0.82f,0.90f,1f},{0.06f,0.07f,0.10f,1f}
    };
    private static final float[][] BASE_COLORS = {
            {0.18f,0.45f,0.22f,1f},{0.76f,0.66f,0.42f,1f},{0.86f,0.88f,0.92f,1f},{0.22f,0.20f,0.18f,1f}
    };
    private static final float[][] BIOME_TINTS = {
            {0.10f,0.32f,0.14f,1f},{0.42f,0.38f,0.30f,1f},{0.22f,0.50f,0.55f,1f}
    };

    // ─── Physics ───────────────────────────────────────────────────────────────
    private static final float GRAVITY      = -0.020f;
    private static final float HOP_VEL      =  0.160f;
    private static final float JET_ACCEL    =  0.032f;
    private static final float MAX_RISE     =  0.420f;
    private static final float MAX_FALL     = -0.600f;
    private static final float FUEL_MAX     = 100f;
    private static final float FUEL_DRAIN   =   1.2f;
    private static final float FUEL_REGEN   =   0.5f;
    private static final float MIN_FUEL_HOP =  10.0f;

    // ─── Camera ────────────────────────────────────────────────────────────────
    private static final float EYE_H       = 4.0f;
    private static final float TPP_DIST    = 6.0f;
    private static final float TPP_PIVOT_H = 1.8f;
    private static final float TPP_VOFF    = 1.5f;
    private static final float LOOK_SENS   = 0.0025f;
    private static final float PITCH_MAX   = (float) Math.toRadians(85);
    private static final float MOVE_SPEED  = 0.15f;

    // ─── Weapons ───────────────────────────────────────────────────────────────
    private static final int WEAPON_NONE  = -1;
    private static final int WEAPON_AK47   = 0;
    private static final int WEAPON_GLOCK  = 1;
    private static final int WEAPON_SNIPER = 2;
    private static final int WEAPON_SHOTGUN= 3;
    private static final int WEAPON_SMG    = 4;
    private static final int NUM_WEAPONS   = 5;

    private static final int[]    MAG    = {30, 15, 5, 6, 25};
    private static final long[]   RELOAD_MS = {1800,900,2500,1200,1100};
    private static final long[]   FIRE_DELAY= {100, 250,1200,650, 70};
    private static final String[] WPN_NAME  = {"AK-47","GLOCK","SNIPER","SHOTGUN","SMG"};
    private static final float    SHOOT_STEP= 0.10f;
    private static final float    SHOOT_MAX = 40f;
    private static final long     HIT_FLASH = 150;
    private static final long     RECOIL_MS = 160;

    // ─── Loot ──────────────────────────────────────────────────────────────────
    private static final float PICKUP_R    = 2.0f;
    private static final int   LOOT_COUNT  = 20;

    // ─── Battle Royale ─────────────────────────────────────────────────────────
    private static final long  BR_MS       = 110_000;
    private static final float BR_FINAL_R  = 3.0f;
    private static final float BR_DMG_PS   = 16f;
    private static final int   BOT_BR      = 7;

    // ─── FFA ───────────────────────────────────────────────────────────────────
    private static final long  FFA_MS      = 120_000;
    private static final int   BOT_FFA     = 5;

    // ─── Demolition ────────────────────────────────────────────────────────────
    private static final int  DEMO_TARGETS = 15;
    private static final long DEMO_MS      = 60_000;

    // ─── Player health ─────────────────────────────────────────────────────────
    private static final float PLAYER_HP   = 100f;
    private static final float BOT_HP      = 100f;

    // ─── Bot AI ────────────────────────────────────────────────────────────────
    private static final float BOT_DETECT  = 14f;
    private static final float BOT_ENGAGE  = 9f;
    private static final float BOT_SPEED   = 0.09f;
    private static final long  BOT_SHOT_MS = 1400;
    private static final float BOT_MISS    = 0.35f;
    private static final float BOT_DMG     = 9f;
    private static final float BOT_HIT_R   = 0.4f;
    private static final float BOT_WANDER  = 10f;

    // ─── Blocky Roblox character anatomy ──────────────────────────────────────
    // All Y in character-local space; y=0 = character root (world GROUND_Y)
    private static final float HIP_Y      = 0.93f;
    private static final float LEG_OX     = 0.18f;
    private static final float LEG_SX     = 0.167f;
    private static final float LEG_SY     = 0.333f;  // length 0.60
    private static final float LEG_LEN    = 0.60f;
    private static final float BOOT_SX    = 0.210f;  // wider than leg
    private static final float BOOT_SY    = 0.183f;  // height 0.33, bottom at y=0 ✓
    private static final float TORSO_SX   = 0.400f;
    private static final float TORSO_SY   = 0.361f;  // height 0.65
    private static final float TORSO_SZ   = 0.222f;
    private static final float SHOULDER_Y = 1.58f;   // HIP_Y + 0.65
    private static final float ARM_OX     = 0.52f;
    private static final float ARM_SX     = 0.167f;
    private static final float ARM_SY     = 0.444f;  // length 0.80
    private static final float ARM_LEN    = 0.80f;
    private static final float FIST_SX    = 0.167f;
    private static final float FIST_SY    = 0.133f;  // height 0.24
    private static final float HEAD_Y     = 1.58f;
    private static final float HEAD_SX    = 0.306f;  // 0.55 wide
    private static final float HEAD_SY    = 0.306f;  // 0.55 tall
    private static final float HAIR_Y     = 2.13f;   // HEAD_Y + 0.55
    private static final float MAX_LEG_SWING = 35f;
    private static final float MAX_ARM_SWING = 28f;
    private static final float WALK_RATE    = 0.25f;
    private static final float IDLE_BOB_A   = 0.04f;
    private static final float IDLE_BOB_R   = 0.05f;

    // ─── Colors ────────────────────────────────────────────────────────────────
    private static final float[] C_SKIN   = {0.85f,0.70f,0.55f,1f};
    private static final float[] C_TACT   = {0.10f,0.10f,0.11f,1f}; // black tactical
    private static final float[] C_ENEMY  = {0.20f,0.08f,0.08f,1f}; // dark red (enemy)
    private static final float[] C_BOOT   = {0.32f,0.33f,0.36f,1f}; // grey boot
    private static final float[] C_HAIR   = {0.07f,0.06f,0.06f,1f}; // near-black
    private static final float[] C_GROUND = {0.18f,0.45f,0.22f,1f};
    private static final float[] C_TARGET = {0.85f,0.16f,0.10f,1f};
    private static final float[] C_ZONE   = {0.30f,0.10f,0.26f,1f};
    private static final float[] C_LOOT   = {0.95f,0.82f,0.15f,1f}; // gold loot pickup
    private static final float[] C_METAL_D= {0.16f,0.16f,0.18f,1f};
    private static final float[] C_METAL_M= {0.32f,0.32f,0.35f,1f};
    private static final float[] C_GRIP   = {0.08f,0.08f,0.08f,1f};
    private static final float[] C_WOOD   = {0.55f,0.35f,0.15f,1f};
    private static final float[] C_SCOPE  = {0.18f,0.18f,0.20f,1f};
    private static final float[] C_FLAME1 = {1.00f,0.70f,0.15f,1f};
    private static final float[] C_FLAME2 = {1.00f,0.35f,0.08f,1f};

    // ─── Inner classes ─────────────────────────────────────────────────────────
    private static class LootItem {
        float x, z; int weaponType; boolean collected;
        LootItem(float x, float z, int w) { this.x=x; this.z=z; weaponType=w; }
    }

    // ─── GL objects ────────────────────────────────────────────────────────────
    private int program;
    private VoxelCube cube;

    // ─── Matrices ──────────────────────────────────────────────────────────────
    private final float[] proj = new float[16];
    private final float[] viewModProj = new float[16]; // near=0.05, for viewmodel
    private final float[] view = new float[16];
    private final float[] vp   = new float[16];
    private final float[] mdl  = new float[16];
    private final float[] mvp  = new float[16];
    private final float[] charMat  = new float[16];
    private final float[] partMat  = new float[16];
    private final float[] wBase    = new float[16];
    private final float[] wTemp    = new float[16];
    private final float[] wMvp     = new float[16];
    private final float[] vmMat    = new float[16];

    // ─── Player state ──────────────────────────────────────────────────────────
    private float playerX, playerZ;
    private float playerY = GROUND_Y;
    private float velY    = 0f;
    private boolean grounded = true;
    private volatile float yaw = 0f;
    private volatile float pitch = 0f;
    private volatile float moveDx, moveDz;
    private volatile boolean tpp = false;
    private volatile boolean thrustHeld = false;
    private float fuel = FUEL_MAX;
    private float playerHealth = PLAYER_HP;
    private volatile boolean playerEliminated = false;

    // ─── Weapon state ──────────────────────────────────────────────────────────
    private int currentWeapon = WEAPON_AK47;
    private int secondWeapon  = WEAPON_GLOCK;
    private final int[]     ammo     = new int[NUM_WEAPONS];
    private final boolean[] reloading= new boolean[NUM_WEAPONS];
    private long lastFireMs  = 0;
    private long lastHitMs   = 0;
    private long lastPickupMs= 0;
    private String pickupMsg = "";

    // ─── Animation ─────────────────────────────────────────────────────────────
    private float walkPhase = 0f;
    private float animTime  = 0f;

    // ─── Settings ──────────────────────────────────────────────────────────────
    private final float sensitivity;

    // ─── Map / mode ────────────────────────────────────────────────────────────
    private final int gridSize;
    private final int mapIdx;
    private final boolean[][] blocks;
    private boolean[][] layer2;  // ruins: second block layer for walls/debris
    private static final float[] C_RUIN2={0.16f,0.14f,0.13f,1f}; // darker ruins wall
    private final float[][][] cellColor;

    private final boolean demolitionMode;
    private final boolean battleRoyaleMode;
    private final boolean ffaMode;
    private final boolean botsPresent;

    // ─── Demolition ────────────────────────────────────────────────────────────
    private boolean[][] targets;
    private volatile int targetsLeft = 0;
    private long demoStart = 0;
    private volatile boolean gameWon = false;
    private volatile boolean gameLost = false;
    private volatile long wonMs = 0;

    // ─── BR / FFA ──────────────────────────────────────────────────────────────
    private final List<Bot> bots = new ArrayList<>();
    private final List<LootItem> loot = new ArrayList<>();
    private final Random rnd = new Random();
    private volatile int kills = 0;
    private float brInitR, brZoneR;
    private long modeStart = 0;
    private volatile boolean brSurvived = false;
    private volatile boolean ffaTimeUp = false;
    private volatile boolean ffaAllDown = false;
    private volatile long survivalMs = 0;

    // ──────────────────────────────────────────────────────────────────────────
    public GameRenderer(Context ctx, String mode, String mapId) {
        demolitionMode   = "demolition".equals(mode);
        battleRoyaleMode = "battleroyale".equals(mode);
        ffaMode          = "ffa".equals(mode);
        botsPresent      = battleRoyaleMode || ffaMode;
        gridSize         = botsPresent ? GRID_BIG : GRID_NORMAL;
        mapIdx           = "desert".equals(mapId)?1:"snow".equals(mapId)?2:"ruins".equals(mapId)?3:0;

        blocks    = new boolean[gridSize*2][gridSize*2];
        for (boolean[] r : blocks) Arrays.fill(r, true);
        cellColor = botsPresent ? buildBiome() : null;

        SharedPreferences p = ctx.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        int sp = p.getInt(SettingsActivity.KEY_SENSITIVITY_PERCENT, SettingsActivity.DEFAULT_SENSITIVITY_PERCENT);
        sensitivity = SettingsActivity.percentToMultiplier(sp);

        // Ammo init
        if (battleRoyaleMode) {
            // start unarmed
            currentWeapon = WEAPON_NONE;
            secondWeapon  = WEAPON_NONE;
            Arrays.fill(ammo, 0);
        } else {
            for (int i = 0; i < NUM_WEAPONS; i++) ammo[i] = MAG[i];
        }

        brInitR = gridSize * 2f * 0.9f;
        brZoneR = brInitR;

        // Spawn player off-center
        if (botsPresent) {
            float[] sp2 = offCenterSpawn(brInitR * 0.65f);
            playerX = sp2[0]; playerZ = sp2[1];
        } else {
            playerX = 0f; playerZ = 10f;
        }
        playerY = GROUND_Y;

        if (demolitionMode) initDemo();
        if (battleRoyaleMode) { modeStart = SystemClock.uptimeMillis(); initLoot(); spawnBots(BOT_BR); }
        if (ffaMode) { modeStart = SystemClock.uptimeMillis(); spawnBots(BOT_FFA); }
        if (mapIdx==3) initRuins();
    }

    private float[] offCenterSpawn(float maxR) {
        float a = rnd.nextFloat() * (float)(Math.PI*2);
        float d = maxR * (0.35f + rnd.nextFloat() * 0.65f);
        return new float[]{(float)Math.cos(a)*d, (float)Math.sin(a)*d};
    }

    private void initDemo() {
        targets = new boolean[gridSize*2][gridSize*2];
        int placed = 0;
        while (placed < DEMO_TARGETS) {
            int x=rnd.nextInt(gridSize*2), z=rnd.nextInt(gridSize*2);
            if (!targets[x][z]) { targets[x][z]=true; placed++; }
        }
        targetsLeft = DEMO_TARGETS;
        demoStart = SystemClock.uptimeMillis();
    }

    private void initLoot() {
        int[] pool = {WEAPON_AK47,WEAPON_AK47,WEAPON_GLOCK,WEAPON_GLOCK,WEAPON_SNIPER,
                      WEAPON_SHOTGUN,WEAPON_SHOTGUN,WEAPON_SMG,WEAPON_SMG,WEAPON_SMG,
                      WEAPON_AK47,WEAPON_GLOCK,WEAPON_SNIPER,WEAPON_SHOTGUN,WEAPON_SMG,
                      WEAPON_AK47,WEAPON_GLOCK,WEAPON_SMG,WEAPON_SNIPER,WEAPON_SHOTGUN};
        for (int i = 0; i < LOOT_COUNT; i++) {
            float[] pos = offCenterSpawn(gridSize * 1.4f);
            loot.add(new LootItem(pos[0], pos[1], pool[i % pool.length]));
        }
    }

    private void spawnBots(int count) {
        for (int i = 0; i < count; i++) {
            float[] p = offCenterSpawn(brInitR * 0.7f);
            Bot b = new Bot(p[0], p[1], BOT_HP);
            if (!battleRoyaleMode) { b.yaw = rnd.nextFloat()*(float)(Math.PI*2); }
            bots.add(b);
        }
    }

    private float[][][] buildBiome() {
        float[][][] c = new float[gridSize*2][gridSize*2][];
        float[] base = BASE_COLORS[mapIdx];
        int S = 12;
        float[] sx = new float[S], sz2 = new float[S];
        float[][] st = new float[S][];
        float ext = gridSize * 2f;
        for (int i=0;i<S;i++){sx[i]=(rnd.nextFloat()*2-1)*ext;sz2[i]=(rnd.nextFloat()*2-1)*ext;st[i]=BIOME_TINTS[rnd.nextInt(BIOME_TINTS.length)];}
        for (int x=-gridSize;x<gridSize;x++) for (int z=-gridSize;z<gridSize;z++){
            float wx=x*2f, wz=z*2f, best=Float.MAX_VALUE; int near=0;
            for (int i=0;i<S;i++){float d=(wx-sx[i])*(wx-sx[i])+(wz-sz2[i])*(wz-sz2[i]);if(d<best){best=d;near=i;}}
            float dist=(float)Math.sqrt(best);
            float[] t=st[near];
            float[] col=dist>14f?base:new float[]{base[0]*.5f+t[0]*.5f,base[1]*.5f+t[1]*.5f,base[2]*.5f+t[2]*.5f,1f};
            c[x+gridSize][z+gridSize]=col;
        }
        return c;
    }

    // ─── Public API ────────────────────────────────────────────────────────────
    public void setMoveInput(float dx, float dy){ moveDx=dx; moveDz=dy; }

    public void addLookDelta(float dx, float dy){
        float s = LOOK_SENS * sensitivity;
        yaw   += dx * s;
        pitch -= dy * s;
        if (pitch > PITCH_MAX) pitch = PITCH_MAX;
        if (pitch < -PITCH_MAX) pitch = -PITCH_MAX;
    }

    public void toggleViewMode(){ tpp=!tpp; }

    public void onThrustPress(){
        thrustHeld=true;
        if (grounded && fuel > MIN_FUEL_HOP){ velY=HOP_VEL; grounded=false; }
    }
    public void onThrustRelease(){ thrustHeld=false; }
    public float getFuelPercent(){ return fuel/FUEL_MAX; }

    public void switchWeapon(){
        if (battleRoyaleMode){ int t=currentWeapon; currentWeapon=secondWeapon; secondWeapon=t; }
        else { currentWeapon=(currentWeapon+1) % NUM_WEAPONS; }
    }

    public void manualReload(){
        int w=currentWeapon; if (w<0) return;
        if (!reloading[w] && ammo[w]<MAG[w]) startReload(w);
    }

    public String getWeaponName(){ return currentWeapon<0?"UNARMED":WPN_NAME[currentWeapon]; }
    public String getSecondWeaponName(){ return secondWeapon<0?"":WPN_NAME[secondWeapon]; }
    public int getCurrentAmmo(){ return currentWeapon<0?0:ammo[currentWeapon]; }
    public int getMagSize(){ return currentWeapon<0?0:MAG[currentWeapon]; }
    public boolean isReloading(){ return currentWeapon>=0 && reloading[currentWeapon]; }
    public boolean isRecentHit(){ return SystemClock.uptimeMillis()-lastHitMs<HIT_FLASH; }
    public float getPlayerHealthPercent(){ return Math.max(0,playerHealth/PLAYER_HP); }
    public boolean isPlayerEliminated(){ return playerEliminated; }

    public boolean isDemolitionMode(){ return demolitionMode; }
    public int getTargetsRemaining(){ return targetsLeft; }
    public long getDemoTimeMs(){ long e=SystemClock.uptimeMillis()-demoStart; return Math.max(0,DEMO_MS-e); }
    public boolean isGameWon(){ return gameWon; }
    public boolean isGameLost(){ return gameLost; }
    public long getCompletionMs(){ return wonMs; }

    public boolean isBattleRoyaleMode(){ return battleRoyaleMode; }
    public boolean isFfaMode(){ return ffaMode; }
    public int getKills(){ return kills; }
    public int getBotsRemaining(){ int n=0; for (Bot b:bots) if(b.alive) n++; return n; }
    public boolean isBrSurvived(){ return brSurvived; }
    public boolean isFfaTimeUp(){ return ffaTimeUp; }
    public boolean isFfaAllDown(){ return ffaAllDown; }
    public long getSurvivalMs(){ return survivalMs; }
    public boolean isInDangerZone(){ if(!battleRoyaleMode) return false; float d=(float)Math.sqrt(playerX*playerX+playerZ*playerZ); return d>brZoneR; }
    public long getModeTimeLeftMs(){ if(!ffaMode&&!battleRoyaleMode) return 0; long dur=battleRoyaleMode?BR_MS:FFA_MS; return Math.max(0,dur-(SystemClock.uptimeMillis()-modeStart)); }

    public String getPickupMessage(){
        return SystemClock.uptimeMillis()-lastPickupMs<2500 ? pickupMsg : "";
    }
    public String getNearbyLootName(){
        if (!battleRoyaleMode) return null;
        for (LootItem item:loot){
            if (item.collected) continue;
            float dx=playerX-item.x, dz=playerZ-item.z;
            if (dx*dx+dz*dz < PICKUP_R*PICKUP_R) return WPN_NAME[item.weaponType];
        }
        return null;
    }

    public void tryShoot(){
        if (currentWeapon<0) return;
        int w=currentWeapon;
        if (reloading[w]) return;
        if (ammo[w]<=0){ startReload(w); return; }
        long now=SystemClock.uptimeMillis();
        if (now-lastFireMs<FIRE_DELAY[w]) return;
        lastFireMs=now;
        ammo[w]--;
        if (ammo[w]==0) startReload(w);

        float cpitch=(float)Math.cos(pitch);
        float dx=(float)Math.sin(yaw)*cpitch, dy=(float)Math.sin(pitch), dz=(float)-Math.cos(yaw)*cpitch;
        float ox=playerX, oy=EYE_H+playerY, oz=playerZ;

        for (float t=0.5f;t<SHOOT_MAX;t+=SHOOT_STEP){
            float px=ox+dx*t, py=oy+dy*t, pz=oz+dz*t;
            if (botsPresent){ for (Bot b:bots){ if(!b.alive) continue;
                float bdx=px-b.x, bdz=pz-b.z;
                if ((float)Math.sqrt(bdx*bdx+bdz*bdz)<BOT_HIT_R && py>GROUND_Y && py<GROUND_Y+2.5f){
                    b.alive=false; kills++; lastHitMs=now; return;
                }
            }}
            if (py<GROUND_Y || py>GROUND_Y+2f*VoxelCube.SIZE) continue;
            int gx=Math.round(px/2f), gz=Math.round(pz/2f);
            if (gx<-gridSize||gx>=gridSize||gz<-gridSize||gz>=gridSize) continue;
            int ix=gx+gridSize, iz=gz+gridSize;
            if (!blocks[ix][iz]) continue;
            float lx=px-gx*2f, lz=pz-gz*2f;
            if (Math.abs(lx)<VoxelCube.SIZE && Math.abs(lz)<VoxelCube.SIZE){
                blocks[ix][iz]=false; lastHitMs=now;
                if (demolitionMode&&!gameWon&&!gameLost&&targets!=null&&targets[ix][iz]){
                    targets[ix][iz]=false; targetsLeft--;
                    if (targetsLeft<=0){gameWon=true;wonMs=now-demoStart;}
                }
                return;
            }
        }
    }

    private void startReload(int w){
        reloading[w]=true;
        new Handler(Looper.getMainLooper()).postDelayed(()->{ ammo[w]=MAG[w]; reloading[w]=false; }, RELOAD_MS[w]);
    }

    // ─── GL Lifecycle ─────────────────────────────────────────────────────────
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig cfg){
        float[] sky=SKY_COLORS[mapIdx];
        GLES20.glClearColor(sky[0],sky[1],sky[2],sky[3]);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        int vs=shader(GLES20.GL_VERTEX_SHADER,VERT), fs=shader(GLES20.GL_FRAGMENT_SHADER,FRAG);
        program=GLES20.glCreateProgram();
        GLES20.glAttachShader(program,vs); GLES20.glAttachShader(program,fs); GLES20.glLinkProgram(program);
        cube=new VoxelCube();
    }

    @Override public void onSurfaceChanged(GL10 gl, int w, int h){
        GLES20.glViewport(0,0,w,h);
        float a=(float)w/h;
        Matrix.perspectiveM(proj,       0, FOV, a, 0.50f, 260f);
        Matrix.perspectiveM(viewModProj, 0, FOV, a, 0.05f, 10f);
    }

    @Override public void onDrawFrame(GL10 gl){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        animTime++;

        // ── Mode timers ──
        if (demolitionMode&&!gameWon&&!gameLost&&getDemoTimeMs()<=0) gameLost=true;

        // ── Jetpack / gravity ──
        boolean thrusting=thrustHeld&&fuel>0f;
        if (thrusting){ velY+=JET_ACCEL; fuel=Math.max(0,fuel-FUEL_DRAIN); }
        else           { fuel=Math.min(FUEL_MAX,fuel+FUEL_REGEN); }
        velY+=GRAVITY;
        velY=Math.max(MAX_FALL,Math.min(MAX_RISE,velY));
        playerY+=velY;
        if (playerY<=GROUND_Y){ playerY=GROUND_Y; velY=0; grounded=true; } else { grounded=false; }

        // ── Movement ──
        float yN=yaw, pN=pitch;
        float fwdX=(float)Math.sin(yN), fwdZ=(float)-Math.cos(yN);
        float rtX=(float)Math.cos(yN), rtZ=(float)Math.sin(yN);
        float fa=-moveDz, sa=moveDx;
        float mag=(float)Math.min(1,Math.sqrt(fa*fa+sa*sa));
        playerX+=(fwdX*fa+rtX*sa)*MOVE_SPEED;
        playerZ+=(fwdZ*fa+rtZ*sa)*MOVE_SPEED;

        // ── Battle Royale zone ──
        if (battleRoyaleMode&&!playerEliminated&&!brSurvived){
            long el=SystemClock.uptimeMillis()-modeStart;
            float t2=Math.min(1f,el/(float)BR_MS);
            brZoneR=brInitR+(BR_FINAL_R-brInitR)*t2;
            if (el>=BR_MS){ brSurvived=true; }
            else if (isInDangerZone()){ damagePlayer(BR_DMG_PS/60f, el); }
        }
        // ── FFA ──
        if (ffaMode&&!playerEliminated){
            long el=SystemClock.uptimeMillis()-modeStart;
            if (el>=FFA_MS) ffaTimeUp=true;
            if (getBotsRemaining()==0) ffaAllDown=true;
        }
        // ── Bots ──
        if (botsPresent) updateBots();
        // ── Loot ──
        if (battleRoyaleMode) checkLoot();

        // ── Camera ──
        float cp=(float)Math.cos(pN);
        float ldX=(float)Math.sin(yN)*cp, ldY=(float)Math.sin(pN), ldZ=(float)-Math.cos(yN)*cp;
        if (tpp){
            float pivX=playerX, pivY=playerY+TPP_PIVOT_H, pivZ=playerZ;
            Matrix.setLookAtM(view,0, pivX-ldX*TPP_DIST, pivY-ldY*TPP_DIST+TPP_VOFF, pivZ-ldZ*TPP_DIST,
                              pivX,pivY,pivZ, 0,1,0);
        } else {
            Matrix.setLookAtM(view,0, playerX,EYE_H+playerY,playerZ,
                              playerX+ldX,EYE_H+playerY+ldY,playerZ+ldZ, 0,1,0);
        }
        Matrix.multiplyMM(vp,0,proj,0,view,0);
        GLES20.glUseProgram(program);

        // ── Ground blocks ──
        float[] baseCol=BASE_COLORS[mapIdx];
        for (int x=-gridSize;x<gridSize;x++) for (int z=-gridSize;z<gridSize;z++){
            int ix=x+gridSize, iz=z+gridSize;
            if (!blocks[ix][iz]) continue;
            Matrix.setIdentityM(mdl,0);
            Matrix.translateM(mdl,0,x*2f,0f,z*2f);
            Matrix.multiplyMM(mvp,0,vp,0,mdl,0);
            float[] col=cellColor!=null?cellColor[ix][iz]:baseCol;
            if (demolitionMode&&targets!=null&&targets[ix][iz]) col=C_TARGET;
            if (battleRoyaleMode){float wx=x*2f,wz=z*2f; if((float)Math.sqrt(wx*wx+wz*wz)>brZoneR) col=C_ZONE;}
            cube.draw(program,mvp,col);
        }

        // ── Ruins layer 2 (walls / debris) ──
        if (layer2!=null) for (int x=-gridSize;x<gridSize;x++) for (int z=-gridSize;z<gridSize;z++){
            int ix=x+gridSize,iz=z+gridSize;
            if (!layer2[ix][iz]) continue;
            Matrix.setIdentityM(mdl,0);
            Matrix.translateM(mdl,0,x*2f,1.8f,z*2f);
            Matrix.multiplyMM(mvp,0,vp,0,mdl,0);
            cube.draw(program,mvp,C_RUIN2);
        }

        // ── Loot cubes ──
        if (battleRoyaleMode){
            for (LootItem item:loot){
                if (item.collected) continue;
                float bob=0.15f+(float)Math.sin(animTime*0.06f)*0.05f;
                Matrix.setIdentityM(mdl,0);
                Matrix.translateM(mdl,0,item.x,GROUND_Y+bob,item.z);
                Matrix.scaleM(mdl,0,0.22f,0.22f,0.22f);
                Matrix.rotateM(mdl,0,animTime*1.5f,0,1,0);
                Matrix.multiplyMM(mvp,0,vp,0,mdl,0);
                cube.draw(program,mvp,C_LOOT);
            }
        }

        // ── Bots ──
        if (botsPresent){
            for (Bot b:bots){
                if (!b.alive) continue;
                drawCharacter(b.x, GROUND_Y, b.z, b.yaw, b.walkPhase, C_ENEMY, false);
            }
        }

        // ── Player (TPP) ──
        if (tpp){
            drawCharacter(playerX,playerY,playerZ,yN,walkPhase,C_TACT,true);
            if (mag>0.05f) walkPhase+=mag*WALK_RATE;
        } else {
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
            drawViewmodel();
        }
    }

    // ─── Damage ────────────────────────────────────────────────────────────────
    private void damagePlayer(float amount, long el){
        playerHealth-=amount;
        if (playerHealth<=0){playerHealth=0;playerEliminated=true;survivalMs=el;}
    }

    // ─── Loot pickup ──────────────────────────────────────────────────────────
    private void checkLoot(){
        for (LootItem item:loot){
            if (item.collected) continue;
            float dx=playerX-item.x, dz=playerZ-item.z;
            if (dx*dx+dz*dz < PICKUP_R*PICKUP_R){
                if (currentWeapon==WEAPON_NONE){
                    currentWeapon=item.weaponType;
                    ammo[item.weaponType]=MAG[item.weaponType];
                    item.collected=true;
                    pickupMsg="PICKED UP: "+WPN_NAME[item.weaponType];
                    lastPickupMs=SystemClock.uptimeMillis();
                } else if (secondWeapon==WEAPON_NONE){
                    secondWeapon=item.weaponType;
                    ammo[item.weaponType]=MAG[item.weaponType];
                    item.collected=true;
                    pickupMsg="PICKED UP: "+WPN_NAME[item.weaponType]+" (SLOT 2)";
                    lastPickupMs=SystemClock.uptimeMillis();
                }
            }
        }
    }

    // ─── Bot AI ────────────────────────────────────────────────────────────────
    private void updateBots(){
        long now=SystemClock.uptimeMillis();
        long el=now-modeStart;
        for (Bot b:bots){
            if (!b.alive) continue;
            boolean danger=battleRoyaleMode&&(float)Math.sqrt(b.x*b.x+b.z*b.z)>brZoneR;
            if (danger){ b.health-=BR_DMG_PS/60f; if(b.health<=0){b.alive=false;continue;} }
            float dx=playerX-b.x, dz=playerZ-b.z;
            float dist=(float)Math.sqrt(dx*dx+dz*dz);
            float mx=0,mz=0; boolean moving=false;
            if (danger){ float len=(float)Math.sqrt(b.x*b.x+b.z*b.z); if(len>.01f){mx=-b.x/len;mz=-b.z/len;moving=true;} }
            else if (!playerEliminated&&dist<BOT_DETECT){
                if(dist>.01f){mx=dx/dist;mz=dz/dist;moving=true;}
                if(dist<BOT_ENGAGE&&now-b.lastShotUptimeMs>BOT_SHOT_MS){
                    b.lastShotUptimeMs=now;
                    if(rnd.nextFloat()<BOT_MISS) damagePlayer(BOT_DMG,el);
                }
            } else {
                float wdx=b.wanderTargetX-b.x, wdz=b.wanderTargetZ-b.z;
                float wl=(float)Math.sqrt(wdx*wdx+wdz*wdz);
                if(wl<1f){b.wanderTargetX=b.x+(rnd.nextFloat()-.5f)*BOT_WANDER*2;b.wanderTargetZ=b.z+(rnd.nextFloat()-.5f)*BOT_WANDER*2;}
                else{mx=wdx/wl;mz=wdz/wl;moving=true;}
            }
            b.x+=mx*BOT_SPEED; b.z+=mz*BOT_SPEED;
            if(moving){b.yaw=(float)Math.atan2(mx,-mz);b.walkPhase+=WALK_RATE;}
        }
    }

    // ─── Character drawing ────────────────────────────────────────────────────
    private void drawCharacter(float x, float y, float z, float yawR, float wPhase, float[] shirt, boolean isPlayer){
        float bob=(float)Math.sin(animTime*IDLE_BOB_R)*IDLE_BOB_A;
        Matrix.setIdentityM(charMat,0);
        Matrix.translateM(charMat,0,x,y+bob,z);
        Matrix.rotateM(charMat,0,(float)Math.toDegrees(yawR),0,1,0);

        float sw=(float)Math.sin(wPhase);
        float lg=sw*MAX_LEG_SWING, ag=sw*MAX_ARM_SWING;

        // Boots (grey, swing with legs from hip — boot bottom at charY=0 = world GROUND_Y ✓)
        swingBox(-LEG_OX, HIP_Y, 0, lg,  LEG_LEN, BOOT_SX, BOOT_SY, BOOT_SX, C_BOOT);
        swingBox( LEG_OX, HIP_Y, 0,-lg,  LEG_LEN, BOOT_SX, BOOT_SY, BOOT_SX, C_BOOT);
        // Legs
        swingPart(-LEG_OX, HIP_Y, 0, lg,  LEG_SX, LEG_SY, LEG_SX, shirt);
        swingPart( LEG_OX, HIP_Y, 0,-lg,  LEG_SX, LEG_SY, LEG_SX, shirt);
        // Torso
        part(0, HIP_Y, 0, TORSO_SX, TORSO_SY, TORSO_SZ, shirt);
        // Arms
        swingPart(-ARM_OX, SHOULDER_Y, 0, ag, ARM_SX, ARM_SY, ARM_SX, shirt);
        swingPart( ARM_OX, SHOULDER_Y, 0,-ag, ARM_SX, ARM_SY, ARM_SX, shirt);
        // Fists (cube hands — geometric, no sphere)
        swingBox(-ARM_OX, SHOULDER_Y, 0, ag,  ARM_LEN, FIST_SX, FIST_SY, FIST_SX, C_SKIN);
        swingBox( ARM_OX, SHOULDER_Y, 0,-ag,  ARM_LEN, FIST_SX, FIST_SY, FIST_SX, C_SKIN);
        // Head (block cube — not sphere)
        part(0, HEAD_Y, 0, HEAD_SX, HEAD_SY, HEAD_SX, C_SKIN);
        // Hair (spiky)
        float hy=HAIR_Y;
        part(0,     hy,     -0.05f, 0.10f, 0.14f, 0.08f, C_HAIR);
        part(-0.14f,hy+0.02f, 0f,   0.08f, 0.11f, 0.07f, C_HAIR);
        part( 0.14f,hy+0.01f, 0.02f,0.08f, 0.12f, 0.07f, C_HAIR);
        part( 0.05f,hy+0.04f,-0.11f,0.07f, 0.10f, 0.06f, C_HAIR);
        part(-0.07f,hy+0.03f,-0.04f,0.07f, 0.09f, 0.06f, C_HAIR);
        part(0, HEAD_Y+HEAD_SY*1.8f-0.04f, 0, 0.27f, 0.028f, 0.27f, C_HAIR); // hair base mat

        // Weapon in right hand
        int wdraw = isPlayer ? currentWeapon : WEAPON_AK47;
        if (wdraw >= 0) drawHeldWeapon(-ag, wdraw);

        // Jetpack flames from boots
        if (isPlayer && thrustHeld && fuel>0) drawBoostFlames(lg);
    }

    private void drawBoostFlames(float legSwing){
        float flk=0.55f+0.45f*(float)Math.abs(Math.sin(animTime*1.4f));
        float fh=0.26f*flk, fsy=fh/1.8f;
        float[] b1={1f,0.70f*flk,0.15f,1f}, b2={1f,0.35f,0.08f,1f};
        // Swing with boots (same pivot/angle as legs)
        swingBox(-LEG_OX, HIP_Y, 0, legSwing,  LEG_LEN+BOOT_SY*1.8f, 0.10f, fsy, 0.10f, b1);
        swingBox( LEG_OX, HIP_Y, 0,-legSwing,  LEG_LEN+BOOT_SY*1.8f, 0.10f, fsy, 0.10f, b1);
        swingBox(-LEG_OX, HIP_Y, 0, legSwing,  LEG_LEN+BOOT_SY*1.8f, 0.05f, fsy*0.65f, 0.05f, b2);
        swingBox( LEG_OX, HIP_Y, 0,-legSwing,  LEG_LEN+BOOT_SY*1.8f, 0.05f, fsy*0.65f, 0.05f, b2);
    }

    // ─── Weapon viewmodel (FPP) ────────────────────────────────────────────────
    private void drawViewmodel(){
        if (currentWeapon < 0) return;
        long sf=SystemClock.uptimeMillis()-lastFireMs;
        float rT=sf<RECOIL_MS?1f-(sf/(float)RECOIL_MS):0f;
        float bobX=(float)Math.sin(animTime*IDLE_BOB_R*0.6f)*0.010f;
        float bobY=(float)Math.cos(animTime*IDLE_BOB_R*0.6f)*0.008f;
        Matrix.setIdentityM(vmMat,0);
        Matrix.translateM(vmMat,0, 0.34f+bobX, -0.26f+bobY+rT*0.04f, -0.62f+rT*0.10f);
        Matrix.rotateM(vmMat,0,-6,0,1,0);
        Matrix.rotateM(vmMat,0, 4,1,0,0);
        drawWeaponParts(vmMat, viewModProj, currentWeapon);
    }

    // ─── Held weapon (TPP right hand) ─────────────────────────────────────────
    private void drawHeldWeapon(float rightArmSwing, int wt){
        System.arraycopy(charMat,0,wBase,0,16);
        Matrix.translateM(wBase,0, ARM_OX, SHOULDER_Y, 0);
        Matrix.rotateM(wBase,0,rightArmSwing,1,0,0);
        Matrix.translateM(wBase,0, 0.02f, -ARM_LEN-0.02f, 0.06f);
        Matrix.rotateM(wBase,0,-10,1,0,0);
        drawWeaponParts(wBase, vp, wt);
    }

    /** Shared geometry — vp can be world VP (TPP) or viewmodel projection (FPP). */
    private void drawWeaponParts(float[] base, float[] vpp, int wt){
        switch (wt){
            case WEAPON_AK47:
                wp(base,vpp, 0f,    0.03f,  0.25f, 0.026f,0.026f,0.40f, C_METAL_D); // barrel
                wp(base,vpp, 0f,    0.07f,  0.14f, 0.012f,0.012f,0.22f, C_METAL_D); // gas tube
                wp(base,vpp, 0f,    0.03f,  0f,    0.052f,0.060f,0.18f, C_METAL_M); // receiver
                wp(base,vpp, 0f,   -0.08f,  0.06f, 0.032f,0.11f, 0.040f,C_METAL_D); // mag
                wp(base,vpp, 0f,   -0.01f, -0.18f, 0.043f,0.050f,0.18f, C_WOOD);    // stock
                wp(base,vpp, 0f,   -0.09f, -0.06f, 0.028f,0.090f,0.035f,C_GRIP);    // grip
                wp(base,vpp, 0f,    0.03f,  0.43f, 0.032f,0.030f,0.042f,C_METAL_D); // muzzle
                break;
            case WEAPON_GLOCK:
                wp(base,vpp, 0f,    0.02f,  0.07f, 0.040f,0.042f,0.13f, C_METAL_D); // slide
                wp(base,vpp, 0f,   -0.015f, 0.05f, 0.042f,0.048f,0.13f, C_METAL_M); // frame
                wp(base,vpp, 0f,    0.020f, 0.18f, 0.020f,0.020f,0.020f,C_METAL_M); // barrel tip
                wp(base,vpp, 0f,   -0.085f, 0.02f, 0.040f,0.086f,0.040f,C_GRIP);    // grip
                wp(base,vpp, 0f,   -0.130f, 0.02f, 0.042f,0.015f,0.042f,C_METAL_D); // mag base
                break;
            case WEAPON_SNIPER:
                wp(base,vpp, 0f,    0.01f,  0.42f, 0.022f,0.022f,0.55f, C_METAL_D); // long barrel
                wp(base,vpp, 0f,    0.09f,  0.12f, 0.025f,0.028f,0.18f, C_SCOPE);   // scope body
                wp(base,vpp, 0f,    0.09f,  0.20f, 0.038f,0.038f,0.012f,C_METAL_D); // scope lens
                wp(base,vpp, 0f,    0.01f, -0.02f, 0.052f,0.062f,0.20f, C_METAL_M); // receiver
                wp(base,vpp, 0f,   -0.01f, -0.22f, 0.040f,0.050f,0.22f, C_WOOD);    // stock
                wp(base,vpp, 0f,   -0.085f,-0.04f, 0.028f,0.085f,0.036f,C_GRIP);    // grip
                wp(base,vpp,-0.04f,-0.065f, 0.30f, 0.010f,0.075f,0.010f,C_METAL_D); // bipod L
                wp(base,vpp, 0.04f,-0.065f, 0.30f, 0.010f,0.075f,0.010f,C_METAL_D); // bipod R
                break;
            case WEAPON_SHOTGUN:
                wp(base,vpp, 0f,    0.01f,  0.24f, 0.038f,0.038f,0.32f, C_METAL_M); // barrel
                wp(base,vpp, 0f,   -0.006f, 0.17f, 0.027f,0.027f,0.26f, C_METAL_D); // tube mag
                wp(base,vpp, 0f,   -0.002f, 0.13f, 0.048f,0.035f,0.10f, C_WOOD);    // pump
                wp(base,vpp, 0f,    0.00f, -0.01f, 0.058f,0.068f,0.18f, C_METAL_M); // receiver
                wp(base,vpp, 0f,   -0.01f, -0.20f, 0.046f,0.058f,0.19f, C_WOOD);    // stock
                wp(base,vpp, 0f,   -0.085f,-0.04f, 0.036f,0.090f,0.038f,C_GRIP);    // grip
                break;
            case WEAPON_SMG:
                wp(base,vpp, 0f,    0.01f,  0.20f, 0.024f,0.024f,0.22f, C_METAL_D); // barrel
                wp(base,vpp, 0f,    0.00f,  0.00f, 0.052f,0.062f,0.20f, C_METAL_M); // receiver
                wp(base,vpp, 0f,   -0.10f,  0.05f, 0.030f,0.13f, 0.036f,C_METAL_D); // mag
                wp(base,vpp, 0f,   -0.08f,  0.05f, 0.036f,0.086f,0.038f,C_GRIP);    // grip
                wp(base,vpp, 0f,   -0.08f,  0.16f, 0.028f,0.070f,0.030f,C_METAL_D); // foregrip
                wp(base,vpp, 0f,    0.00f, -0.12f, 0.038f,0.042f,0.11f, C_METAL_D); // folded stock
                break;
        }
    }

    private void wp(float[] base, float[] vpp, float ox,float oy,float oz, float sx,float sy,float sz, float[] col){
        System.arraycopy(base,0,wTemp,0,16);
        Matrix.translateM(wTemp,0,ox,oy,oz);
        Matrix.scaleM(wTemp,0,sx,sy,sz);
        Matrix.multiplyMM(wMvp,0,vpp,0,wTemp,0);
        cube.draw(program,wMvp,col);
    }

    // ─── Part helpers ─────────────────────────────────────────────────────────
    /** Static cube: base at (ox,oy,oz) in char-local space, extends up by 1.8*sy. */
    private void part(float ox,float oy,float oz,float sx,float sy,float sz,float[] col){
        System.arraycopy(charMat,0,partMat,0,16);
        Matrix.translateM(partMat,0,ox,oy,oz);
        Matrix.scaleM(partMat,0,sx,sy,sz);
        Matrix.multiplyMM(mvp,0,vp,0,partMat,0);
        cube.draw(program,mvp,col);
    }

    /** Swinging part — cube pivots at (ox,pivotY,oz), hangs DOWN by 1.8*sy. */
    private void swingPart(float ox,float pivotY,float oz,float deg,float sx,float sy,float sz,float[] col){
        System.arraycopy(charMat,0,partMat,0,16);
        Matrix.translateM(partMat,0,ox,pivotY,oz);
        Matrix.rotateM(partMat,0,deg,1,0,0);
        Matrix.scaleM(partMat,0,sx,-sy,sz);
        Matrix.multiplyMM(mvp,0,vp,0,partMat,0);
        cube.draw(program,mvp,col);
    }

    /** Swinging box at TIPS of limbs — pivots at (ox,pivotY,oz), offset by tipOff, hangs DOWN by 1.8*sy. */
    private void swingBox(float ox,float pivotY,float oz,float deg,float tipOff,float sx,float sy,float sz,float[] col){
        System.arraycopy(charMat,0,partMat,0,16);
        Matrix.translateM(partMat,0,ox,pivotY,oz);
        Matrix.rotateM(partMat,0,deg,1,0,0);
        Matrix.translateM(partMat,0,0,-tipOff,0);
        Matrix.scaleM(partMat,0,sx,-sy,sz);
        Matrix.multiplyMM(mvp,0,vp,0,partMat,0);
        cube.draw(program,mvp,col);
    }

    private int shader(int type,String src){
        int s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,src); GLES20.glCompileShader(s); return s;
    }

    /** Ruins map: punch holes in layer1, add walls and debris on layer2. */
    private void initRuins(){
        layer2=new boolean[gridSize*2][gridSize*2];
        // Craters / holes in ground
        for(int x=0;x<gridSize*2;x++) for(int z=0;z<gridSize*2;z++)
            if(rnd.nextFloat()<0.20f) blocks[x][z]=false;
        // Wall segments
        for(int w=0;w<22;w++){
            int sx2=rnd.nextInt(gridSize*2), sz2=rnd.nextInt(gridSize*2);
            boolean hz=rnd.nextBoolean();
            int len=3+rnd.nextInt(7);
            for(int i=0;i<len;i++){
                int nx=hz?sx2+i:sx2, nz=hz?sz2:sz2+i;
                if(nx>=0&&nx<gridSize*2&&nz>=0&&nz<gridSize*2){
                    blocks[nx][nz]=true;
                    layer2[nx][nz]=true;
                }
            }
        }
        // Debris piles (2x2 and 3x3)
        for(int d=0;d<20;d++){
            int cs=1+rnd.nextInt(2);
            int cx=rnd.nextInt(gridSize*2-cs), cz=rnd.nextInt(gridSize*2-cs);
            for(int dx=0;dx<cs;dx++) for(int dz=0;dz<cs;dz++){
                int nx=cx+dx, nz=cz+dz;
                blocks[nx][nz]=true;
                if(rnd.nextFloat()<0.75f) layer2[nx][nz]=true;
            }
        }
    }
}
