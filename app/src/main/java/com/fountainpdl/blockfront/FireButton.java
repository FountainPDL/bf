package com.fountainpdl.blockfront;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** Simple round tap button — fires once per ACTION_DOWN. */
public class FireButton extends View {

    public interface OnFireListener {
        void onFire();
    }

    private final Paint paint = new Paint();
    private boolean pressed = false;
    private OnFireListener listener;

    public FireButton(Context context) {
        super(context);
        paint.setAntiAlias(true);
    }

    public void setOnFireListener(OnFireListener l) {
        this.listener = l;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(pressed ? Color.argb(200, 212, 80, 60) : Color.argb(140, 212, 175, 55));
        float r = Math.min(getWidth(), getHeight()) / 2f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, r, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                invalidate();
                if (listener != null) listener.onFire();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
}
