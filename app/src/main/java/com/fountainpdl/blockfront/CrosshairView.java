package com.fountainpdl.blockfront;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Small tick-mark crosshair that briefly flashes gold when a shot lands. */
public class CrosshairView extends View {

    private final Paint paint = new Paint();
    private boolean hit = false;

    public CrosshairView(Context context) {
        super(context);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(4f);
    }

    public void setHit(boolean hit) {
        if (this.hit != hit) {
            this.hit = hit;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float gap = 8f;
        float len = 10f;
        paint.setColor(hit ? Color.argb(255, 212, 175, 55) : Color.argb(220, 255, 255, 255));

        canvas.drawLine(cx, cy - gap - len, cx, cy - gap, paint);
        canvas.drawLine(cx, cy + gap, cx, cy + gap + len, paint);
        canvas.drawLine(cx - gap - len, cy, cx - gap, cy, paint);
        canvas.drawLine(cx + gap, cy, cx + gap + len, cy, paint);
    }
}
