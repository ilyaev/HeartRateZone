package com.pbartz.heartmonitor.view;

import android.animation.Animator;
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
import android.widget.RelativeLayout;

import com.pbartz.heartmonitor.R;

/**
 * TODO: document your custom view class.
 */
public class Transition extends View {

    private Paint transitionPaint;
    private RelativeLayout targetLayout;
    private RelativeLayout sourceLayout;

    public int speed = 200;

    private int transparence = 0;

    public Transition(Context context) {
        super(context);
        init(null, 0);
    }

    public Transition(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public Transition(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.Transition, defStyle, 0);


        a.recycle();


        transitionPaint = new Paint();
        transitionPaint.setARGB(255, 20, 20, 20);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), getHeight(), transitionPaint);

    }

    public void startTransition() {
        this.setVisibility(View.VISIBLE);

        ObjectAnimator mAnimatorX = ObjectAnimator.ofInt(this, "transparence", 0);

        mAnimatorX.setIntValues(255);
        mAnimatorX.setDuration(speed);
        mAnimatorX.setInterpolator(new AccelerateInterpolator());

        final Transition that = this;

        mAnimatorX.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                sourceLayout.setVisibility(INVISIBLE);
                targetLayout.setVisibility(VISIBLE);

                ObjectAnimator mAnimatorY = ObjectAnimator.ofInt(that, "transparence", 255);

                mAnimatorY.setIntValues(0);
                mAnimatorY.setDuration(speed);
                mAnimatorY.setInterpolator(new AccelerateInterpolator());
                mAnimatorY.start();

                mAnimatorY.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        that.setVisibility(INVISIBLE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });

            }

            @Override
            public void onAnimationCancel(Animator animation) {
                sourceLayout.setVisibility(INVISIBLE);
                targetLayout.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        mAnimatorX.start();
    }

    public void setTransparence(int transparence) {
        this.transparence = transparence;
        transitionPaint.setAlpha(transparence);
        invalidate();
    }

    public int getTransparence() {
        return this.transparence;
    }


    public void setTargetLayout(RelativeLayout layout) {
        targetLayout = layout;
    }

    public void setSourceLayout(RelativeLayout layout) {
        sourceLayout = layout;
    }
}
