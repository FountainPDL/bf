package com.fountainpdl.blockfront;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CreditsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                560, FrameLayout.LayoutParams.WRAP_CONTENT);
        cp.gravity = Gravity.CENTER;
        root.addView(col, cp);

        TextView title = new TextView(this);
        title.setText("CREDITS");
        title.setTextColor(Color.rgb(212, 175, 55));
        title.setTextSize(28f);
        title.getPaint().setFakeBoldText(true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(title);

        col.addView(line("BLOCK FRONT",           "FountainPDL Ministries", 28));
        col.addView(line("Engine",                "Custom OpenGL ES 2.0 voxel renderer", 22));
        col.addView(line("Inspired by",           "Mini Militia & Apex Legends", 22));
        col.addView(line("Character design",      "Roblox-style blocky aesthetic", 22));
        col.addView(line("Termux + GitHub Actions","Offline Android CI/CD workflow", 22));

        HudButton back = new HudButton(this, "BACK");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 100);
        bp.topMargin = 50;
        back.setLayoutParams(bp);
        back.setOnTapListener(this::finish);
        col.addView(back);

        setContentView(root);
    }

    private LinearLayout line(String label, String value, int topMargin) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.topMargin = topMargin;
        row.setLayoutParams(rp);
        TextView lv = new TextView(this);
        lv.setText(label);
        lv.setTextColor(Color.rgb(212, 175, 55));
        lv.setTextSize(13f);
        lv.setLetterSpacing(0.1f);
        row.addView(lv);
        TextView vv = new TextView(this);
        vv.setText(value);
        vv.setTextColor(Color.WHITE);
        vv.setTextSize(17f);
        row.addView(vv);
        return row;
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void hideSystemUI() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
