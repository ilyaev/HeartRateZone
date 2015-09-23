package com.pbartz.heartmonitor.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.pbartz.heartmonitor.ControlActivity;
import com.pbartz.heartmonitor.R;
import com.pbartz.heartmonitor.zone.Config;

/**
 * TODO: document your custom view class.
 */
public class ZoneProgress extends View {

    Paint defPaint;
    Paint textPaint;

    Paint onePaint;
    Paint twoPaint;

    float zoneHeight;
    int hrValue = 0;
    int hrCurrent;
    private ControlActivity parentActivity;

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

        hrCurrent = 0;

    }

    private void initZoneConfig() {
        Config.init(185);
        defPaint = new Paint();
        defPaint.setARGB(125, 0, 255, 0);
        defPaint.setStyle(Paint.Style.FILL);
        defPaint.setStrokeWidth(20);

        textPaint = new Paint();
        textPaint.setARGB(255, 100, 100, 100);
        textPaint.setAntiAlias(true);

        onePaint = new Paint();
        onePaint.setARGB(255, 230, 230, 230);
        onePaint.setStyle(Paint.Style.FILL);

        twoPaint = new Paint();
        twoPaint.setStyle(Paint.Style.FILL);
        twoPaint.setARGB(255, 230, 230, 230);

    }

    public void setHrCurrent(int hrCurrent) {
        this.hrCurrent = hrCurrent;

        defPaint.set(Config.getPaintByZone(Config.getZoneByHr(hrValue)));
        defPaint.setAlpha(125);

        invalidate();
    }

    public int getHrCurrent() {
        return hrCurrent;
    }

    public void updateHrValue(int hrValue) {

        int newHrCurrent = (int)Config.hrValueToScreenHeight(hrValue, getHeight());
        this.hrValue = hrValue;

        ObjectAnimator hrAnimation = ObjectAnimator.ofInt(ZoneProgress.this, "hrCurrent", hrCurrent);
        hrAnimation.setIntValues(newHrCurrent);
        hrAnimation.setDuration(300);
        hrAnimation.start();
        hrAnimation.setInterpolator(new BounceInterpolator());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float colOneWidth = getColOneWidth();
        float textHeight = zoneHeight / 3f;

        float smTextHeight = textHeight / 2.4f;

        textPaint.setTextSize(textHeight);

        float bottom = textPaint.getFontMetrics().bottom;

        String labelTop = "22 min | 22%";
        String labelBottom = "";


        long secSumm = 0;
        for(int i = 0 ; i < 5 ; i++) {
            secSumm += parentActivity.getDataSet().zoneSecs[i];
        }

        for (int zone = 0 ; zone < 5 ; zone ++) {

            textPaint.setTextSize(textHeight);

            labelBottom = (int)Config.zoneMap.get(zone).hrValueFrom + " - " + (int)Config.zoneMap.get(zone).hrValueTo;

            textPaint.setARGB(255, 100, 100, 100);

            float centerX = colOneWidth / 2f;
            float centerY = getHeight() - (zone * zoneHeight) - zoneHeight / 2f;

            canvas.drawRect(0, getHeight() - (zone + 1) * zoneHeight + zoneHeight * 0.05f, colOneWidth, getHeight() - zone * zoneHeight - zoneHeight * 0.05f, Config.getPaintByZone(zone));

            canvas.drawRect(colOneWidth + zoneHeight * 0.05f, getHeight() - (zone + 1) * zoneHeight + zoneHeight * 0.05f, getWidth(), getHeight() - zone * zoneHeight - zoneHeight * 0.05f, twoPaint);


            float textWidth = textPaint.measureText(Config.zoneMap.get(zone).label);

            canvas.drawLine(0, getHeight() - zone * zoneHeight - 2, getWidth(), getHeight() - zone * zoneHeight - 2, textPaint);

            if (zone == 4) {
                textPaint.setARGB(255, 240, 240, 240);
            } else if (zone == 3) {
                textPaint.setARGB(255, 220, 220, 220);
            }

            canvas.drawText(Config.zoneMap.get(zone).label, centerX - textWidth / 2f, centerY + textHeight / 2f - bottom / 2f, textPaint);


            textPaint.setTextSize(smTextHeight);


            centerY -= zoneHeight / 3.5f;

            long secValue = parentActivity.getDataSet().zoneSecs[zone];

            if (parentActivity.getDataSet().zoneSecs[zone] > 0) {
                int secs = (int) parentActivity.getDataSet().zoneSecs[zone] / 1000;
                String secsLabel = "sec";
                if (secs > 180) {
                    secs = (int) secs / 60;
                    secsLabel = "min";
                }
                labelTop = "" + secs + " " + secsLabel + " | " + (int)Math.round((((float)secValue / (float)secSumm) * 100)) + "%";
                textWidth = textPaint.measureText(labelTop);
                canvas.drawText(labelTop, centerX - textWidth / 2f, centerY + smTextHeight / 2f - bottom / 2f, textPaint);

            }

            textWidth = textPaint.measureText(labelBottom);

            centerY += (zoneHeight / 3.5f) * 2.2f;

            canvas.drawText(labelBottom, centerX - textWidth / 2f, centerY + smTextHeight / 2f - bottom / 2f, textPaint);



        }

       canvas.drawRect(colOneWidth + zoneHeight * 0.05f, ((getHeight() - hrCurrent) > 0 ? getHeight() - hrCurrent : 0), getWidth(), getHeight(), defPaint);



    }

    public void setParentActivity(ControlActivity activity) {
        parentActivity = activity;
    }

    private float getColOneWidth() {
        return getWidth() * 0.2f;
    }

    public float getColTwoWidth() {
        return getWidth() < getHeight() ? getWidth() - getColOneWidth() : getHeight();
    }

    public Point getGaugeCenter() {
        Point res = new Point();

        res.set( (int)((getWidth() - getColOneWidth()) / 2f + getColOneWidth()), (int)(getHeight() / 2f));

        return res;
    }
}
