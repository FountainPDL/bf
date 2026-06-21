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
    private TextView ammoText;
    private TextView fuelText;
    private TextView targetsText;
    private TextView timerText;
    private TextView healthText;
    private TextView zoneText;
    private HudButton weaponButton;
    private FrameLayout resultOverlay;
    private TextView resultTitle;
    private TextView resultSubtitle;
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
        root.addView(glSurfaceView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LookSurfaceView lookSurface = new LookSurfaceView(this);
        lookSurface.setOnLookListener((dx, dy) -> gameRenderer.addLookDelta(dx, dy));
        root.addView(lookSurface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        crosshair = new CrosshairView(this);
        FrameLayout.LayoutParams crosshairParams = new FrameLayout.LayoutParams(64, 64);
        crosshairParams.gravity = Gravity.CENTER;
        root.addView(crosshair, crosshairParams);

        targetsText = hudLabel(Color.rgb(212, 175, 55));
        FrameLayout.LayoutParams targetsParams = topCenterParams(36);
        root.addView(targetsText, targetsParams);

        timerText = hudLabel(Color.WHITE);
        root.addView(timerText, topCenterParams(66));

        healthText = hudLabel(Color.rgb(220, 70, 60));
        root.addView(healthText, topCenterParams(36));

        zoneText = hudLabel(Color.rgb(190, 120, 220));
        root.addView(zoneText, topCenterParams(66));

        // --- Top-left: weapon name / switch ---
        weaponButton = new HudButton(this, gameRenderer.getWeaponName());
        FrameLayout.LayoutParams weaponParams = new FrameLayout.LayoutParams(170, 80);
        weaponParams.gravity = Gravity.TOP | Gravity.START;
        weaponParams.setMargins(40, 40, 0, 0);
        weaponButton.setLayoutParams(weaponParams);
        weaponButton.setOnTapListener(() -> glSurfaceView.queueEvent(gameRenderer::switchWeapon));
        root.addView(weaponButton);

        // --- Top-right: view toggle ---
        HudButton viewToggle = new HudButton(this, "VIEW");
        FrameLayout.LayoutParams viewToggleParams = new FrameLayout.LayoutParams(160, 80);
        viewToggleParams.gravity = Gravity.TOP | Gravity.END;
        viewToggleParams.setMargins(0, 40, 40, 0);
        viewToggle.setLayoutParams(viewToggleParams);
        viewToggle.setOnTapListener(() -> glSurfaceView.queueEvent(gameRenderer::toggleViewMode));
        root.addView(viewToggle);

        // --- Bottom-left: joystick + jetpack (tap=hop, hold=fly) ---
        TouchJoystick moveStick = new TouchJoystick(this);
        FrameLayout.LayoutParams stickParams = new FrameLayout.LayoutParams(280, 280);
        stickParams.gravity = Gravity.BOTTOM | Gravity.START;
        stickParams.setMargins(40, 0, 0, 40);
        moveStick.setLayoutParams(stickParams);
        moveStick.setOnMoveListener((dx, dy) -> gameRenderer.setMoveInput(dx, dy));
        root.addView(moveStick);

        HudButton jumpButton = new HudButton(this, "JUMP");
        FrameLayout.LayoutParams jumpParams = new FrameLayout.LayoutParams(130, 100);
        jumpParams.gravity = Gravity.BOTTOM | Gravity.START;
        jumpParams.setMargins(300, 0, 0, 340);
        jumpButton.setLayoutParams(jumpParams);
        jumpButton.setOnPressListener(() -> glSurfaceView.queueEvent(gameRenderer::onThrustPress));
        jumpButton.setOnReleaseListener(() -> glSurfaceView.queueEvent(gameRenderer::onThrustRelease));
        root.addView(jumpButton);

        fuelText = new TextView(this);
        fuelText.setTextColor(Color.rgb(212, 175, 55));
        fuelText.setTextSize(13f);
        FrameLayout.LayoutParams fuelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        fuelParams.gravity = Gravity.BOTTOM | Gravity.START;
        fuelParams.setMargins(300, 0, 0, 446);
        fuelText.setLayoutParams(fuelParams);
        root.addView(fuelText);

        // --- Bottom-right: reload, fire, ammo ---
        HudButton reloadButton = new HudButton(this, "RELOAD");
        FrameLayout.LayoutParams reloadParams = new FrameLayout.LayoutParams(150, 100);
        reloadParams.gravity = Gravity.BOTTOM | Gravity.END;
        reloadParams.setMargins(0, 0, 226, 40);
        reloadButton.setLayoutParams(reloadParams);
        reloadButton.setOnTapListener(() -> glSurfaceView.queueEvent(gameRenderer::manualReload));
        root.addView(reloadButton);

        FireButton fireButton = new FireButton(this);
        FrameLayout.LayoutParams fireParams = new FrameLayout.LayoutParams(170, 170);
        fireParams.gravity = Gravity.BOTTOM | Gravity.END;
        fireParams.setMargins(0, 0, 40, 40);
        fireButton.setLayoutParams(fireParams);
        fireButton.setOnFireListener(() -> glSurfaceView.queueEvent(() -> gameRenderer.tryShoot()));
        root.addView(fireButton);

        ammoText = new TextView(this);
        ammoText.setTextColor(Color.rgb(212, 175, 55));
        ammoText.setTextSize(16f);
        ammoText.getPaint().setFakeBoldText(true);
        FrameLayout.LayoutParams ammoParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        ammoParams.gravity = Gravity.BOTTOM | Gravity.END;
        ammoParams.setMargins(0, 0, 50, 230);
        ammoText.setLayoutParams(ammoParams);
        root.addView(ammoText);

        // --- Result overlay (Demolition / Battle Royale end-of-match) ---
        resultOverlay = new FrameLayout(this);
        resultOverlay.setBackgroundColor(Color.argb(215, 0, 0, 0));
        resultOverlay.setVisibility(View.GONE);
        root.addView(resultOverlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout resultColumn = new LinearLayout(this);
        resultColumn.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams resultColumnParams = new FrameLayout.LayoutParams(
                560, FrameLayout.LayoutParams.WRAP_CONTENT);
        resultColumnParams.gravity = Gravity.CENTER;
        resultOverlay.addView(resultColumn, resultColumnParams);

        resultTitle = new TextView(this);
        resultTitle.setTextSize(30f);
        resultTitle.getPaint().setFakeBoldText(true);
        resultTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        resultColumn.addView(resultTitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        resultSubtitle = new TextView(this);
        resultSubtitle.setTextColor(Color.WHITE);
        resultSubtitle.setTextSize(16f);
        resultSubtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = 18;
        resultColumn.addView(resultSubtitle, subParams);

        HudButton retryButton = new HudButton(this, "RETRY");
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 110);
        retryParams.topMargin = 50;
        retryButton.setLayoutParams(retryParams);
        retryButton.setOnTapListener(() -> {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        });
        resultColumn.addView(retryButton);

        HudButton menuButton = new HudButton(this, "MENU");
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 110);
        menuParams.topMargin = 20;
        menuButton.setLayoutParams(menuParams);
        menuButton.setOnTapListener(() -> {
            startActivity(new Intent(this, MenuActivity.class));
            finish();
        });
        resultColumn.addView(menuButton);

        setContentView(root);

        hudPoller = () -> {
            if (gameRenderer.isReloading()) {
                ammoText.setText("RELOADING…");
            } else {
                ammoText.setText(gameRenderer.getCurrentAmmo() + " / " + gameRenderer.getMagSize());
            }
            crosshair.setHit(gameRenderer.isRecentHit());
            weaponButton.setLabel(gameRenderer.getWeaponName());
            fuelText.setText("FUEL " + (int) (gameRenderer.getFuelPercent() * 100) + "%");

            targetsText.setVisibility(gameRenderer.isDemolitionMode() ? View.VISIBLE : View.GONE);
            timerText.setVisibility(gameRenderer.isDemolitionMode() ? View.VISIBLE : View.GONE);
            healthText.setVisibility(gameRenderer.isBattleRoyaleMode() ? View.VISIBLE : View.GONE);
            zoneText.setVisibility(gameRenderer.isBattleRoyaleMode() ? View.VISIBLE : View.GONE);

            if (gameRenderer.isDemolitionMode()) {
                targetsText.setText("TARGETS: " + gameRenderer.getTargetsRemaining());
                long remainingMs = gameRenderer.getTimeRemainingMs();
                int totalSeconds = (int) (remainingMs / 1000);
                timerText.setText(String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60));

                if (gameRenderer.isGameWon() && resultOverlay.getVisibility() != View.VISIBLE) {
                    showResult("MISSION COMPLETE", Color.rgb(212, 175, 55),
                            "Cleared in " + formatSeconds(gameRenderer.getCompletionTimeMs()));
                } else if (gameRenderer.isGameLost() && resultOverlay.getVisibility() != View.VISIBLE) {
                    showResult("TIME'S UP", Color.rgb(200, 60, 50),
                            gameRenderer.getTargetsRemaining() + " targets remaining");
                }
            }

            if (gameRenderer.isBattleRoyaleMode()) {
                healthText.setText("HP: " + (int) (gameRenderer.getBrHealthPercent() * 100));
                zoneText.setText(gameRenderer.isInDangerZone() ? "OUTSIDE THE ZONE" : "ZONE SAFE");

                if (gameRenderer.isBrEliminated() && resultOverlay.getVisibility() != View.VISIBLE) {
                    showResult("ELIMINATED", Color.rgb(200, 60, 50),
                            "Survived " + formatSeconds(gameRenderer.getBrSurvivalMs()));
                } else if (gameRenderer.isBrSurvived() && resultOverlay.getVisibility() != View.VISIBLE) {
                    showResult("ZONE SURVIVED", Color.rgb(212, 175, 55),
                            "You held the final circle");
                }
            }

            hudHandler.postDelayed(hudPoller, HUD_POLL_MS);
        };
        hudHandler.post(hudPoller);
    }

    private TextView hudLabel(int color) {
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setTextSize(16f);
        tv.getPaint().setFakeBoldText(true);
        tv.setVisibility(View.GONE);
        return tv;
    }

    private FrameLayout.LayoutParams topCenterParams(int topMargin) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        p.topMargin = topMargin;
        return p;
    }

    private void showResult(String title, int color, String subtitle) {
        resultTitle.setText(title);
        resultTitle.setTextColor(color);
        resultSubtitle.setText(subtitle);
        resultOverlay.setVisibility(View.VISIBLE);
    }

    private String formatSeconds(long ms) {
        long s = ms / 1000;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    protected void onDestroy() {
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
