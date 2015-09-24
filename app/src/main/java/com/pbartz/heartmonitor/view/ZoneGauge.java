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
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;

import com.pbartz.heartmonitor.ControlActivity;
import com.pbartz.heartmonitor.R;
import com.pbartz.heartmonitor.zone.Config;

import java.util.ArrayList;

/**
 * TODO: document your custom view class.
 */
public class ZoneGauge extends View {

    public class CustomLabel {

        public String text;
        public float x = 0;
        public float y = 0;

        public float size = 50;

        public float textWidth = 0;
        public float bottom;

        public TextPaint paint;

        private String oldText;

        public CustomLabel(String text, float x, float y) {
            this.text = text;
            this.oldText = text;
            this.x = x;
            this.y = y;

            paint = new TextPaint();
            paint.setTextSize(size);
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            paint.setTextAlign(Paint.Align.LEFT);

            recalculateMetrics();
        }

        public void setSize(float size) {
            this.size = size;
            paint.setTextSize(size);
            recalculateMetrics();
        }

        public void setText(String text) {
            this.text = text;
            recalculateMetrics();
        }

        public void recalculateMetrics() {
            textWidth = paint.measureText(text);

            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            bottom = fontMetrics.bottom;

        }

        public void draw(Canvas canvas) {
            canvas.drawText(text,
                    x - textWidth / 2,
                    y + size / 2 - bottom / 2,
                    paint);
        }


    }

    public class ZoneSegment {

        public RectF mRect;
        public Point mCenter;
        public float mRadius;
        public int index;
        public float angleFrom;
        public float angleTo;
        public Paint mPaint;

        public int value = 0;

        public ZoneSegment(int index, Point center, float radius) {
            this.index = index;
            mRect = new RectF(0,0,0,0);
            mPaint = new Paint();

            mCenter = center;
            mRadius = radius;

            mPaint.setColor(Color.argb(125, (int)Math.round(Math.random() * 255), (int)Math.round(Math.random() * 255), (int)Math.round(Math.random() * 255)));
        }

        public void calculate(int zoneValue, int allValue, float shift, int zoneIndex) {

            float range = 360 * ( (float)zoneValue / (float)allValue );

            this.angleFrom = shift;
            this.angleTo = shift + range;

            value = zoneValue;

            float radius = mRadius;

            if (Config.getZoneByHr(hrValue) == zoneIndex) {
                radius += mRadius * 0.05f;
            }

            mRect.set(mCenter.x - radius, mCenter.y - radius, mCenter.x + radius, mCenter.y + radius);

        }

        public void draw(Canvas canvas, float glShift) {
            if (value > 0) {
                canvas.drawArc(mRect, glShift + angleFrom + 1, angleTo - angleFrom - 1, true, Config.getPaintByZone(index));
            }
        }

    }

    private Paint mCirclePaint;
    private Paint mCircleOutlinePaint;
    public float mRadius = 0;

    private CustomLabel labelHR;
    private CustomLabel labelTitle;
    private CustomLabel labelLevel;

    public int fZone = 10;

    public float glShift = 0;

    private int[] mZoneWeight = {10, 20, 30, 40, 50};

    private Point pCenter;

    public float shiftX = 0;
    public float shiftY = 0;

    private int hrValue = -10;

    private ArrayList<ZoneSegment> zones;

    private ControlActivity parentActivity;

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

        labelHR = new CustomLabel("61", 0, 0);
        labelHR.paint.setColor(Color.argb(255, 0, 0, 0));

        labelTitle = new CustomLabel("HEART RATE, BPM", 0, 0);
        labelTitle.paint.setColor(Color.argb(255, 150, 150, 150));
        labelLevel = new CustomLabel("ENDURANCE", 0, 0);
        labelLevel.paint.setColor(Color.argb(255, 255, 0, 0));

        mCirclePaint = new Paint();
        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setColor(Color.argb(255, 255, 255, 255));
        mCirclePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        mCircleOutlinePaint = new Paint();
        mCircleOutlinePaint.setStyle(Paint.Style.STROKE);
        mCircleOutlinePaint.setColor(Color.argb(255, 200, 200, 200));
        mCircleOutlinePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (pCenter == null) {
            pCenter = new Point();
        }

        if (parentActivity != null) {
            mRadius = parentActivity.viewProgress.getColTwoWidth() * 0.8f / 2f;
        } else {
            mRadius = getWidth() / 3f;
        }

        labelHR.setSize((int) (mRadius / 1.5f));
        labelTitle.setSize(labelHR.size / 6f);
        labelLevel.setSize(labelHR.size / 5f);


        if (parentActivity != null) {

            pCenter.set(parentActivity.viewProgress.getGaugeCenter().x, parentActivity.viewProgress.getGaugeCenter().y);

        } else {

            pCenter.set(getWidth() / 2, (int) (mRadius + mRadius / 5f));

        }

        labelHR.x = pCenter.x;
        labelHR.y = pCenter.y;

        labelTitle.x = pCenter.x;
        labelTitle.y = pCenter.y - labelHR.size / 2 - labelTitle.size * 0.7f;

        labelLevel.x = pCenter.x;
        labelLevel.y = pCenter.y + labelHR.size / 2 + labelLevel.size * 1.2f;

        if (zones == null) {

            zones = new ArrayList<>(5);

            for(int i = 0 ; i < 5 ; i++) {
                ZoneSegment segment = new ZoneSegment(i, pCenter, mRadius );
                segment.mPaint.setColor(Color.argb(50 * (i + 1), 255, 0, 0));
                zones.add(i, segment);
            }
        }

    }

    public void setShiftX(float sX) {
        shiftX = sX;
    }

    public void setShiftY(float sY) {
        shiftY = sY;
    }

    public float getShiftX() {
        return shiftX;
    }

    public float getShiftY() {
        return shiftY;
    }

    public void updateHrValue(int hrValue) {
        this.hrValue = hrValue;

        labelHR.setText(Integer.toString(hrValue));
        labelLevel.setText(Config.zoneLevel[Config.getZoneByHr(hrValue)]);

        updateGauge(parentActivity.getDataSet().getZoneCounts());
        invalidate();
    }

    public void setParentActivity(ControlActivity activity) {
        parentActivity = activity;
    }


    public void updateGauge(int[] gValues) {

        int summ = 0;
        for (int i = 0 ; i < 5 ; i++){
            summ += gValues[i];
        }

        float shift = 0;

        for(int i = 0 ; i < 5 ; i++) {
            ZoneSegment segment = zones.get(i);

            segment.calculate(gValues[i], summ, shift, i);

            shift = segment.angleTo;

        }

        focusZone(Config.getZoneByHr(hrValue));

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

        fZone = zone;

        float nShift = zones.get(zone).angleFrom + (zones.get(zone).angleTo - zones.get(zone).angleFrom) / 2f;
        nShift *= -1;

        if (Math.abs(glShift - nShift) > 5) {

            ObjectAnimator mAnimatorX = ObjectAnimator.ofFloat(ZoneGauge.this, "glShift", glShift);
            mAnimatorX.setFloatValues(nShift);
            mAnimatorX.setDuration(900);
            mAnimatorX.start();
            mAnimatorX.setInterpolator(new BounceInterpolator());

        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (hrValue <= 0) {
            return;
        }

        for(int i = 0 ; i < 5 ; i++) {
            zones.get(i).draw(canvas, glShift + 90);
        }

        canvas.drawCircle(shiftX + pCenter.x, shiftY + pCenter.y, mRadius - mRadius / 5f, mCirclePaint);

        labelHR.draw(canvas);
        labelTitle.draw(canvas);
        labelLevel.draw(canvas);

        mCircleOutlinePaint.setStrokeWidth(mRadius * 0.01f);
        canvas.drawCircle(pCenter.x, pCenter.y, mRadius + mRadius * 0.05f, mCircleOutlinePaint);


    }

    public void setCenterX(float cX) {
        pCenter.x = (int)cX;
        updateHrValue(hrValue);
        labelHR.x = pCenter.x;
        labelHR.y = pCenter.y;

        labelTitle.x = pCenter.x;
        labelTitle.y = pCenter.y - labelHR.size / 2 - labelTitle.size * 0.7f;

        labelLevel.x = pCenter.x;
        labelLevel.y = pCenter.y + labelHR.size / 2 + labelLevel.size * 1.2f;

        invalidate();
    }

    public float getCenterX() {
        return pCenter.x;
    }

    public void setCenterY(float cY) {
        pCenter.y = (int) cY;
        updateHrValue(hrValue);
        labelHR.x = pCenter.x;
        labelHR.y = pCenter.y;

        labelTitle.x = pCenter.x;
        labelTitle.y = pCenter.y - labelHR.size / 2 - labelTitle.size * 0.7f;

        labelLevel.x = pCenter.x;
        labelLevel.y = pCenter.y + labelHR.size / 2 + labelLevel.size * 1.2f;
        invalidate();
    }

    public float getCenterY() {
        return pCenter.y;
    }
}
