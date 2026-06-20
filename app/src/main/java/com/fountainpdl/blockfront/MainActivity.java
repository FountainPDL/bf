package com.fountainpdl.blockfront;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final long HUD_POLL_MS = 80;

    private GLSurfaceView glSurfaceView;
    private GameRenderer gameRenderer;
    private CrosshairView crosshair;
    private TextView ammoText;
    private final Handler hudHandler = new Handler(Looper.getMainLooper());
    private Runnable hudPoller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        FrameLayout root = new FrameLayout(this);

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        gameRenderer = new GameRenderer(this);
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

        setContentView(root);

        hudPoller = () -> {
            if (gameRenderer.isReloading()) {
                ammoText.setText("RELOADING…");
            } else {
                ammoText.setText(gameRenderer.getCurrentAmmo() + " / " + gameRenderer.getMagSize());
            }
            crosshair.setHit(gameRenderer.isRecentHit());
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
