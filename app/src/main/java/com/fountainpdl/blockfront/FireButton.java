package com.fountainpdl.blockfront;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

/** Round fire button — first shot on DOWN, then repeats at 80 ms while held.
 *  GameRenderer.tryShoot() enforces weapon-specific fire delay internally. */
public class FireButton extends View {

    public interface OnFireListener { void onFire(); }

    private final Paint paint = new Paint();
    private boolean pressed = false;
    private OnFireListener listener;
    private final Handler repeatHandler = new Handler(Looper.getMainLooper());
    private Runnable repeatRunnable;

    public FireButton(Context context) {
        super(context);
        paint.setAntiAlias(true);
    }

    public void setOnFireListener(OnFireListener l) { this.listener = l; }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(pressed ? Color.argb(210, 220, 60, 40) : Color.argb(150, 212, 80, 55));
        float r = Math.min(getWidth(), getHeight()) / 2f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, r, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true; invalidate();
                fire();
                startRepeat();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                pressed = false; invalidate();
                stopRepeat();
                return true;
            default: return super.onTouchEvent(event);
        }
    }

    private void fire() { if (listener != null) listener.onFire(); }

    private void startRepeat() {
        repeatRunnable = new Runnable() {
            @Override public void run() {
                fire();
                repeatHandler.postDelayed(this, 80);
            }
        };
        repeatHandler.postDelayed(repeatRunnable, 160);
    }

    private void stopRepeat() {
        if (repeatRunnable != null) {
            repeatHandler.removeCallbacks(repeatRunnable);
            repeatRunnable = null;
        }
    }
}
