package com.fountainpdl.blockfront;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Dark card with a colored accent bar, title, subtitle, and an optional
 *  "locked" state — used for both the menu's mode cards and the map picker. */
public class CardButton extends View {

    public interface OnTapListener {
        void onTap();
    }

    private final Paint bgPaint = new Paint();
    private final Paint accentPaint = new Paint();
    private final Paint titlePaint = new Paint();
    private final Paint subtitlePaint = new Paint();
    private final Paint lockPaint = new Paint();
    private final RectF rect = new RectF();
    private final RectF accentRect = new RectF();

    private final String title;
    private final String subtitle;
    private final int accentColor;
    private boolean pressed = false;
    private boolean locked = false;
    private OnTapListener listener;

    public CardButton(Context context, String title, String subtitle, int accentColor) {
        super(context);
        this.title = title;
        this.subtitle = subtitle;
        this.accentColor = accentColor;

        bgPaint.setAntiAlias(true);

        accentPaint.setAntiAlias(true);
        accentPaint.setColor(accentColor);

        titlePaint.setAntiAlias(true);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(30f);
        titlePaint.setFakeBoldText(true);

        subtitlePaint.setAntiAlias(true);
        subtitlePaint.setColor(Color.argb(200, 255, 255, 255));
        subtitlePaint.setTextSize(15f);

        lockPaint.setAntiAlias(true);
        lockPaint.setColor(Color.argb(220, 170, 170, 170));
        lockPaint.setTextSize(13f);
        lockPaint.setTextAlign(Paint.Align.RIGHT);
        lockPaint.setFakeBoldText(true);
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        invalidate();
    }

    public void setOnTapListener(OnTapListener l) {
        this.listener = l;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        rect.set(0, 0, w, h);
        accentRect.set(0, 0, 10f, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        bgPaint.setColor(pressed ? Color.argb(235, 32, 32, 32) : Color.argb(195, 16, 16, 16));
        canvas.drawRoundRect(rect, 16f, 16f, bgPaint);

        canvas.save();
        canvas.clipRect(0f, 0f, 16f, getHeight());
        accentPaint.setAlpha(locked ? 70 : 255);
        canvas.drawRoundRect(0f, 0f, 16f, getHeight(), 16f, 16f, accentPaint);
        canvas.restore();

        float padLeft = 38f;
        titlePaint.setAlpha(locked ? 110 : 255);
        canvas.drawText(title, padLeft, getHeight() * 0.42f, titlePaint);
        subtitlePaint.setAlpha(locked ? 90 : 200);
        canvas.drawText(subtitle, padLeft, getHeight() * 0.42f + 32f, subtitlePaint);

        if (locked) {
            canvas.drawText("COMING SOON", getWidth() - 26f, 32f, lockPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (locked) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                pressed = false;
                invalidate();
                if (listener != null) listener.onTap();
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
}
