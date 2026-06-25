package com.fountainpdl.blockfront;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Main menu — CODM-style home screen:
 * - Left 62%: fountain ambient backdrop + game title + mode cards
 * - Right 38%: auto-rotating voxel operator (AK-47 in hand) on dark bg
 */
public class MenuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        // Root: full screen dark base
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(4,4,6));

        // Fountain loop as subtle ambient left-half backdrop
        FountainSplashView fountain = new FountainSplashView(this);
        fountain.setAlpha(0.55f);
        FrameLayout.LayoutParams fountainParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView(fountain, fountainParams);

        // Horizontal split
        LinearLayout hSplit = new LinearLayout(this);
        hSplit.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(hSplit, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // ── Left panel ──────────────────────────────────────────────────────
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(50,36,30,36);
        hSplit.addView(left, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 0.60f));

        TextView title = new TextView(this);
        title.setText("BLOCK FRONT");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28f);
        title.getPaint().setFakeBoldText(true);
        title.setLetterSpacing(0.08f);
        left.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView sub = new TextView(this);
        sub.setText("FountainPDL Ministries");
        sub.setTextColor(Color.rgb(212,175,55));
        sub.setTextSize(11f);
        sub.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subP.bottomMargin = 20;
        left.addView(sub, subP);

        // Mode cards
        left.addView(modeCard("BATTLE ROYALE",
                "Shrinking zone · bots · loot · solo survival",
                Color.rgb(212,175,55), 130, "battleroyale"));
        left.addView(spacer(10));
        left.addView(modeCard("FREE-FOR-ALL",
                "5 bots · 2 min · most kills wins",
                Color.rgb(190,90,170), 96, "ffa"));
        left.addView(spacer(10));
        left.addView(modeCard("DEMOLITION",
                "Clear 15 targets before time runs out",
                Color.rgb(190,70,60), 96, "demolition"));
        left.addView(spacer(10));
        left.addView(modeCard("SANDBOX",
                "Free roam · all weapons · no objectives",
                Color.rgb(80,150,90), 96, "sandbox"));
        left.addView(spacer(10));
        left.addView(modeCard("CAMPAIGN",
                "Story missions — coming soon",
                Color.rgb(110,110,115), 96, "campaign"));

        left.addView(spacer(16));

        // Bottom row: settings / credits / quit
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        left.addView(btnRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        btnRow.addView(smallBtn("SETTINGS", ()->startActivity(new Intent(this,SettingsActivity.class))));
        btnRow.addView(rowGap());
        btnRow.addView(smallBtn("CREDITS",  ()->startActivity(new Intent(this,CreditsActivity.class))));
        btnRow.addView(rowGap());
        btnRow.addView(smallBtn("QUIT", this::finishAffinity));

        // ── Right panel — operator preview ──────────────────────────────────
        FrameLayout rightFrame = new FrameLayout(this);
        rightFrame.setBackgroundColor(Color.rgb(6,6,9));
        hSplit.addView(rightFrame, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 0.40f));

        GLSurfaceView operatorView = new GLSurfaceView(this);
        operatorView.setEGLContextClientVersion(2);
        operatorView.setRenderer(new OperatorRenderer(this));
        operatorView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        rightFrame.addView(operatorView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Overlay text on operator panel
        TextView opLabel = new TextView(this);
        opLabel.setText("OPERATOR");
        opLabel.setTextColor(Color.argb(180,212,175,55));
        opLabel.setTextSize(11f);
        opLabel.setLetterSpacing(0.18f);
        FrameLayout.LayoutParams olP = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        olP.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        olP.bottomMargin = 20;
        rightFrame.addView(opLabel, olP);

        setContentView(root);
    }

    private CardButton modeCard(String title, String subtitle, int accent, int height, String mode) {
        CardButton card = new CardButton(this, title, subtitle, accent);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height));
        if ("campaign".equals(mode)) {
            card.setLocked(true);
            card.setOnTapListener(()->startActivity(new Intent(this, CampaignActivity.class)));
        } else {
            card.setOnTapListener(()->{
                Intent i=new Intent(this,MapSelectActivity.class);
                i.putExtra("mode", mode);
                startActivity(i);
            });
        }
        return card;
    }

    private HudButton smallBtn(String label, Runnable action) {
        HudButton b = new HudButton(this, label);
        b.setLayoutParams(new LinearLayout.LayoutParams(0,78,1f));
        b.setOnTapListener(action::run);
        return b;
    }

    private View spacer(int h) {
        View v=new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,h));
        return v;
    }
    private View rowGap() {
        View v=new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(12,LinearLayout.LayoutParams.MATCH_PARENT));
        return v;
    }

    @Override public void onWindowFocusChanged(boolean f){super.onWindowFocusChanged(f);if(f)hideSystemUI();}
    private void hideSystemUI(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                |View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
