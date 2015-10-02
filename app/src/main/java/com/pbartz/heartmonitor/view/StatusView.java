package com.pbartz.heartmonitor.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.pbartz.heartmonitor.R;

/**
 * TODO: document your custom view class.
 */
public class StatusView extends View {

    private Paint paint = new Paint();
    private Paint bmpPaint = new Paint();

    private Bitmap bmpHeartRaw;
    private Bitmap bmpHeart;

    private float heartShiftX = 0;
    private float heartShiftY = 0;

    public StatusView(Context context) {
        super(context);
        init(null, 0);
    }

    public StatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public StatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int rawWidth = bmpHeartRaw.getWidth();
        int rawHeight = bmpHeartRaw.getHeight();
        double aspectRatio = rawWidth / rawHeight;

        bmpHeart = getResizedBitmap(bmpHeartRaw, (int)(getWidth() / 2f), (int)(getWidth() / 2f * aspectRatio));


    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.StatusView, defStyle, 0);


        bmpHeartRaw = BitmapFactory.decodeResource(getResources(), R.drawable.heart2);


        a.recycle();

        paint.setARGB(255, 255, 0, 0);

        bmpPaint.setARGB(255, 0, 0, 0);
        ColorMatrix ma = new ColorMatrix();
        ma.setSaturation(0);
        bmpPaint.setAlpha(125);
        //ma.setScale(0.6f, 1, 1, 1);
        bmpPaint.setColorFilter(new ColorMatrixColorFilter(ma));

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {

    }

    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth)
    {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    public float getHeartPositionX() {
        return getWidth() / 2 - bmpHeart.getWidth() / 2f;
    }

    public float getHeartPositionY() {
        return getHeight() / 2f - (bmpHeart.getHeight() / 2f);
    }


    public void animateTo(float nX, float nY, long duration, long delay) {

        if (bmpHeart == null) {
            return;
        }

        if (nX != heartShiftX) {

            ObjectAnimator hrAnimation = ObjectAnimator.ofFloat(this, "heartShiftX", heartShiftX);
            hrAnimation.setStartDelay(delay);
            hrAnimation.setFloatValues(nX);
            hrAnimation.setDuration(duration);
            hrAnimation.setInterpolator(new AccelerateInterpolator());
            hrAnimation.start();

        }

        if (nY != heartShiftY) {

            ObjectAnimator hrAnimationY = ObjectAnimator.ofFloat(this, "heartShiftY", heartShiftY);
            hrAnimationY.setStartDelay(delay);
            hrAnimationY.setFloatValues(nY);
            hrAnimationY.setDuration(duration);
            hrAnimationY.setInterpolator(new AccelerateInterpolator());
            hrAnimationY.start();

        }

    }

    public void setHeartShiftX(float x) {
        heartShiftX = x;
        invalidate();
    }

    public void setHeartShiftY(float y) {
        heartShiftY = y;
        invalidate();
    }

    public float getHeartShiftX() {
        return heartShiftX;
    }

    public float getHeartShiftY() {
        return heartShiftY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        canvas.drawColor(Color.argb(0, 0, 0, 0));

        canvas.drawBitmap(bmpHeart, heartShiftX + this.getHeartPositionX(), heartShiftY + this.getHeartPositionY(), bmpPaint);


    }

    public void lightUpHeart(int trans) {
        ColorMatrix ma = new ColorMatrix();
        ma.setSaturation(10);
        bmpPaint.setColorFilter(new ColorMatrixColorFilter(ma));
        bmpPaint.setAlpha(trans);
        invalidate();
    }

    public void fadeDownHeart() {

        ColorMatrix ma = new ColorMatrix();
        ma.setSaturation(0);
        bmpPaint.setAlpha(125);
        //ma.setScale(0.6f, 1, 1, 1);
        bmpPaint.setColorFilter(new ColorMatrixColorFilter(ma));
        invalidate();

    }


    public int gettHeartHeight() {
        return bmpHeart.getHeight();
    }
}
