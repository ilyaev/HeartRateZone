package com.pbartz.heartmonitor.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.pbartz.heartmonitor.R;
import com.pbartz.heartmonitor.zone.Config;

/**
 * TODO: document your custom view class.
 */
public class ZoneProgress extends View {

    Paint defPaint;
    Paint textPaint;

    float zoneHeight;
    int hrCurrent;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        zoneHeight = h / 5;

        textPaint.setTextSize(zoneHeight / 2);
    }

    public ZoneProgress(Context context) {
        super(context);
        init(null, 0);
    }

    public ZoneProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ZoneProgress(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {

        initZoneConfig();

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ZoneProgress, defStyle, 0);

        a.recycle();

        hrCurrent = 60;

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void initZoneConfig() {
        Config.init(185);
        defPaint = new Paint();
        defPaint.setARGB(125, 0, 255, 0);
        defPaint.setStyle(Paint.Style.FILL);
        defPaint.setStrokeWidth(20);

        textPaint = new Paint();
        textPaint.setARGB(255, 0, 0, 0);
        textPaint.setAntiAlias(true);

    }

    private void invalidateTextPaintAndMeasurements() {

    }

    public void setHrCurrent(int hrCurrent) {
        this.hrCurrent = hrCurrent;

        if (hrCurrent > getHeight() / 2) {
            defPaint.setARGB(125, 255, 0, 0);
        } else {
            defPaint.setARGB(125, 0, 255, 0);
        }

        invalidate();
    }

    public int getHrCurrent() {
        return hrCurrent;
    }

    public void updateHrValue(int hrValue) {
        ObjectAnimator mAnimatorX = ObjectAnimator.ofInt(ZoneProgress.this, "hrCurrent", hrCurrent);
        mAnimatorX.setIntValues(hrValue);
        mAnimatorX.setDuration(300);
        mAnimatorX.start();
        //mAnimatorX.setInterpolator(new AccelerateInterpolator());
        mAnimatorX.setInterpolator(new BounceInterpolator());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int zone = 0 ; zone < 5 ; zone ++) {
            canvas.drawText(Config.zoneMap.get(zone).label, 0, getHeight() - ((zoneHeight / 2 - zoneHeight / 4) + zone * zoneHeight), textPaint);
            canvas.drawLine(0, getHeight() - zone * zoneHeight - 2, getWidth(), getHeight() - zone * zoneHeight - 2, textPaint);
        }

        canvas.drawRect(0, getHeight() - hrCurrent, getWidth(), getHeight(), defPaint);



    }
}
