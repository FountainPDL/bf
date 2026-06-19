package com.fountainpdl.blockfront;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

/**
 * Full-screen transparent drag surface for FPS-style look control.
 * Sits above the GLSurfaceView but below the joystick/fire button, so
 * those corners still get their own touches (Android hit-tests topmost
 * child first; this view only receives touches that miss everything
 * above it).
 */
public class LookSurfaceView extends View {

    public interface OnLookListener {
        void onLook(float dx, float dy);
    }

    private float lastX, lastY;
    private boolean tracking = false;
    private OnLookListener listener;

    public LookSurfaceView(Context context) {
        super(context);
        setClickable(true);
    }

    public void setOnLookListener(OnLookListener l) {
        this.listener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                tracking = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (tracking) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    lastX = event.getX();
                    lastY = event.getY();
                    if (listener != null) listener.onLook(dx, dy);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                tracking = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
}
