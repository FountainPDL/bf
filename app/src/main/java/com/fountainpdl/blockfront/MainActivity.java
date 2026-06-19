package com.fountainpdl.blockfront;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private GLSurfaceView glSurfaceView;
    private GameRenderer gameRenderer;

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

        // Full-screen drag-to-look. Sits above the GL surface but below the
        // joystick/fire button, so those corners keep their own touches.
        LookSurfaceView lookSurface = new LookSurfaceView(this);
        lookSurface.setOnLookListener((dx, dy) -> gameRenderer.addLookDelta(dx, dy));
        root.addView(lookSurface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView crosshair = new TextView(this);
        crosshair.setText("+");
        crosshair.setTextColor(Color.argb(220, 255, 255, 255));
        crosshair.setTextSize(22f);
        crosshair.setClickable(false);
        crosshair.setFocusable(false);
        FrameLayout.LayoutParams crosshairParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
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

        setContentView(root);
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
