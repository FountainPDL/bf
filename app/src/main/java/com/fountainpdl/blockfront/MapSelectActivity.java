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

/** Map picker shown after choosing a mode, before MainActivity launches. */
public class MapSelectActivity extends Activity {

    private String mode = "sandbox";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        String fromIntent = getIntent().getStringExtra("mode");
        if (fromIntent != null) mode = fromIntent;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(
                560, FrameLayout.LayoutParams.WRAP_CONTENT);
        columnParams.gravity = Gravity.CENTER;
        root.addView(column, columnParams);

        TextView title = new TextView(this);
        title.setText("SELECT MAP");
        title.setTextColor(Color.rgb(212, 175, 55));
        title.setTextSize(26f);
        title.getPaint().setFakeBoldText(true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        column.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        column.addView(spacer(30));
        column.addView(mapCard("GRASSLANDS", "Open fields, classic loadout", Color.rgb(90, 170, 90), "grasslands"));
        column.addView(spacer(20));
        column.addView(mapCard("DESERT", "Sun-bleached dunes", Color.rgb(205, 165, 90), "desert"));
        column.addView(spacer(20));
        column.addView(mapCard("SNOW", "Frozen ground, pale skies", Color.rgb(170, 190, 215), "snow"));
        column.addView(spacer(36));

        HudButton back = new HudButton(this, "BACK");
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 100);
        back.setLayoutParams(backParams);
        back.setOnTapListener(this::finish);
        column.addView(back);

        setContentView(root);
    }

    private CardButton mapCard(String title, String subtitle, int accent, String mapId) {
        CardButton card = new CardButton(this, title, subtitle, accent);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 120));
        card.setOnTapListener(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("mode", mode);
            intent.putExtra("map", mapId);
            startActivity(intent);
        });
        return card;
    }

    private View spacer(int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
        return v;
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
