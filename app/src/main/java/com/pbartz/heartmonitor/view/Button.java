package com.pbartz.heartmonitor.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Interpolator;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;

import com.pbartz.heartmonitor.R;

/**
 * Created by yura.ilyaev on 9/15/2015.
 */
public class Button extends View {

    private static final String TAG = "ViewButton";

    private String mText;
    private Paint mTextPaint;
    private Paint mPaint;

    private float centerX;
    private float centerY;
    private float mWidth;
    private float mHeight;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        Log.i(TAG, "Size Chnaged to: " + w + " / " + h);
        Log.i(TAG, "Padding: " + getPaddingLeft() + " / " + getPaddingRight());

        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
        mWidth = getWidth() / 4;
        mHeight = getHeight() / 4;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(centerX - mWidth / 2, centerY - mHeight / 2, centerX + mWidth / 2, centerY + mHeight / 2, mTextPaint);

    }

    public Button(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.Button, 0, 0);

        try {

            mText = a.getString(R.styleable.Button_labelText);

        } finally {
            a.recycle();
        }

        init();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {



            Log.i(TAG, "TOuch Down: " + getX() + " / " + getY());

            if (event.getX() > centerX - mWidth / 2 && event.getX() < centerX + mWidth / 2 && event.getY() > centerY - mHeight / 2 && event.getY() < centerY + mHeight / 2) {


                ObjectAnimator mAnimatorX = ObjectAnimator.ofFloat(Button.this, "centerX", centerX);
                mAnimatorX.setFloatValues(event.getX());
                mAnimatorX.setDuration(1000);
                mAnimatorX.start();
                mAnimatorX.setInterpolator(new BounceInterpolator());

                ObjectAnimator mAnimatorY = ObjectAnimator.ofFloat(Button.this, "centerY", centerY);
                mAnimatorY.setFloatValues(event.getY());
                mAnimatorY.setDuration(1000);
                mAnimatorY.start();
                mAnimatorY.setInterpolator(new BounceInterpolator());

                return true;

            }

        }

        return false;
    }

    public void setCenterX(float cX) {
        centerX = cX;
        invalidate();
    }

    public float getCenterX() {
        return centerX;
    }

    public void setCenterY(float cY) {
        centerY = cY;
        invalidate();
    }

    public float getCenterY() {
        return centerY;
    }

    private void init() {

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.argb(125, 255, 0, 0));
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextSize(50);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(50);

    }

    public void setmText(String text) {
        mText = text;
        invalidate();
        requestLayout();
    }
}
