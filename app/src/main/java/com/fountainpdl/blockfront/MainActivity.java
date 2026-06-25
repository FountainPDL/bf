package com.fountainpdl.blockfront;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final long POLL = 80;

    private GLSurfaceView gl;
    private GameRenderer gr;
    private CrosshairView crosshair;
    private TextView ammoText, fuelText, targetsText, timerText;
    private TextView healthText, zoneText, killsText, botsText;
    private TextView pickupText, slot2Text;
    private HudButton weaponBtn;
    private FrameLayout resultOverlay;
    private TextView resultTitle, resultSub;
    private final Handler h = new Handler(Looper.getMainLooper());
    private Runnable poller;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        hideSystemUI();

        String mode = getIntent().getStringExtra("mode"); if (mode==null) mode="sandbox";
        String map  = getIntent().getStringExtra("map");  if (map==null)  map ="grasslands";

        FrameLayout root = new FrameLayout(this);

        gl = new GLSurfaceView(this);
        gl.setEGLContextClientVersion(2);
        gr = new GameRenderer(this, mode, map);
        gl.setRenderer(gr);
        gl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(gl, mp());

        LookSurfaceView lv = new LookSurfaceView(this);
        lv.setOnLookListener((dx,dy)->gr.addLookDelta(dx,dy));
        root.addView(lv, mp());

        crosshair=new CrosshairView(this); FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(64,64); cp.gravity=Gravity.CENTER; root.addView(crosshair,cp);

        targetsText=hlab(Color.rgb(212,175,55)); root.addView(targetsText, tc(36));
        timerText  =hlab(Color.WHITE);           root.addView(timerText,   tc(66));
        healthText =hlab(Color.rgb(220,70,60));  root.addView(healthText,  tc(36));
        zoneText   =hlab(Color.rgb(190,120,220));root.addView(zoneText,    tc(66));
        killsText  =hlab(Color.rgb(212,175,55)); root.addView(killsText,   tc(96));
        botsText   =hlab(Color.WHITE);           root.addView(botsText,    tc(126));

        pickupText =hlab(Color.rgb(255,220,60)); root.addView(pickupText, tc(160));

        // Weapon slot display (bottom-left above joystick)
        slot2Text = new TextView(this);
        slot2Text.setTextColor(Color.argb(200,180,180,180));
        slot2Text.setTextSize(13f);
        FrameLayout.LayoutParams s2p=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        s2p.gravity=Gravity.BOTTOM|Gravity.START; s2p.setMargins(40,0,0,340);
        slot2Text.setLayoutParams(s2p);
        root.addView(slot2Text);

        weaponBtn=new HudButton(this,gr.getWeaponName());
        FrameLayout.LayoutParams wp2=new FrameLayout.LayoutParams(170,80); wp2.gravity=Gravity.TOP|Gravity.START; wp2.setMargins(40,40,0,0);
        weaponBtn.setLayoutParams(wp2);
        weaponBtn.setOnTapListener(()->gl.queueEvent(gr::switchWeapon));
        root.addView(weaponBtn);

        HudButton viewBtn=new HudButton(this,"VIEW");
        FrameLayout.LayoutParams vp3=new FrameLayout.LayoutParams(160,80); vp3.gravity=Gravity.TOP|Gravity.END; vp3.setMargins(0,40,40,0);
        viewBtn.setLayoutParams(vp3);
        viewBtn.setOnTapListener(()->gl.queueEvent(gr::toggleViewMode));
        root.addView(viewBtn);

        TouchJoystick stick=new TouchJoystick(this);
        FrameLayout.LayoutParams sp3=new FrameLayout.LayoutParams(280,280); sp3.gravity=Gravity.BOTTOM|Gravity.START; sp3.setMargins(40,0,0,40);
        stick.setLayoutParams(sp3);
        stick.setOnMoveListener((dx,dy)->gr.setMoveInput(dx,dy));
        root.addView(stick);

        HudButton jumpBtn=new HudButton(this,"JUMP");
        FrameLayout.LayoutParams jp3=new FrameLayout.LayoutParams(130,100); jp3.gravity=Gravity.BOTTOM|Gravity.START; jp3.setMargins(300,0,0,340);
        jumpBtn.setLayoutParams(jp3);
        jumpBtn.setOnPressListener(()->gl.queueEvent(gr::onThrustPress));
        jumpBtn.setOnReleaseListener(()->gl.queueEvent(gr::onThrustRelease));
        root.addView(jumpBtn);

        fuelText=new TextView(this); fuelText.setTextColor(Color.rgb(212,175,55)); fuelText.setTextSize(13f);
        FrameLayout.LayoutParams fp3=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        fp3.gravity=Gravity.BOTTOM|Gravity.START; fp3.setMargins(300,0,0,446); fuelText.setLayoutParams(fp3);
        root.addView(fuelText);

        HudButton reloadBtn=new HudButton(this,"RELOAD");
        FrameLayout.LayoutParams rp3=new FrameLayout.LayoutParams(150,100); rp3.gravity=Gravity.BOTTOM|Gravity.END; rp3.setMargins(0,0,226,40);
        reloadBtn.setLayoutParams(rp3);
        reloadBtn.setOnTapListener(()->gl.queueEvent(gr::manualReload));
        root.addView(reloadBtn);

        FireButton fire=new FireButton(this);
        FrameLayout.LayoutParams ff=new FrameLayout.LayoutParams(170,170); ff.gravity=Gravity.BOTTOM|Gravity.END; ff.setMargins(0,0,40,40);
        fire.setLayoutParams(ff);
        fire.setOnFireListener(()->gl.queueEvent(()->gr.tryShoot()));
        root.addView(fire);

        ammoText=new TextView(this); ammoText.setTextColor(Color.rgb(212,175,55)); ammoText.setTextSize(16f); ammoText.getPaint().setFakeBoldText(true);
        FrameLayout.LayoutParams ap3=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        ap3.gravity=Gravity.BOTTOM|Gravity.END; ap3.setMargins(0,0,50,230); ammoText.setLayoutParams(ap3);
        root.addView(ammoText);

        resultOverlay=new FrameLayout(this); resultOverlay.setBackgroundColor(Color.argb(215,0,0,0)); resultOverlay.setVisibility(View.GONE);
        root.addView(resultOverlay, mp());

        LinearLayout rc=new LinearLayout(this); rc.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams rcp=new FrameLayout.LayoutParams(560,FrameLayout.LayoutParams.WRAP_CONTENT); rcp.gravity=Gravity.CENTER;
        resultOverlay.addView(rc,rcp);

        resultTitle=new TextView(this); resultTitle.setTextSize(30f); resultTitle.getPaint().setFakeBoldText(true); resultTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        rc.addView(resultTitle,lp(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.WRAP_CONTENT,0));
        resultSub=new TextView(this); resultSub.setTextColor(Color.WHITE); resultSub.setTextSize(16f); resultSub.setGravity(Gravity.CENTER_HORIZONTAL);
        rc.addView(resultSub,lp(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.WRAP_CONTENT,18));

        HudButton retryBtn=new HudButton(this,"RETRY");
        retryBtn.setLayoutParams(lp(FrameLayout.LayoutParams.MATCH_PARENT,110,50));
        retryBtn.setOnTapListener(()->{Intent i=getIntent();finish();startActivity(i);});
        rc.addView(retryBtn);

        HudButton menuBtn=new HudButton(this,"MENU");
        menuBtn.setLayoutParams(lp(FrameLayout.LayoutParams.MATCH_PARENT,110,20));
        menuBtn.setOnTapListener(()->{startActivity(new Intent(this,MenuActivity.class));finish();});
        rc.addView(menuBtn);

        setContentView(root);

        poller=()->{
            boolean demo=gr.isDemolitionMode(), br=gr.isBattleRoyaleMode(), ffa=gr.isFfaMode();
            String wname=gr.getWeaponName();
            ammoText.setText(gr.getCurrentAmmo()<0?"":gr.isReloading()?"RELOADING…":gr.getCurrentAmmo()+" / "+gr.getMagSize());
            if (wname.equals("UNARMED")) ammoText.setText("UNARMED");
            crosshair.setHit(gr.isRecentHit());
            weaponBtn.setLabel(wname);
            fuelText.setText("JET "+((int)(gr.getFuelPercent()*100))+"%");
            String s2=gr.getSecondWeaponName();
            slot2Text.setText(s2.isEmpty()?"":("2: "+s2));
            String pm=gr.getPickupMessage();
            pickupText.setText(pm);
            vis(pickupText,!pm.isEmpty());

            vis(targetsText,demo); vis(timerText,demo||ffa);
            vis(healthText,br||ffa); vis(zoneText,br);
            vis(killsText,br||ffa); vis(botsText,br||ffa);

            if (demo){
                targetsText.setText("TARGETS: "+gr.getTargetsRemaining());
                timerText.setText(fmt(gr.getDemoTimeMs()));
                if (gr.isGameWon()&&resultOverlay.getVisibility()!=View.VISIBLE)
                    showResult("MISSION COMPLETE",Color.rgb(212,175,55),"Cleared in "+fmt(gr.getCompletionMs()));
                else if (gr.isGameLost()&&resultOverlay.getVisibility()!=View.VISIBLE)
                    showResult("TIME'S UP",Color.rgb(200,60,50),gr.getTargetsRemaining()+" targets left");
            }
            if (br||ffa){
                healthText.setText("HP: "+(int)(gr.getPlayerHealthPercent()*100));
                killsText.setText("KILLS: "+gr.getKills());
                botsText.setText("ENEMIES: "+gr.getBotsRemaining());
            }
            if (br){
                zoneText.setText(gr.isInDangerZone()?"⚠ OUTSIDE ZONE":"ZONE SAFE");
                if (gr.isPlayerEliminated()&&resultOverlay.getVisibility()!=View.VISIBLE)
                    showResult("ELIMINATED",Color.rgb(200,60,50),"Survived "+fmt(gr.getSurvivalMs())+" · "+gr.getKills()+" kills");
                else if (gr.isBrSurvived()&&resultOverlay.getVisibility()!=View.VISIBLE)
                    showResult("ZONE SURVIVED",Color.rgb(212,175,55),gr.getKills()+" kills · held the final circle");
            }
            if (ffa){
                timerText.setText(fmt(gr.getModeTimeLeftMs()));
                if (gr.isPlayerEliminated()&&resultOverlay.getVisibility()!=View.VISIBLE)
                    showResult("ELIMINATED",Color.rgb(200,60,50),gr.getKills()+" kills before you went down");
                else if (gr.isFfaAllDown()&&resultOverlay.getVisibility()!=View.VISIBLE)
                    showResult("ALL DOWN",Color.rgb(212,175,55),gr.getKills()+" kills · cleared the field");
                else if (gr.isFfaTimeUp()&&resultOverlay.getVisibility()!=View.VISIBLE)
                    showResult("TIME'S UP",Color.WHITE,"Final: "+gr.getKills()+" kills");
            }
            h.postDelayed(poller, POLL);
        };
        h.post(poller);
    }

    private FrameLayout.LayoutParams mp(){ return new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT); }
    private TextView hlab(int col){ TextView t=new TextView(this); t.setTextColor(col); t.setTextSize(15f); t.getPaint().setFakeBoldText(true); t.setVisibility(View.GONE); return t; }
    private FrameLayout.LayoutParams tc(int top){ FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT); p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL; p.topMargin=top; return p; }
    private LinearLayout.LayoutParams lp(int w,int hh,int top){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,hh); p.topMargin=top; return p; }
    private void vis(View v,boolean show){ v.setVisibility(show?View.VISIBLE:View.GONE); }
    private void showResult(String t,int col,String sub){ resultTitle.setText(t); resultTitle.setTextColor(col); resultSub.setText(sub); resultOverlay.setVisibility(View.VISIBLE); }
    private String fmt(long ms){ long s=ms/1000; return (s/60)+":"+ String.format("%02d",s%60); }

    @Override protected void onResume(){ super.onResume(); gl.onResume(); }
    @Override protected void onPause(){ super.onPause(); gl.onPause(); }
    @Override protected void onDestroy(){ super.onDestroy(); if(poller!=null) h.removeCallbacks(poller); }
    @Override public void onWindowFocusChanged(boolean f){ super.onWindowFocusChanged(f); if(f) hideSystemUI(); }
    private void hideSystemUI(){ getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY); }
}
