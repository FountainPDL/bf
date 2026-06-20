package com.fountainpdl.blockfront;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Main menu: SANDBOX / DEMOLITION / SETTINGS / QUIT, fountain loop as ambient backdrop. */
public class MenuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        FountainSplashView fountain = new FountainSplashView(this);
        root.addView(fountain, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText("BLOCK FRONT");
        title.setTextColor(Color.WHITE);
        title.setTextSize(34f);
        title.getPaint().setFakeBoldText(true);
        title.setLetterSpacing(0.08f);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        titleParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        titleParams.topMargin = 60;
        root.addView(title, titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("a FountainPDL Ministries production");
        subtitle.setTextColor(Color.rgb(212, 175, 55));
        subtitle.setTextSize(13f);
        FrameLayout.LayoutParams subtitleParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        subtitleParams.topMargin = 112;
        root.addView(subtitle, subtitleParams);

        LinearLayout buttonColumn = new LinearLayout(this);
        buttonColumn.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(
                520, FrameLayout.LayoutParams.WRAP_CONTENT);
        columnParams.gravity = Gravity.CENTER;
        root.addView(buttonColumn, columnParams);

        buttonColumn.addView(menuButton("SANDBOX", () -> launchGame("sandbox")));
        buttonColumn.addView(spacer());
        buttonColumn.addView(menuButton("DEMOLITION", () -> launchGame("demolition")));
        buttonColumn.addView(spacer());
        buttonColumn.addView(menuButton("SETTINGS", () -> startActivity(new Intent(this, SettingsActivity.class))));
        buttonColumn.addView(spacer());
        buttonColumn.addView(menuButton("QUIT", this::finishAffinity));

        setContentView(root);
    }

    private HudButton menuButton(String label, Runnable action) {
        HudButton button = new HudButton(this, label);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 110));
        button.setOnTapListener(action::run);
        return button;
    }

    private View spacer() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24));
        return v;
    }

    private void launchGame(String mode) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("mode", mode);
        startActivity(intent);
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
