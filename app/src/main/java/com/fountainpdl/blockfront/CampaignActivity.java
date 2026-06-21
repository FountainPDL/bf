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

public class CampaignActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(
                620, FrameLayout.LayoutParams.WRAP_CONTENT);
        columnParams.gravity = Gravity.CENTER;
        root.addView(column, columnParams);

        TextView title = new TextView(this);
        title.setText("CAMPAIGN");
        title.setTextColor(Color.rgb(212, 175, 55));
        title.setTextSize(28f);
        title.getPaint().setFakeBoldText(true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        column.addView(title);

        TextView soon = new TextView(this);
        soon.setText("COMING SOON");
        soon.setTextColor(Color.WHITE);
        soon.setTextSize(14f);
        soon.setGravity(Gravity.CENTER_HORIZONTAL);
        soon.setLetterSpacing(0.15f);
        LinearLayout.LayoutParams soonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        soonParams.topMargin = 10;
        column.addView(soon, soonParams);

        TextView body = new TextView(this);
        body.setText("Story missions, NPCs, and objectives are planned but not built yet — "
                + "Battle Royale and Demolition come first since they reuse the systems "
                + "already in place. Campaign needs its own mission/dialogue framework.");
        body.setTextColor(Color.argb(210, 255, 255, 255));
        body.setTextSize(15f);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setLineSpacing(6f, 1f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = 36;
        column.addView(body, bodyParams);

        HudButton back = new HudButton(this, "BACK");
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 100);
        backParams.topMargin = 50;
        back.setLayoutParams(backParams);
        back.setOnTapListener(this::finish);
        column.addView(back);

        setContentView(root);
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
