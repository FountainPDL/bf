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

public class MapSelectActivity extends Activity {

    private String mode = "sandbox";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        String m = getIntent().getStringExtra("mode");
        if (m != null) mode = m;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                560, FrameLayout.LayoutParams.WRAP_CONTENT);
        cp.gravity = Gravity.CENTER;
        root.addView(col, cp);

        TextView title = new TextView(this);
        title.setText("SELECT MAP");
        title.setTextColor(Color.rgb(212,175,55));
        title.setTextSize(24f);
        title.getPaint().setFakeBoldText(true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(title);

        col.addView(spacer(24));
        col.addView(mapCard("GRASSLANDS",
                "Open fields · classic voxel terrain",
                Color.rgb(90,170,90), "grasslands"));
        col.addView(spacer(14));
        col.addView(mapCard("DESERT",
                "Sun-bleached dunes · hot sky",
                Color.rgb(205,165,90), "desert"));
        col.addView(spacer(14));
        col.addView(mapCard("SNOW",
                "Frozen ground · pale grey skies",
                Color.rgb(170,190,215), "snow"));
        col.addView(spacer(14));
        col.addView(mapCard("RUINS",
                "Collapsed walls · debris · dark sky · cover-heavy",
                Color.rgb(140,120,100), "ruins"));
        col.addView(spacer(28));

        HudButton back = new HudButton(this, "BACK");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 94);
        back.setLayoutParams(bp);
        back.setOnTapListener(this::finish);
        col.addView(back);

        setContentView(root);
    }

    private CardButton mapCard(String title, String sub, int accent, String mapId) {
        CardButton c = new CardButton(this, title, sub, accent);
        c.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 116));
        c.setOnTapListener(() -> {
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra("mode", mode);
            i.putExtra("map", mapId);
            startActivity(i);
        });
        return c;
    }

    private View spacer(int h) {
        View v=new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,h));
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
