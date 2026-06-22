package com.fountainpdl.blockfront;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends Activity {

    private static final long SPLASH_DURATION_MS = 5000;

    private ValueAnimator glowAnim;
    private ValueAnimator progressAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(6, 7, 13));

        CyberpunkSplashView backdrop = new CyberpunkSplashView(this);
        root.addView(backdrop, matchParent());

        addCornerHud(root);

        LinearLayout centerColumn = new LinearLayout(this);
        centerColumn.setOrientation(LinearLayout.VERTICAL);
        centerColumn.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams ccp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        ccp.gravity = Gravity.CENTER;
        ccp.topMargin = (int)(getResources().getDisplayMetrics().heightPixels * 0.12f);
        root.addView(centerColumn, ccp);

        TextView brand1 = makeText("FOUNTAINPDL", 46f, Color.rgb(0, 210, 255), true);
        brand1.setLetterSpacing(0.10f);
        brand1.setAlpha(0f);
        centerColumn.addView(brand1);

        TextView brand2 = makeText("MINISTRIES", 38f, Color.rgb(212, 175, 55), true);
        brand2.setLetterSpacing(0.14f);
        brand2.setAlpha(0f);
        centerColumn.addView(brand2);

        View divider = new View(this);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 2);
        dp.topMargin = 12; dp.bottomMargin = 12;
        divider.setLayoutParams(dp);
        divider.setBackgroundColor(Color.argb(140, 0, 180, 220));
        divider.setAlpha(0f);
        centerColumn.addView(divider);

        TextView productionLine = makeText("A FOUNTAINPDL PRODUCTION", 13f,
                Color.argb(200, 255, 255, 255), false);
        productionLine.setLetterSpacing(0.18f);
        productionLine.setAlpha(0f);
        centerColumn.addView(productionLine);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            brand1.animate().alpha(1f).translationY(0f).setDuration(500).start();
        }, 350);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            brand2.animate().alpha(1f).setDuration(400).start();
        }, 700);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            divider.animate().alpha(1f).setDuration(300).start();
            productionLine.animate().alpha(1f).setDuration(500).start();
            startGlowLoop(brand1, brand2);
        }, 1000);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MenuActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION_MS);

        setContentView(root);
    }

    private void addCornerHud(FrameLayout root) {
        int pad = 24;

        root.addView(cornerText("SYSTEM ACTIVE v1.0", Color.rgb(0,200,255)),
                corner(Gravity.TOP | Gravity.START, pad, pad, 0, 0));
        root.addView(cornerText("CONNECTING...", Color.rgb(0,200,255)),
                corner(Gravity.TOP | Gravity.END, 0, pad, pad, 0));

        root.addView(cornerText("INITIALIZING FOUNTAINPDL_CORE",
                Color.argb(190, 0,170,210)),
                corner(Gravity.BOTTOM | Gravity.START, pad, 0, 0, 52));

        FrameLayout loadingContainer = new FrameLayout(this);
        FrameLayout.LayoutParams lcp = corner(Gravity.BOTTOM | Gravity.END, 0, 0, pad, 44);
        lcp.width = 300;
        root.addView(loadingContainer, lcp);

        TextView loadingLabel = new TextView(this);
        loadingLabel.setTextColor(Color.rgb(0, 200, 255));
        loadingLabel.setTextSize(11f);
        loadingLabel.setTypeface(Typeface.MONOSPACE);
        loadingLabel.setGravity(Gravity.END);
        loadingContainer.addView(loadingLabel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END));

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(0);
        FrameLayout.LayoutParams barp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 6, Gravity.BOTTOM);
        barp.topMargin = 22;
        loadingContainer.addView(bar, barp);

        progressAnim = ValueAnimator.ofInt(0, 100);
        progressAnim.setDuration(4500);
        progressAnim.setInterpolator(new LinearInterpolator());
        progressAnim.addUpdateListener(a -> {
            int p = (int) a.getAnimatedValue();
            bar.setProgress(p);
            loadingLabel.setText("LOADING... " + p + "%");
        });
        new Handler(Looper.getMainLooper()).postDelayed(progressAnim::start, 200);
    }

    private TextView cornerText(String text, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(11f);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setShadowLayer(6f, 0f, 0f, color);
        return tv;
    }

    private FrameLayout.LayoutParams corner(int gravity, int left, int top, int right, int bottom) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.gravity = gravity;
        p.setMargins(left, top, right, bottom);
        return p;
    }

    private TextView makeText(String s, float size, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextColor(color);
        tv.setTextSize(size);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        if (bold) tv.getPaint().setFakeBoldText(true);
        tv.setShadowLayer(18f, 0f, 0f, color);
        return tv;
    }

    private void startGlowLoop(TextView... views) {
        glowAnim = ValueAnimator.ofFloat(10f, 28f);
        glowAnim.setDuration(1100);
        glowAnim.setRepeatMode(ValueAnimator.REVERSE);
        glowAnim.setRepeatCount(ValueAnimator.INFINITE);
        glowAnim.setInterpolator(new LinearInterpolator());
        glowAnim.addUpdateListener(a -> {
            float r = (float) a.getAnimatedValue();
            for (TextView tv : views) {
                tv.setShadowLayer(r, 0f, 0f, tv.getCurrentTextColor());
            }
        });
        glowAnim.start();
    }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (glowAnim != null) glowAnim.cancel();
        if (progressAnim != null) progressAnim.cancel();
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
