package com.fountainpdl.blockfront;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** Simple virtual joystick. dx/dy reported in [-1, 1]. */
public class TouchJoystick extends View {

    public interface OnMoveListener {
        void onMove(float dx, float dy);
    }

    private final Paint basePaint = new Paint();
    private final Paint knobPaint = new Paint();
    private float centerX, centerY, knobX, knobY, radius;
    private OnMoveListener listener;

    public TouchJoystick(Context context) {
        super(context);
        basePaint.setColor(Color.argb(80, 212, 175, 55));
        knobPaint.setColor(Color.argb(180, 212, 175, 55));
        basePaint.setAntiAlias(true);
        knobPaint.setAntiAlias(true);
    }

    public void setOnMoveListener(OnMoveListener l) {
        this.listener = l;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        centerX = w / 2f;
        centerY = h / 2f;
        knobX = centerX;
        knobY = centerY;
        radius = Math.min(w, h) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, radius, basePaint);
        canvas.drawCircle(knobX, knobY, radius / 3f, knobPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                float dx = event.getX() - centerX;
                float dy = event.getY() - centerY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > radius && radius > 0) {
                    dx = dx / dist * radius;
                    dy = dy / dist * radius;
                }
                knobX = centerX + dx;
                knobY = centerY + dy;
                if (listener != null && radius > 0) {
                    listener.onMove(dx / radius, dy / radius);
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                knobX = centerX;
                knobY = centerY;
                if (listener != null) listener.onMove(0, 0);
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
}
