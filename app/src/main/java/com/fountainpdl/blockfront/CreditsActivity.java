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

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(
                560, FrameLayout.LayoutParams.WRAP_CONTENT);
        columnParams.gravity = Gravity.CENTER;
        root.addView(column, columnParams);

        TextView title = new TextView(this);
        title.setText("CREDITS");
        title.setTextColor(Color.rgb(212, 175, 55));
        title.setTextSize(28f);
        title.getPaint().setFakeBoldText(true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        column.addView(title);

        column.addView(creditLine("BLOCK FRONT", "A FountainPDL Ministries production", 28));
        column.addView(creditLine("Engine", "Custom OpenGL ES 2.0 voxel renderer", 22));
        column.addView(creditLine("Built with", "Claude (Anthropic), via Termux + GitHub Actions", 22));
        column.addView(creditLine("Inspired by", "Mini Militia & Apex Legends", 22));

        HudButton back = new HudButton(this, "BACK");
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 100);
        backParams.topMargin = 50;
        back.setLayoutParams(backParams);
        back.setOnTapListener(this::finish);
        column.addView(back);

        setContentView(root);
    }

    private LinearLayout creditLine(String label, String value, int topMargin) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = topMargin;
        row.setLayoutParams(rowParams);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(Color.rgb(212, 175, 55));
        labelView.setTextSize(13f);
        labelView.setLetterSpacing(0.1f);
        row.addView(labelView);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(Color.WHITE);
        valueView.setTextSize(17f);
        row.addView(valueView);

        return row;
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
