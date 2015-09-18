package com.pbartz.heartmonitor.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.pbartz.heartmonitor.R;

/**
 * TODO: document your custom view class.
 */
public class ZoneGauge extends View {

    private TextPaint mTextPaint;
    private Paint mCirclePaint;
    private float mTextWidth;
    private float mTextHeight;
    private float mBottom;

    private Point pCenter;

    private int hrValue = 60;

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
        mCirclePaint.setColor(Color.argb(125, 255, 0, 0));
        mCirclePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (pCenter == null) {
            pCenter = new Point();
        }

        pCenter.set((getWidth() / 4) * 3, getHeight() / 5);

    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(100);
        mTextPaint.setColor(Color.argb(255, 0, 0, 0));
        mTextWidth = mTextPaint.measureText(Integer.toString(hrValue));

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mBottom = fontMetrics.bottom;
        mTextHeight = 100;//fontMetrics.bottom
    }

    public void updateHrValue(int hrValue) {
        this.hrValue = hrValue;
        invalidateTextPaintAndMeasurements();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //canvas.drawCircle(pCenter.x, pCenter.y, mTextHeight * 4, mCirclePaint);

        float radius = mTextWidth;

        Log.i("TAG", " " + mTextHeight);

        canvas.drawArc(pCenter.x - radius, pCenter.y - radius, pCenter.x + radius, pCenter.y + radius, 0f, 275f, true, mCirclePaint);

        // Draw the text.
        canvas.drawText(Integer.toString(hrValue),
                pCenter.x - mTextWidth / 2,
                pCenter.y + mTextHeight / 2 - mBottom / 2,
                mTextPaint);

        canvas.drawRect(pCenter.x - mTextWidth / 2, pCenter.y - mTextHeight / 2, pCenter.x + mTextWidth / 2, pCenter.y + mTextHeight / 2, mCirclePaint);

    }

}
