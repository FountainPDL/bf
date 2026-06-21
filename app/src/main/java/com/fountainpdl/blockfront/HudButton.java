package com.fountainpdl.blockfront;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Small rounded tap button matching the game's gold/black HUD style. */
public class HudButton extends View {

    public interface OnTapListener {
        void onTap();
    }

    private final Paint bgPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final RectF rect = new RectF();
    private String label;
    private boolean pressed = false;
    private OnTapListener listener;

    public HudButton(Context context, String label) {
        super(context);
        this.label = label;
        bgPaint.setAntiAlias(true);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.rgb(212, 175, 55));
        textPaint.setTextSize(26f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    public void setOnTapListener(OnTapListener l) {
        this.listener = l;
    }

    public void setLabel(String label) {
        if (!label.equals(this.label)) {
            this.label = label;
            postInvalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        rect.set(0, 0, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        bgPaint.setColor(pressed ? Color.argb(220, 40, 40, 40) : Color.argb(160, 18, 18, 18));
        canvas.drawRoundRect(rect, 18f, 18f, bgPaint);
        float textY = getHeight() / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(label, getWidth() / 2f, textY, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
