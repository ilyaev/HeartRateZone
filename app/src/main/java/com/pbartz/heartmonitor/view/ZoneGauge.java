package com.pbartz.heartmonitor.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.BounceInterpolator;

import com.pbartz.heartmonitor.R;
import com.pbartz.heartmonitor.zone.Config;

import java.util.ArrayList;

/**
 * TODO: document your custom view class.
 */
public class ZoneGauge extends View {

    public class ZoneSegment {

        public RectF mRect;
        public Point mCenter;
        public float mRadius;
        public int index;
        public int angleFrom;
        public int angleTo;
        public Paint mPaint;

        public ZoneSegment(int index, Point center, float radius) {
            this.index = index;
            mRect = new RectF(0,0,0,0);
            mPaint = new Paint();

            mCenter = center;
            mRadius = radius;

            mPaint.setColor(Color.argb(125, (int)Math.round(Math.random() * 255), (int)Math.round(Math.random() * 255), (int)Math.round(Math.random() * 255)));
        }

        public void calculate(int zoneValue, int allValue, int shift) {

            float perc = (float) zoneValue / ((float) allValue / 100f);

            float range = (360f / 100f) * perc;

            this.angleFrom = shift;
            this.angleTo = shift + (int) range;

            mRect.set(mCenter.x - mRadius, mCenter.y - mRadius, mCenter.x + mRadius, mCenter.y + mRadius);

        }

        public void draw(Canvas canvas, float glShift) {
            canvas.drawArc(mRect, glShift + angleFrom + 1, angleTo - angleFrom - 1, true, mPaint);
        }

    }

    private TextPaint mTextPaint;
    private Paint mCirclePaint;
    private float mTextWidth;
    private float mTextHeight;
    private float mBottom;
    public float mRadius = 0;

    public int fZone = 10;

    public float glShift = 0;

    private int[] mZoneWeight = {10, 20, 30, 40, 50};

    private Point pCenter;

    private int hrValue = 60;

    private ArrayList<ZoneSegment> zones;

    public ZoneGauge(Context context) {
        super(context);

        init(null, 0);
    }


    public ZoneGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ZoneGauge(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ZoneGauge, defStyle, 0);

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        mCirclePaint = new Paint();
        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setColor(Color.argb(255, 255, 255, 255));
        mCirclePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        // Update TextPaint and text measurements from attributes
        invalidateValue();
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateValue() {

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (pCenter == null) {
            pCenter = new Point();
        }

        pCenter.set((getWidth() / 4) * 3, getHeight() / 5);

        if (zones == null) {
            zones = new ArrayList<>(5);
            for(int i = 0 ; i < 5 ; i++) {
                ZoneSegment segment = new ZoneSegment(i, pCenter, mRadius );
                segment.mPaint.setColor(Color.argb(50 * (i + 1), 255, 0, 0));
                zones.add(i, segment);
            }
        }

    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(100);
        mTextPaint.setColor(Color.argb(255, 0, 0, 0));
        mTextWidth = mTextPaint.measureText(Integer.toString(hrValue));

        if (mRadius == 0) {
            mRadius = mTextWidth * 2;
        }

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mBottom = fontMetrics.bottom;
        mTextHeight = 100;//fontMetrics.bottom
    }

    public void updateHrValue(int hrValue) {
        this.hrValue = hrValue;
        invalidateTextPaintAndMeasurements();
        int[] tmp = {50, 50, 50, 50, 50};
        updateGauge(tmp);
        invalidate();
    }


    public void updateGauge(int[] gValues) {

        int summ = 0;
        for (int i = 0 ; i < 5 ; i++){
            summ += gValues[i];
        }

        int shift = 0;

        for(int i = 0 ; i < 5 ; i++) {
            ZoneSegment segment = zones.get(i);

            segment.calculate(gValues[i], summ, shift);

            shift = segment.angleTo;

        }

        if (fZone > 5) {
            focusZone(0);
        } else {


            focusZone(Config.getZoneByHr(hrValue));

            //focusZone((int)Math.round(Math.random() * 4));

        }

        invalidate();
    }

    public void setGlShift(float nShift) {
        glShift = nShift;
        invalidate();
    }

    public float getGlShift() {
        return glShift;
    }

    public void focusZone(int zone) {

        if (fZone == zone) {
            return;
        }

        fZone = zone;

        float nShift = zones.get(zone).angleFrom + (zones.get(zone).angleTo - zones.get(zone).angleFrom) / 2f;
        nShift *= -1;

        ObjectAnimator mAnimatorX = ObjectAnimator.ofFloat(ZoneGauge.this, "glShift", glShift);
        mAnimatorX.setFloatValues(nShift);
        mAnimatorX.setDuration(1000);
        mAnimatorX.start();
        mAnimatorX.setInterpolator(new BounceInterpolator());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for(int i = 0 ; i < 5 ; i++) {
            zones.get(i).draw(canvas, glShift + 90);
        }

        canvas.drawCircle(pCenter.x, pCenter.y, mRadius - mRadius / 7f, mCirclePaint);

        // Draw the text.
        canvas.drawText(Integer.toString(hrValue),
                pCenter.x - mTextWidth / 2,
                pCenter.y + mTextHeight / 2 - mBottom / 2,
                mTextPaint);

        //canvas.drawRect(pCenter.x - mTextWidth / 2, pCenter.y - mTextHeight / 2, pCenter.x + mTextWidth / 2, pCenter.y + mTextHeight / 2, mCirclePaint);

    }

}
