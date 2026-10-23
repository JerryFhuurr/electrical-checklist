package com.jerry.electrical_checklist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/** Native bitmap surface shared by PDF pages and images, with pinch/double-tap zoom and drag. */
public final class DocumentPageView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestures;
    private Bitmap bitmap;
    private float zoom = 1, panX, panY;
    private float[] pendingState;

    public DocumentPageView(Context context, AttributeSet attributes) {
        super(context, attributes);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                zoomAt(zoom * detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                return true;
            }
        });
        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent event) { return true; }
            @Override public boolean onDoubleTap(MotionEvent event) {
                zoomAt(zoom > 1.1f ? 1 : 2.5f, event.getX(), event.getY());
                return true;
            }
            @Override public boolean onScroll(MotionEvent first, MotionEvent current, float dx, float dy) {
                if (!scaleDetector.isInProgress()) { panX -= dx; panY -= dy; clampPan(); invalidate(); }
                return true;
            }
        });
    }

    public void setPage(Bitmap page, float[] state) {
        bitmap = page; zoom = 1; panX = panY = 0; pendingState = state;
        restorePendingState(); invalidate();
    }
    public boolean hasPage() { return bitmap != null; }
    public float getZoom() { return zoom; }
    public float[] snapshot() {
        return new float[]{zoom, getWidth() == 0 ? 0 : panX / getWidth(), getHeight() == 0 ? 0 : panY / getHeight()};
    }
    public void fitPage() { zoomAt(1, getWidth() / 2f, getHeight() / 2f); }
    public void zoomBy(float factor) { zoomAt(zoom * factor, getWidth() / 2f, getHeight() / 2f); }

    private void restorePendingState() {
        if (pendingState == null || pendingState.length != 3 || getWidth() == 0 || getHeight() == 0) return;
        zoom = Math.max(1, Math.min(6, pendingState[0]));
        panX = pendingState[1] * getWidth(); panY = pendingState[2] * getHeight();
        pendingState = null; clampPan();
    }
    private void zoomAt(float target, float x, float y) {
        if (bitmap == null) return;
        float next = Math.max(1, Math.min(6, target)), ratio = next / zoom;
        panX = x - getWidth() / 2f - (x - getWidth() / 2f - panX) * ratio;
        panY = y - getHeight() / 2f - (y - getHeight() / 2f - panY) * ratio;
        zoom = next; clampPan(); invalidate();
    }
    private float baseScale() {
        return bitmap == null ? 1 : Math.min((float) getWidth() / bitmap.getWidth(), (float) getHeight() / bitmap.getHeight());
    }
    private void clampPan() {
        if (bitmap == null) { panX = panY = 0; return; }
        float scale = baseScale() * zoom;
        float maxX = Math.max(0, (bitmap.getWidth() * scale - getWidth()) / 2f);
        float maxY = Math.max(0, (bitmap.getHeight() * scale - getHeight()) / 2f);
        panX = Math.max(-maxX, Math.min(maxX, panX)); panY = Math.max(-maxY, Math.min(maxY, panY));
    }
    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        restorePendingState(); clampPan();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) return;
        float scale = baseScale() * zoom;
        canvas.save();
        canvas.translate((getWidth() - bitmap.getWidth() * scale) / 2f + panX,
                (getHeight() - bitmap.getHeight() * scale) / 2f + panY);
        canvas.scale(scale, scale); canvas.drawBitmap(bitmap, 0, 0, paint); canvas.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event); gestures.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP) performClick();
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
