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
    private TextView targetsText;
    private TextView timerText;
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

        FrameLayout root = new FrameLayout(this);

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        gameRenderer = new GameRenderer(this, mode);
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

        targetsText = new TextView(this);
        targetsText.setTextColor(Color.rgb(212, 175, 55));
        targetsText.setTextSize(16f);
        targetsText.getPaint().setFakeBoldText(true);
        targetsText.setVisibility(View.GONE);
        FrameLayout.LayoutParams targetsParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        targetsParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        targetsParams.topMargin = 36;
        root.addView(targetsText, targetsParams);

        timerText = new TextView(this);
        timerText.setTextColor(Color.WHITE);
        timerText.setTextSize(16f);
        timerText.getPaint().setFakeBoldText(true);
        timerText.setVisibility(View.GONE);
        FrameLayout.LayoutParams timerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        timerParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        timerParams.topMargin = 66;
        root.addView(timerText, timerParams);

        TouchJoystick moveStick = new TouchJoystick(this);
        FrameLayout.LayoutParams stickParams = new FrameLayout.LayoutParams(280, 280);
        stickParams.gravity = Gravity.BOTTOM | Gravity.START;
        stickParams.setMargins(40, 0, 0, 40);
        moveStick.setLayoutParams(stickParams);
        moveStick.setOnMoveListener((dx, dy) -> gameRenderer.setMoveInput(dx, dy));
        root.addView(moveStick);

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

        HudButton viewToggle = new HudButton(this, "VIEW");
        FrameLayout.LayoutParams viewToggleParams = new FrameLayout.LayoutParams(160, 80);
        viewToggleParams.gravity = Gravity.TOP | Gravity.END;
        viewToggleParams.setMargins(0, 40, 40, 0);
        viewToggle.setLayoutParams(viewToggleParams);
        viewToggle.setOnTapListener(() -> glSurfaceView.queueEvent(() -> gameRenderer.toggleViewMode()));
        root.addView(viewToggle);

        // --- Result overlay (Demolition win/lose) ---
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

            if (gameRenderer.isDemolitionMode()) {
                targetsText.setVisibility(View.VISIBLE);
                timerText.setVisibility(View.VISIBLE);
                targetsText.setText("TARGETS: " + gameRenderer.getTargetsRemaining());

                long remainingMs = gameRenderer.getTimeRemainingMs();
                int totalSeconds = (int) (remainingMs / 1000);
                timerText.setText(String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60));

                if (gameRenderer.isGameWon() && resultOverlay.getVisibility() != View.VISIBLE) {
                    resultTitle.setText("MISSION COMPLETE");
                    resultTitle.setTextColor(Color.rgb(212, 175, 55));
                    long t = gameRenderer.getCompletionTimeMs() / 1000;
                    resultSubtitle.setText("Cleared in " + (t / 60) + ":" + String.format("%02d", t % 60));
                    resultOverlay.setVisibility(View.VISIBLE);
                } else if (gameRenderer.isGameLost() && resultOverlay.getVisibility() != View.VISIBLE) {
                    resultTitle.setText("TIME'S UP");
                    resultTitle.setTextColor(Color.rgb(200, 60, 50));
                    resultSubtitle.setText(gameRenderer.getTargetsRemaining() + " targets remaining");
                    resultOverlay.setVisibility(View.VISIBLE);
                }
            } else {
                targetsText.setVisibility(View.GONE);
                timerText.setVisibility(View.GONE);
            }

            hudHandler.postDelayed(hudPoller, HUD_POLL_MS);
        };
        hudHandler.post(hudPoller);
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
