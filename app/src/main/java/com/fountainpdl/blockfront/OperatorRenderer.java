package com.fountainpdl.blockfront;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Self-contained renderer for the menu's CODM-style operator preview.
 * Draws the blocky voxel character holding the AK-47, slowly auto-rotating
 * on the Y axis, with a simple top-down brightening gradient baked into
 * the clear color. No physics, no bots, no map — just the character.
 */
public class OperatorRenderer implements GLSurfaceView.Renderer {

    private static final String VERT =
            "uniform mat4 uMVP;attribute vec4 vPos;attribute vec4 vCol;varying vec4 fCol;" +
            "void main(){gl_Position=uMVP*vPos;fCol=vCol;}";
    private static final String FRAG =
            "precision mediump float;varying vec4 fCol;void main(){gl_FragColor=fCol;}";

    // Character anatomy — mirrors GameRenderer's constants exactly
    private static final float HIP_Y=0.93f, LEG_OX=0.18f, LEG_SX=0.167f, LEG_SY=0.333f, LEG_LEN=0.60f;
    private static final float BOOT_SX=0.210f, BOOT_SY=0.183f;
    private static final float TORSO_SX=0.400f, TORSO_SY=0.361f, TORSO_SZ=0.222f;
    private static final float SHOULDER_Y=1.58f, ARM_OX=0.52f, ARM_SX=0.167f, ARM_SY=0.444f, ARM_LEN=0.80f;
    private static final float FIST_SX=0.167f, FIST_SY=0.133f;
    private static final float HEAD_Y=1.58f, HEAD_SX=0.306f, HEAD_SY=0.306f, HAIR_Y=2.13f;

    // Colors
    private static final float[] SKIN ={0.85f,0.70f,0.55f,1f};
    private static final float[] TACT ={0.10f,0.10f,0.11f,1f};
    private static final float[] BOOT ={0.32f,0.33f,0.36f,1f};
    private static final float[] HAIR ={0.07f,0.06f,0.06f,1f};
    private static final float[] MD   ={0.16f,0.16f,0.18f,1f};
    private static final float[] MM   ={0.32f,0.32f,0.35f,1f};
    private static final float[] GRP  ={0.08f,0.08f,0.08f,1f};
    private static final float[] WOOD ={0.55f,0.35f,0.15f,1f};

    private int program;
    private VoxelCube cube;

    private final float[] proj = new float[16];
    private final float[] view = new float[16];
    private final float[] vp   = new float[16];
    private final float[] charM= new float[16];
    private final float[] partM= new float[16];
    private final float[] mvp  = new float[16];
    private final float[] wB   = new float[16];
    private final float[] wT   = new float[16];
    private final float[] wMvp = new float[16];

    private float yaw = 0f;
    private float animTime = 0f;

    public OperatorRenderer(Context ctx) {}

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig cfg){
        GLES20.glClearColor(0.06f,0.06f,0.08f,1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        int vs=sh(GLES20.GL_VERTEX_SHADER,VERT), fs=sh(GLES20.GL_FRAGMENT_SHADER,FRAG);
        program=GLES20.glCreateProgram();
        GLES20.glAttachShader(program,vs); GLES20.glAttachShader(program,fs);
        GLES20.glLinkProgram(program);
        cube=new VoxelCube();
    }

    @Override public void onSurfaceChanged(GL10 gl, int w, int h){
        GLES20.glViewport(0,0,w,h);
        Matrix.perspectiveM(proj,0,60f,(float)w/h,0.5f,20f);
    }

    @Override public void onDrawFrame(GL10 gl){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        animTime++;
        yaw += 0.8f;

        // Camera: slightly above and in front of character centre
        Matrix.setLookAtM(view,0, 0f,3.8f,6.5f, 0f,1.2f,0f, 0f,1f,0f);
        Matrix.multiplyMM(vp,0,proj,0,view,0);

        GLES20.glUseProgram(program);
        drawOperator();
    }

    private void drawOperator(){
        Matrix.setIdentityM(charM,0);
        // Centre at origin, standing on y=0 (= GROUND_Y in game, but here the
        // operator floats at y=0 since there's no ground to stand on).
        // Shift down so feet are at y=0 and head is up.
        Matrix.translateM(charM,0,0f,0f,0f);
        Matrix.rotateM(charM,0,yaw,0,1,0);

        // Boots
        sBox(-LEG_OX,HIP_Y,0, 0, LEG_LEN, BOOT_SX,BOOT_SY,BOOT_SX, BOOT);
        sBox( LEG_OX,HIP_Y,0, 0, LEG_LEN, BOOT_SX,BOOT_SY,BOOT_SX, BOOT);
        // Legs
        sPart(-LEG_OX,HIP_Y,0, 0,LEG_SX,LEG_SY,LEG_SX, TACT);
        sPart( LEG_OX,HIP_Y,0, 0,LEG_SX,LEG_SY,LEG_SX, TACT);
        // Torso
        part(0,HIP_Y,0, TORSO_SX,TORSO_SY,TORSO_SZ, TACT);
        // Arms — right arm forward (rifle hold pose), left arm supporting
        float rightArmAngle = -28f; // tilted forward
        float leftArmAngle  =  15f;
        sPart(-ARM_OX,SHOULDER_Y,0, leftArmAngle,  ARM_SX,ARM_SY,ARM_SX, TACT);
        sPart( ARM_OX,SHOULDER_Y,0, rightArmAngle, ARM_SX,ARM_SY,ARM_SX, TACT);
        // Fists
        sBox(-ARM_OX,SHOULDER_Y,0, leftArmAngle,   ARM_LEN, FIST_SX,FIST_SY,FIST_SX, SKIN);
        sBox( ARM_OX,SHOULDER_Y,0, rightArmAngle,  ARM_LEN, FIST_SX,FIST_SY,FIST_SX, SKIN);
        // Head
        part(0,HEAD_Y,0, HEAD_SX,HEAD_SY,HEAD_SX, SKIN);
        // Hair
        float hy=HAIR_Y;
        part(0,    hy,    -0.05f,0.10f,0.14f,0.08f, HAIR);
        part(-0.14f,hy+0.02f,0f, 0.08f,0.11f,0.07f, HAIR);
        part( 0.14f,hy+0.01f,0.02f,0.08f,0.12f,0.07f,HAIR);
        part( 0.05f,hy+0.04f,-0.11f,0.07f,0.10f,0.06f,HAIR);
        part(0,HEAD_Y+HEAD_SY*1.8f-0.04f,0, 0.27f,0.028f,0.27f, HAIR);
        // AK-47 in right hand
        drawAK47(rightArmAngle);
    }

    private void drawAK47(float armAngle){
        System.arraycopy(charM,0,wB,0,16);
        Matrix.translateM(wB,0,ARM_OX,SHOULDER_Y,0);
        Matrix.rotateM(wB,0,armAngle,1,0,0);
        Matrix.translateM(wB,0,0.02f,-ARM_LEN-0.02f,0.06f);
        Matrix.rotateM(wB,0,-10,1,0,0);
        wp(0f,0.03f,0.25f, 0.026f,0.026f,0.40f, MD);
        wp(0f,0.07f,0.14f, 0.012f,0.012f,0.22f, MD);
        wp(0f,0.03f,0f,    0.052f,0.060f,0.18f, MM);
        wp(0f,-0.08f,0.06f,0.032f,0.11f, 0.040f,MD);
        wp(0f,-0.01f,-0.18f,0.043f,0.050f,0.18f,WOOD);
        wp(0f,-0.09f,-0.06f,0.028f,0.090f,0.035f,GRP);
        wp(0f,0.03f,0.43f, 0.032f,0.030f,0.042f,MD);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void part(float ox,float oy,float oz,float sx,float sy,float sz,float[] c){
        System.arraycopy(charM,0,partM,0,16);
        Matrix.translateM(partM,0,ox,oy,oz);
        Matrix.scaleM(partM,0,sx,sy,sz);
        Matrix.multiplyMM(mvp,0,vp,0,partM,0);
        cube.draw(program,mvp,c);
    }
    private void sPart(float ox,float py,float oz,float deg,float sx,float sy,float sz,float[] c){
        System.arraycopy(charM,0,partM,0,16);
        Matrix.translateM(partM,0,ox,py,oz);
        Matrix.rotateM(partM,0,deg,1,0,0);
        Matrix.scaleM(partM,0,sx,-sy,sz);
        Matrix.multiplyMM(mvp,0,vp,0,partM,0);
        cube.draw(program,mvp,c);
    }
    private void sBox(float ox,float py,float oz,float deg,float tip,float sx,float sy,float sz,float[] c){
        System.arraycopy(charM,0,partM,0,16);
        Matrix.translateM(partM,0,ox,py,oz);
        Matrix.rotateM(partM,0,deg,1,0,0);
        Matrix.translateM(partM,0,0,-tip,0);
        Matrix.scaleM(partM,0,sx,-sy,sz);
        Matrix.multiplyMM(mvp,0,vp,0,partM,0);
        cube.draw(program,mvp,c);
    }
    private void wp(float ox,float oy,float oz,float sx,float sy,float sz,float[] c){
        System.arraycopy(wB,0,wT,0,16);
        Matrix.translateM(wT,0,ox,oy,oz);
        Matrix.scaleM(wT,0,sx,sy,sz);
        Matrix.multiplyMM(wMvp,0,vp,0,wT,0);
        cube.draw(program,wMvp,c);
    }
    private int sh(int type,String src){
        int s=GLES20.glCreateShader(type);
        GLES20.glShaderSource(s,src); GLES20.glCompileShader(s); return s;
    }
}
