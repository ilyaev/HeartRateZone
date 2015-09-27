package com.pbartz.heartmonitor.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.pbartz.heartmonitor.ControlActivity;
import com.pbartz.heartmonitor.R;
import com.pbartz.heartmonitor.service.RandomService;
import com.pbartz.heartmonitor.zone.Chart;
import com.pbartz.heartmonitor.zone.Config;

import java.util.ArrayList;

/**
 * TODO: document your custom view class.
 */
public class ZoneChart extends View {

    private Paint paint;

    private ControlActivity parentActivity;

    private float shiftX = 0 ;

    private float shiftY = 0;

    public ZoneChart(Context context) {
        super(context);



        init(null, 0);
    }

    public ZoneChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ZoneChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ZoneChart, defStyle, 0);



        a.recycle();
        // Update TextPaint and text measurements from attributes

        paint = new Paint();

        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {

        paint.setColor(Color.argb(125, 255, 0, 0));
    }

    public void setParentActivity(ControlActivity activity) {
        parentActivity = activity;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int nPoints = parentActivity.getDataSet().data.size() > 90 ? 90 : parentActivity.getDataSet().data.size();
        float sWidth = getWidth() / (float)nPoints;

        ArrayList<Chart.HrPoint> points = parentActivity.getDataSet().data;

        int size = points.size();

        int num = 1;

        for(int i = size - 1 ; (i >= 0 && i > (size - nPoints)) ; i--) {

            float x = getWidth() - num * sWidth;
            float y = getHeight() * (points.get(i).hrValue / 205f);

            canvas.drawRect(shiftX + x - (sWidth / 2), shiftY + getHeight() - y, shiftX + x + (sWidth / 2), shiftY + getHeight(), Config.getPaintByZone(points.get(i).zone));

            num += 1;
        }



    }

    public void animateShift(float x, float y, long duration, long delay) {


        if (x != shiftX) {

            ObjectAnimator hrAnimation = ObjectAnimator.ofFloat(this, "shiftX", shiftY);
            hrAnimation.setStartDelay(delay);
            hrAnimation.setFloatValues(x);
            hrAnimation.setDuration(duration);
            hrAnimation.setInterpolator(new AccelerateInterpolator());
            hrAnimation.start();

        }

        if (y != shiftY) {

            ObjectAnimator hyAnimation = ObjectAnimator.ofFloat(this, "shiftY", shiftY);
            hyAnimation.setStartDelay(delay);
            hyAnimation.setFloatValues(y);
            hyAnimation.setDuration(duration);
            hyAnimation.setInterpolator(new AccelerateInterpolator());
            hyAnimation.start();

        }

    }

    public void setShiftX(float sX) {
        shiftX = sX;
        invalidate();
    }

    public void setShiftY(float sY) {
        shiftY = sY;
        invalidate();
    }

    public float getShiftX() {
        return shiftX;
    }

    public float getShiftY() {
        return shiftY;
    }

}
