package com.fountainpdl.blockfront;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Menu operator preview renderer.
 * Loads operator.bin from assets (produced by gen_mesh_data.py).
 * Falls back to the blocky voxel character if the binary is not yet present.
 */
public class OperatorRenderer implements GLSurfaceView.Renderer {

    private static final String FLAT_VERT =
            "uniform mat4 uMVP;attribute vec4 vPos;attribute vec4 vCol;varying vec4 fCol;" +
            "void main(){gl_Position=uMVP*vPos;fCol=vCol;}";
    private static final String FLAT_FRAG =
            "precision mediump float;varying vec4 fCol;void main(){gl_FragColor=fCol;}";

    // Anatomy constants (match GameRenderer)
    private static final float HIP_Y=0.93f,LEG_OX=0.18f,LEG_SX=0.167f,LEG_SY=0.333f,LEG_LEN=0.60f;
    private static final float BOOT_SX=0.210f,BOOT_SY=0.183f,TORSO_SX=0.400f,TORSO_SY=0.361f,TORSO_SZ=0.222f;
    private static final float SHOULDER_Y=1.58f,ARM_OX=0.52f,ARM_SX=0.167f,ARM_SY=0.444f,ARM_LEN=0.80f;
    private static final float FIST_SX=0.167f,FIST_SY=0.133f,HEAD_Y=1.58f,HEAD_SX=0.306f,HEAD_SY=0.306f,HAIR_Y=2.13f;

    private static final float[] SKIN={0.85f,0.70f,0.55f,1f},TACT={0.10f,0.10f,0.11f,1f};
    private static final float[] BOOT={0.32f,0.33f,0.36f,1f},HAIR={0.07f,0.06f,0.06f,1f};
    private static final float[] MD={0.16f,0.16f,0.18f,1f},MM={0.32f,0.32f,0.35f,1f};
    private static final float[] GRP={0.08f,0.08f,0.08f,1f},WOOD={0.55f,0.35f,0.15f,1f};
    private static final float[] TINT={1f,1f,1f,1f};

    private final Context ctx;
    private int flatProgram;
    private VoxelCube cube;
    private ObjMesh operatorMesh;
    private TexturedShader texShader;
    private boolean hasObjMesh = false;

    private final float[] proj=new float[16],view=new float[16],vp=new float[16];
    private final float[] charM=new float[16],partM=new float[16],mvp=new float[16];
    private final float[] wB=new float[16],wT=new float[16],wMvp=new float[16];
    private float yaw=0f;

    public OperatorRenderer(Context ctx){ this.ctx=ctx; }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig cfg){
        GLES20.glClearColor(0.05f,0.05f,0.07f,1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        flatProgram=buildProg(FLAT_VERT,FLAT_FRAG);
        cube=new VoxelCube();
        // Try loading actual OBJ binary — only present after gen_mesh_data.py is run
        try {
            operatorMesh=ObjMesh.fromAsset(ctx,"operator.bin");
            texShader=new TexturedShader(ctx,"operator_tex.jpg");
            hasObjMesh=true;
        } catch (Exception e){
            hasObjMesh=false; // falls back to voxel character
        }
    }

    @Override public void onSurfaceChanged(GL10 gl,int w,int h){
        GLES20.glViewport(0,0,w,h);
        Matrix.perspectiveM(proj,0,60f,(float)w/h,0.5f,20f);
    }

    @Override public void onDrawFrame(GL10 gl){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        yaw+=0.7f;
        if(hasObjMesh){
            Matrix.setLookAtM(view,0,0,0,3.5f,0,0,0,0,1,0);
            Matrix.multiplyMM(vp,0,proj,0,view,0);
            float[]mM=new float[16],fMvp=new float[16];
            Matrix.setIdentityM(mM,0);
            Matrix.rotateM(mM,0,yaw,0,1,0);
            Matrix.multiplyMM(fMvp,0,vp,0,mM,0);
            texShader.bind(TINT);
            operatorMesh.drawTextured(texShader.getProgram(),fMvp);
        } else {
            Matrix.setLookAtM(view,0,0f,3.8f,6.5f,0f,1.2f,0f,0f,1f,0f);
            Matrix.multiplyMM(vp,0,proj,0,view,0);
            GLES20.glUseProgram(flatProgram);
            drawVoxel();
        }
    }

    private void drawVoxel(){
        Matrix.setIdentityM(charM,0);
        Matrix.rotateM(charM,0,yaw,0,1,0);
        float ra=-28f,la=15f;
        sBox(-LEG_OX,HIP_Y,0,0,LEG_LEN,BOOT_SX,BOOT_SY,BOOT_SX,BOOT);
        sBox(LEG_OX,HIP_Y,0,0,LEG_LEN,BOOT_SX,BOOT_SY,BOOT_SX,BOOT);
        sPrt(-LEG_OX,HIP_Y,0,0,LEG_SX,LEG_SY,LEG_SX,TACT);
        sPrt(LEG_OX,HIP_Y,0,0,LEG_SX,LEG_SY,LEG_SX,TACT);
        prt(0,HIP_Y,0,TORSO_SX,TORSO_SY,TORSO_SZ,TACT);
        sPrt(-ARM_OX,SHOULDER_Y,0,la,ARM_SX,ARM_SY,ARM_SX,TACT);
        sPrt(ARM_OX,SHOULDER_Y,0,ra,ARM_SX,ARM_SY,ARM_SX,TACT);
        sBox(-ARM_OX,SHOULDER_Y,0,la,ARM_LEN,FIST_SX,FIST_SY,FIST_SX,SKIN);
        sBox(ARM_OX,SHOULDER_Y,0,ra,ARM_LEN,FIST_SX,FIST_SY,FIST_SX,SKIN);
        prt(0,HEAD_Y,0,HEAD_SX,HEAD_SY,HEAD_SX,SKIN);
        prt(0,HAIR_Y,-0.05f,0.10f,0.14f,0.08f,HAIR);
        prt(-0.14f,HAIR_Y+0.02f,0f,0.08f,0.11f,0.07f,HAIR);
        prt(0.14f,HAIR_Y+0.01f,0.02f,0.08f,0.12f,0.07f,HAIR);
        prt(0,HEAD_Y+HEAD_SY*1.8f-0.04f,0,0.27f,0.028f,0.27f,HAIR);
        System.arraycopy(charM,0,wB,0,16);
        Matrix.translateM(wB,0,ARM_OX,SHOULDER_Y,0);
        Matrix.rotateM(wB,0,ra,1,0,0);
        Matrix.translateM(wB,0,0.02f,-ARM_LEN-0.02f,0.06f);
        Matrix.rotateM(wB,0,-10,1,0,0);
        wp(0f,0.03f,0.25f,0.026f,0.026f,0.40f,MD);
        wp(0f,0.07f,0.14f,0.012f,0.012f,0.22f,MD);
        wp(0f,0.03f,0f,0.052f,0.060f,0.18f,MM);
        wp(0f,-0.08f,0.06f,0.032f,0.11f,0.040f,MD);
        wp(0f,-0.01f,-0.18f,0.043f,0.050f,0.18f,WOOD);
        wp(0f,-0.09f,-0.06f,0.028f,0.090f,0.035f,GRP);
    }

    private void prt(float ox,float oy,float oz,float sx,float sy,float sz,float[]c){
        System.arraycopy(charM,0,partM,0,16);Matrix.translateM(partM,0,ox,oy,oz);Matrix.scaleM(partM,0,sx,sy,sz);Matrix.multiplyMM(mvp,0,vp,0,partM,0);cube.draw(flatProgram,mvp,c);}
    private void sPrt(float ox,float py,float oz,float d,float sx,float sy,float sz,float[]c){
        System.arraycopy(charM,0,partM,0,16);Matrix.translateM(partM,0,ox,py,oz);Matrix.rotateM(partM,0,d,1,0,0);Matrix.scaleM(partM,0,sx,-sy,sz);Matrix.multiplyMM(mvp,0,vp,0,partM,0);cube.draw(flatProgram,mvp,c);}
    private void sBox(float ox,float py,float oz,float d,float tip,float sx,float sy,float sz,float[]c){
        System.arraycopy(charM,0,partM,0,16);Matrix.translateM(partM,0,ox,py,oz);Matrix.rotateM(partM,0,d,1,0,0);Matrix.translateM(partM,0,0,-tip,0);Matrix.scaleM(partM,0,sx,-sy,sz);Matrix.multiplyMM(mvp,0,vp,0,partM,0);cube.draw(flatProgram,mvp,c);}
    private void wp(float ox,float oy,float oz,float sx,float sy,float sz,float[]c){
        System.arraycopy(wB,0,wT,0,16);Matrix.translateM(wT,0,ox,oy,oz);Matrix.scaleM(wT,0,sx,sy,sz);Matrix.multiplyMM(wMvp,0,vp,0,wT,0);cube.draw(flatProgram,wMvp,c);}

    private static int buildProg(String v,String f){
        int vs=sh(GLES20.GL_VERTEX_SHADER,v),fs=sh(GLES20.GL_FRAGMENT_SHADER,f);
        int p=GLES20.glCreateProgram();GLES20.glAttachShader(p,vs);GLES20.glAttachShader(p,fs);GLES20.glLinkProgram(p);return p;}
    private static int sh(int t,String s){int sh=GLES20.glCreateShader(t);GLES20.glShaderSource(sh,s);GLES20.glCompileShader(sh);return sh;}
}

