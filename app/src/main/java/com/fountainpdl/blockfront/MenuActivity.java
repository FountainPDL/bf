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

/** Main menu: a featured Battle Royale card, Demolition/Sandbox/Campaign
 *  cards, and a Settings/Credits/Quit row — the fountain loop runs as an
 *  ambient backdrop behind everything. */
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
        title.setTextSize(32f);
        title.getPaint().setFakeBoldText(true);
        title.setLetterSpacing(0.08f);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        titleParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        titleParams.topMargin = 30;
        root.addView(title, titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("a FountainPDL Ministries production");
        subtitle.setTextColor(Color.rgb(212, 175, 55));
        subtitle.setTextSize(12f);
        FrameLayout.LayoutParams subtitleParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        subtitleParams.topMargin = 78;
        root.addView(subtitle, subtitleParams);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(
                620, FrameLayout.LayoutParams.WRAP_CONTENT);
        columnParams.gravity = Gravity.CENTER;
        columnParams.topMargin = 30;
        root.addView(column, columnParams);

        CardButton battleRoyale = new CardButton(this, "BATTLE ROYALE",
                "Shrinking zone \u00b7 jetpack \u00b7 solo survival", Color.rgb(212, 175, 55));
        battleRoyale.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 150));
        battleRoyale.setOnTapListener(() -> goToMapSelect("battleroyale"));
        column.addView(battleRoyale);

        column.addView(spacer(16));

        CardButton demolition = new CardButton(this, "DEMOLITION",
                "Clear 15 targets before the clock runs out", Color.rgb(190, 70, 60));
        demolition.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 116));
        demolition.setOnTapListener(() -> goToMapSelect("demolition"));
        column.addView(demolition);

        column.addView(spacer(14));

        CardButton sandbox = new CardButton(this, "SANDBOX",
                "Free roam, unlimited ammo, no objectives", Color.rgb(80, 150, 90));
        sandbox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 116));
        sandbox.setOnTapListener(() -> goToMapSelect("sandbox"));
        column.addView(sandbox);

        column.addView(spacer(14));

        CardButton campaign = new CardButton(this, "CAMPAIGN",
                "Story missions", Color.rgb(120, 120, 125));
        campaign.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 116));
        campaign.setOnTapListener(() -> startActivity(new Intent(this, CampaignActivity.class)));
        column.addView(campaign);

        column.addView(spacer(22));

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        column.addView(bottomRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        bottomRow.addView(smallButton("SETTINGS",
                () -> startActivity(new Intent(this, SettingsActivity.class))));
        bottomRow.addView(rowSpacer());
        bottomRow.addView(smallButton("CREDITS",
                () -> startActivity(new Intent(this, CreditsActivity.class))));
        bottomRow.addView(rowSpacer());
        bottomRow.addView(smallButton("QUIT", this::finishAffinity));

        setContentView(root);
    }

    private HudButton smallButton(String label, Runnable action) {
        HudButton button = new HudButton(this, label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 90, 1f);
        button.setLayoutParams(params);
        button.setOnTapListener(action::run);
        return button;
    }

    private View rowSpacer() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(16, LinearLayout.LayoutParams.MATCH_PARENT));
        return v;
    }

    private View spacer(int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
        return v;
    }

    private void goToMapSelect(String mode) {
        Intent intent = new Intent(this, MapSelectActivity.class);
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
