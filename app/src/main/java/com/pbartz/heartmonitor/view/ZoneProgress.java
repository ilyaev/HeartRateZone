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

    Paint onePaint;
    Paint twoPaint;

    float zoneHeight;
    int hrValue = 0;
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

//        if (hrCurrent > getHeight() / 2) {
//            defPaint.setARGB(125, 255, 0, 0);
//        } else {
//            defPaint.setARGB(125, 0, 255, 0);
//        }

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

        float colOneWidth = getWidth() * 0.2f;
        float textHeight = zoneHeight / 3f;
        textPaint.setTextSize(textHeight);

        float bottom = textPaint.getFontMetrics().bottom;

        for (int zone = 0 ; zone < 5 ; zone ++) {

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





        }

       canvas.drawRect(0, getHeight() - hrCurrent, getWidth(), getHeight(), defPaint);



    }
}
