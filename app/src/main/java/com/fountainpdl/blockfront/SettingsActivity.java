package com.fountainpdl.blockfront;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Settings: look sensitivity (persisted) — kept intentionally small and honest;
 *  no placeholder toggles for systems (audio, graphics tiers) that don't exist yet. */
public class SettingsActivity extends Activity {

    public static final String PREFS_NAME = "block_front_prefs";
    public static final String KEY_SENSITIVITY_PERCENT = "look_sensitivity_percent";
    public static final int DEFAULT_SENSITIVITY_PERCENT = 50;

    private SharedPreferences prefs;
    private TextView valueLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(
                560, FrameLayout.LayoutParams.WRAP_CONTENT);
        columnParams.gravity = Gravity.CENTER;
        root.addView(column, columnParams);

        TextView title = new TextView(this);
        title.setText("SETTINGS");
        title.setTextColor(Color.rgb(212, 175, 55));
        title.setTextSize(26f);
        title.getPaint().setFakeBoldText(true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        column.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView label = new TextView(this);
        label.setText("LOOK SENSITIVITY");
        label.setTextColor(Color.WHITE);
        label.setTextSize(15f);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = 50;
        column.addView(label, labelParams);

        valueLabel = new TextView(this);
        valueLabel.setTextColor(Color.rgb(212, 175, 55));
        valueLabel.setTextSize(14f);
        column.addView(valueLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        int currentPercent = prefs.getInt(KEY_SENSITIVITY_PERCENT, DEFAULT_SENSITIVITY_PERCENT);
        updateValueLabel(currentPercent);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(100);
        seekBar.setProgress(currentPercent);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                updateValueLabel(progress);
                prefs.edit().putInt(KEY_SENSITIVITY_PERCENT, progress).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {}

            @Override
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        seekParams.topMargin = 10;
        column.addView(seekBar, seekParams);

        HudButton back = new HudButton(this, "BACK");
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 110);
        backParams.topMargin = 60;
        back.setLayoutParams(backParams);
        back.setOnTapListener(this::finish);
        column.addView(back);

        setContentView(root);
    }

    private void updateValueLabel(int percent) {
        float multiplier = percentToMultiplier(percent);
        valueLabel.setText(percent + "%  (" + String.format("%.2f", multiplier) + "x)");
    }

    /** 0% -> 0.4x, 50% -> 1.0x (default), 100% -> 2.2x */
    public static float percentToMultiplier(int percent) {
        if (percent <= 50) {
            return 0.4f + (percent / 50f) * 0.6f;
        } else {
            return 1.0f + ((percent - 50) / 50f) * 1.2f;
        }
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
