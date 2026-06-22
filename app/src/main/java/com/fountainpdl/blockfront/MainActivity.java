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

    private static final long HUD_POLL_MS = 80;

    private GLSurfaceView glSurfaceView;
    private GameRenderer gameRenderer;
    private CrosshairView crosshair;
    private TextView ammoText, fuelText, targetsText, timerText;
    private TextView healthText, zoneText, killsText, botsText;
    private HudButton weaponButton;
    private FrameLayout resultOverlay;
    private TextView resultTitle, resultSubtitle;
    private final Handler hudHandler = new Handler(Looper.getMainLooper());
    private Runnable hudPoller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        String mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "sandbox";
        String mapId = getIntent().getStringExtra("map");
        if (mapId == null) mapId = "grasslands";

        FrameLayout root = new FrameLayout(this);

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        gameRenderer = new GameRenderer(this, mode, mapId);
        glSurfaceView.setRenderer(gameRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(glSurfaceView, matchParent());

        LookSurfaceView lookSurface = new LookSurfaceView(this);
        lookSurface.setOnLookListener((dx, dy) -> gameRenderer.addLookDelta(dx, dy));
        root.addView(lookSurface, matchParent());

        crosshair = new CrosshairView(this);
        FrameLayout.LayoutParams chp = new FrameLayout.LayoutParams(64, 64);
        chp.gravity = Gravity.CENTER;
        root.addView(crosshair, chp);

        targetsText = hudLabel(Color.rgb(212, 175, 55));  root.addView(targetsText, topCenter(36));
        timerText   = hudLabel(Color.WHITE);              root.addView(timerText,   topCenter(66));
        healthText  = hudLabel(Color.rgb(220, 70, 60));   root.addView(healthText,  topCenter(36));
        zoneText    = hudLabel(Color.rgb(190, 120, 220)); root.addView(zoneText,    topCenter(66));
        killsText   = hudLabel(Color.rgb(212, 175, 55));  root.addView(killsText,   topCenter(96));
        botsText    = hudLabel(Color.WHITE);              root.addView(botsText,    topCenter(126));

        weaponButton = new HudButton(this, gameRenderer.getWeaponName());
        FrameLayout.LayoutParams wp = new FrameLayout.LayoutParams(170, 80);
        wp.gravity = Gravity.TOP | Gravity.START; wp.setMargins(40,40,0,0);
        weaponButton.setLayoutParams(wp);
        weaponButton.setOnTapListener(() -> glSurfaceView.queueEvent(gameRenderer::switchWeapon));
        root.addView(weaponButton);

        HudButton viewToggle = new HudButton(this, "VIEW");
        FrameLayout.LayoutParams vtp = new FrameLayout.LayoutParams(160, 80);
        vtp.gravity = Gravity.TOP | Gravity.END; vtp.setMargins(0,40,40,0);
        viewToggle.setLayoutParams(vtp);
        viewToggle.setOnTapListener(() -> glSurfaceView.queueEvent(gameRenderer::toggleViewMode));
        root.addView(viewToggle);

        TouchJoystick moveStick = new TouchJoystick(this);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(280,280);
        sp.gravity = Gravity.BOTTOM | Gravity.START; sp.setMargins(40,0,0,40);
        moveStick.setLayoutParams(sp);
        moveStick.setOnMoveListener((dx, dy) -> gameRenderer.setMoveInput(dx, dy));
        root.addView(moveStick);

        HudButton jumpButton = new HudButton(this, "JUMP");
        FrameLayout.LayoutParams jp = new FrameLayout.LayoutParams(130,100);
        jp.gravity = Gravity.BOTTOM | Gravity.START; jp.setMargins(300,0,0,340);
        jumpButton.setLayoutParams(jp);
        jumpButton.setOnPressListener(() -> glSurfaceView.queueEvent(gameRenderer::onThrustPress));
        jumpButton.setOnReleaseListener(() -> glSurfaceView.queueEvent(gameRenderer::onThrustRelease));
        root.addView(jumpButton);

        fuelText = new TextView(this);
        fuelText.setTextColor(Color.rgb(212, 175, 55)); fuelText.setTextSize(13f);
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        fp.gravity = Gravity.BOTTOM | Gravity.START; fp.setMargins(300,0,0,446);
        fuelText.setLayoutParams(fp);
        root.addView(fuelText);

        HudButton reloadButton = new HudButton(this, "RELOAD");
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(150,100);
        rp.gravity = Gravity.BOTTOM | Gravity.END; rp.setMargins(0,0,226,40);
        reloadButton.setLayoutParams(rp);
        reloadButton.setOnTapListener(() -> glSurfaceView.queueEvent(gameRenderer::manualReload));
        root.addView(reloadButton);

        FireButton fireButton = new FireButton(this);
        FrameLayout.LayoutParams ff = new FrameLayout.LayoutParams(170,170);
        ff.gravity = Gravity.BOTTOM | Gravity.END; ff.setMargins(0,0,40,40);
        fireButton.setLayoutParams(ff);
        fireButton.setOnFireListener(() -> glSurfaceView.queueEvent(() -> gameRenderer.tryShoot()));
        root.addView(fireButton);

        ammoText = new TextView(this);
        ammoText.setTextColor(Color.rgb(212,175,55)); ammoText.setTextSize(16f);
        ammoText.getPaint().setFakeBoldText(true);
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        ap.gravity = Gravity.BOTTOM | Gravity.END; ap.setMargins(0,0,50,230);
        ammoText.setLayoutParams(ap);
        root.addView(ammoText);

        resultOverlay = new FrameLayout(this);
        resultOverlay.setBackgroundColor(Color.argb(215,0,0,0));
        resultOverlay.setVisibility(View.GONE);
        root.addView(resultOverlay, matchParent());

        LinearLayout resultColumn = new LinearLayout(this);
        resultColumn.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams rcp = new FrameLayout.LayoutParams(
                560, FrameLayout.LayoutParams.WRAP_CONTENT);
        rcp.gravity = Gravity.CENTER;
        resultOverlay.addView(resultColumn, rcp);

        resultTitle = new TextView(this);
        resultTitle.setTextSize(30f); resultTitle.getPaint().setFakeBoldText(true);
        resultTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        resultColumn.addView(resultTitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        resultSubtitle = new TextView(this);
        resultSubtitle.setTextColor(Color.WHITE); resultSubtitle.setTextSize(16f);
        resultSubtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams subp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subp.topMargin = 18;
        resultColumn.addView(resultSubtitle, subp);

        HudButton retryButton = new HudButton(this, "RETRY");
        LinearLayout.LayoutParams retp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 110);
        retp.topMargin = 50; retryButton.setLayoutParams(retp);
        retryButton.setOnTapListener(() -> { Intent i = getIntent(); finish(); startActivity(i); });
        resultColumn.addView(retryButton);

        HudButton menuButton = new HudButton(this, "MENU");
        LinearLayout.LayoutParams menup = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 110);
        menup.topMargin = 20; menuButton.setLayoutParams(menup);
        menuButton.setOnTapListener(() -> { startActivity(new Intent(this, MenuActivity.class)); finish(); });
        resultColumn.addView(menuButton);

        setContentView(root);

        hudPoller = () -> {
            ammoText.setText(gameRenderer.isReloading() ? "RELOADING\u2026"
                    : gameRenderer.getCurrentAmmo() + " / " + gameRenderer.getMagSize());
            crosshair.setHit(gameRenderer.isRecentHit());
            weaponButton.setLabel(gameRenderer.getWeaponName());
            fuelText.setText("FUEL " + (int)(gameRenderer.getFuelPercent() * 100) + "%");

            boolean isDemo = gameRenderer.isDemolitionMode();
            boolean isBr   = gameRenderer.isBattleRoyaleMode();
            boolean isFfa  = gameRenderer.isFfaMode();

            vis(targetsText, isDemo);
            vis(timerText,   isDemo || isFfa);
            vis(healthText,  isBr || isFfa);
            vis(zoneText,    isBr);
            vis(killsText,   isBr || isFfa);
            vis(botsText,    isBr || isFfa);

            if (isDemo) {
                targetsText.setText("TARGETS: " + gameRenderer.getTargetsRemaining());
                timerText.setText(fmt(gameRenderer.getTimeRemainingMs()));
                if (gameRenderer.isGameWon() && resultOverlay.getVisibility() != View.VISIBLE)
                    showResult("MISSION COMPLETE", Color.rgb(212,175,55),
                            "Cleared in " + fmt(gameRenderer.getCompletionTimeMs()));
                else if (gameRenderer.isGameLost() && resultOverlay.getVisibility() != View.VISIBLE)
                    showResult("TIME'S UP", Color.rgb(200,60,50),
                            gameRenderer.getTargetsRemaining() + " targets remaining");
            }

            if (isBr || isFfa) {
                healthText.setText("HP: " + (int)(gameRenderer.getPlayerHealthPercent() * 100));
                killsText.setText("KILLS: " + gameRenderer.getKillCount());
                botsText.setText("ENEMIES: " + gameRenderer.getBotsRemaining());
            }

            if (isBr) {
                zoneText.setText(gameRenderer.isInDangerZone() ? "OUTSIDE THE ZONE" : "ZONE SAFE");
                if (gameRenderer.isPlayerEliminated() && resultOverlay.getVisibility() != View.VISIBLE)
                    showResult("ELIMINATED", Color.rgb(200,60,50),
                            "Survived " + fmt(gameRenderer.getSurvivalMs())
                                    + " \u00b7 " + gameRenderer.getKillCount() + " kills");
                else if (gameRenderer.isBrSurvived() && resultOverlay.getVisibility() != View.VISIBLE)
                    showResult("ZONE SURVIVED", Color.rgb(212,175,55),
                            gameRenderer.getKillCount() + " kills \u00b7 held the final circle");
            }

            if (isFfa) {
                timerText.setText(fmt(gameRenderer.getModeTimeRemainingMs()));
                if (gameRenderer.isPlayerEliminated() && resultOverlay.getVisibility() != View.VISIBLE)
                    showResult("ELIMINATED", Color.rgb(200,60,50),
                            gameRenderer.getKillCount() + " kills before you went down");
                else if (gameRenderer.isFfaAllEliminated() && resultOverlay.getVisibility() != View.VISIBLE)
                    showResult("ALL ENEMIES DOWN", Color.rgb(212,175,55),
                            gameRenderer.getKillCount() + " kills \u00b7 cleared the field");
                else if (gameRenderer.isFfaTimeUp() && resultOverlay.getVisibility() != View.VISIBLE)
                    showResult("TIME'S UP", Color.WHITE,
                            "Final score: " + gameRenderer.getKillCount() + " kills");
            }

            hudHandler.postDelayed(hudPoller, HUD_POLL_MS);
        };
        hudHandler.post(hudPoller);
    }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }

    private TextView hudLabel(int color) {
        TextView tv = new TextView(this);
        tv.setTextColor(color); tv.setTextSize(15f);
        tv.getPaint().setFakeBoldText(true);
        tv.setVisibility(View.GONE);
        return tv;
    }

    private FrameLayout.LayoutParams topCenter(int topMargin) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        p.topMargin = topMargin;
        return p;
    }

    private void vis(View v, boolean show) {
        v.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showResult(String title, int color, String subtitle) {
        resultTitle.setText(title); resultTitle.setTextColor(color);
        resultSubtitle.setText(subtitle);
        resultOverlay.setVisibility(View.VISIBLE);
    }

    private String fmt(long ms) {
        long s = ms / 1000;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    @Override protected void onResume() { super.onResume(); glSurfaceView.onResume(); }
    @Override protected void onPause()  { super.onPause();  glSurfaceView.onPause();  }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (hudPoller != null) hudHandler.removeCallbacks(hudPoller);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void hideSystemUI() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}

