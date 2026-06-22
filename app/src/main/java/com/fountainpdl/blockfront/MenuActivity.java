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

public class MenuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        FountainSplashView fountain = new FountainSplashView(this);
        root.addView(fountain, matchParent());

        TextView title = new TextView(this);
        title.setText("BLOCK FRONT");
        title.setTextColor(Color.WHITE); title.setTextSize(30f);
        title.getPaint().setFakeBoldText(true); title.setLetterSpacing(0.08f);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        tp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP; tp.topMargin = 22;
        root.addView(title, tp);

        TextView sub = new TextView(this);
        sub.setText("a FountainPDL Ministries production");
        sub.setTextColor(Color.rgb(212,175,55)); sub.setTextSize(11f);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        sp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP; sp.topMargin = 66;
        root.addView(sub, sp);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(620,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        cp.gravity = Gravity.CENTER; cp.topMargin = 22;
        root.addView(column, cp);

        column.addView(modeCard("BATTLE ROYALE",
                "Shrinking zone \u00b7 7 bots \u00b7 jetpack \u00b7 solo survival",
                Color.rgb(212,175,55), 132, "battleroyale"));
        column.addView(spacer(11));
        column.addView(modeCard("FREE-FOR-ALL",
                "5 bots \u00b7 2 minutes \u00b7 most kills wins",
                Color.rgb(190,90,170), 100, "ffa"));
        column.addView(spacer(11));
        column.addView(modeCard("DEMOLITION",
                "Clear 15 targets before the clock runs out",
                Color.rgb(190,70,60), 100, "demolition"));
        column.addView(spacer(11));
        column.addView(modeCard("SANDBOX",
                "Free roam, unlimited ammo, no objectives",
                Color.rgb(80,150,90), 100, "sandbox"));
        column.addView(spacer(11));
        column.addView(campaignCard());
        column.addView(spacer(16));

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        column.addView(bottomRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        bottomRow.addView(smallBtn("SETTINGS", () -> startActivity(new Intent(this, SettingsActivity.class))));
        bottomRow.addView(gap());
        bottomRow.addView(smallBtn("CREDITS",  () -> startActivity(new Intent(this, CreditsActivity.class))));
        bottomRow.addView(gap());
        bottomRow.addView(smallBtn("QUIT", this::finishAffinity));

        setContentView(root);
    }

    private CardButton modeCard(String title, String subtitle, int accent, int height, String mode) {
        CardButton card = new CardButton(this, title, subtitle, accent);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height));
        card.setOnTapListener(() -> {
            Intent i = new Intent(this, MapSelectActivity.class);
            i.putExtra("mode", mode);
            startActivity(i);
        });
        return card;
    }

    private CardButton campaignCard() {
        CardButton card = new CardButton(this, "CAMPAIGN", "Story missions",
                Color.rgb(120,120,125));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 100));
        card.setOnTapListener(() -> startActivity(new Intent(this, CampaignActivity.class)));
        return card;
    }

    private HudButton smallBtn(String label, Runnable action) {
        HudButton button = new HudButton(this, label);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 80, 1f);
        button.setLayoutParams(p);
        button.setOnTapListener(action::run);
        return button;
    }

    private View gap() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(14, LinearLayout.LayoutParams.MATCH_PARENT));
        return v;
    }

    private View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h));
        return v;
    }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
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

