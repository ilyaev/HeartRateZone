package com.pbartz.heartmonitor.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
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
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.pbartz.heartmonitor.R;

/**
 * TODO: document your custom view class.
 */
public class SpinnerView extends View {

    public class SpinnerCircle {

        public boolean animationInProgress = false;

        private float centerX;
        private float centerY;

        private float originX;
        private float originY;

        private float rotation = 0;

        private float radius;

        private float radiusShift;

        private Paint paint;

        private View parentView;

        private float pieValue = 360;

        AnimatorSet aSet = new AnimatorSet();
        AnimatorSet bSet = new AnimatorSet();

        public SpinnerCircle(float x, float y, float r) {
            this.centerX = x;
            this.centerY = y;
            this.radius = r;
            originX = x;
            originY = y;
            paint = new Paint();

            bSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (animationInProgress) {
                        Log.i("ATAG", "restart animation");
                        bSet.start();
                    }
                }
            });

            aSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (animationInProgress) {
                        aSet.play(getRotationAnimator());
                        aSet.start();
                    }
                }
            });
        }

        public ObjectAnimator getRotationAnimator() {
            ObjectAnimator animator = ObjectAnimator.ofFloat(this, "rotation", rotation);
            animator.setFloatValues(360);
            animator.setDuration(3000);
            animator.setInterpolator(new LinearInterpolator());

            return animator;
        }

        public void startRotationAnimation(float radiusShift) {
            this.radiusShift = radiusShift;

            aSet.cancel();

            aSet.play(getRotationAnimator());
            aSet.start();

            animationInProgress = true;


        }

        public void startPieAnimation() {
            ObjectAnimator aFirst = ObjectAnimator.ofFloat(this, "pieValue", 360);
            aFirst.setFloatValues(90);
            aFirst.setDuration(1500);
            aFirst.setInterpolator(new LinearInterpolator());

            ObjectAnimator aSecond = ObjectAnimator.ofFloat(this, "pieValue", 45);
            aSecond.setFloatValues(360);
            aSecond.setDuration(2500);
            aSecond.setInterpolator(new LinearInterpolator());

            bSet.cancel();
            bSet.playSequentially(aFirst, aSecond);
            bSet.start();

            animationInProgress = true;
        }

        public float getRotation() {
            return rotation;
        }

        public void setRotation(float rotation) {

            this.rotation = rotation;

            this.setCenterX((float) (originX + radiusShift * Math.cos(Math.toRadians(rotation))));
            this.setCenterY((float) (originY + radiusShift * Math.sin(Math.toRadians(rotation))));
        }

        public void setParentView(View view) {
            parentView = view;
        }

        public void draw(Canvas canvas) {
            if (pieValue == 360) {
                canvas.drawCircle(centerX, centerY, radius, paint);
            } else {

                float rFrom = - rotation;
                float rTo = pieValue;

                canvas.drawArc(centerX - radius, centerY - radius, centerX + radius, centerY + radius, rFrom, rTo, true, paint);

            }
        }

        public float getCenterX() {
            return centerX;
        }

        public void setCenterX(float centerX) {
            this.centerX = centerX;
            if (parentView != null) {
                parentView.invalidate();
            }
        }

        public float getPieValue() {
            return pieValue;
        }

        public void setPieValue(float pieValue) {
            this.pieValue = pieValue;
            if (parentView != null) {
                parentView.invalidate();
            }
        }

        public float getCenterY() {
            return centerY;
        }

        public void setCenterY(float centerY) {
            this.centerY = centerY;
            if (parentView != null) {
                parentView.invalidate();
            }
        }

        public float getRadius() {
            return radius;
        }

        public void setRadius(float radius) {
            this.radius = radius;
            if (parentView != null) {
                parentView.invalidate();
            }
        }

        public Paint getPaint() {
            return paint;
        }

        public void setPaint(Paint paint) {
            this.paint = paint;
            if (parentView != null) {
                parentView.invalidate();
            }
        }

    }


    private SpinnerCircle innerCircle;
    private SpinnerCircle outerCircle;

    public SpinnerView(Context context) {
        super(context);
        init(null, 0);
    }

    public SpinnerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public SpinnerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.SpinnerView, defStyle, 0);


        a.recycle();

        innerCircle = new SpinnerCircle(getWidth() / 2, getHeight() / 2, getWidth() * 0.65f);
        outerCircle = new SpinnerCircle(getWidth() / 2, getHeight() / 2, getWidth() * 0.8f);

        innerCircle.setParentView(this);
        outerCircle.setParentView(this);


        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        innerCircle = new SpinnerCircle(getWidth() / 2, getHeight() / 2, (getWidth() / 2) * 0.65f);
        outerCircle = new SpinnerCircle(getWidth() / 2, getHeight() / 2, (getWidth() / 2) * 0.75f);

        outerCircle.getPaint().setColor(Color.parseColor("#ff5200")); //#3b4662
        outerCircle.getPaint().setAlpha(125);
        innerCircle.getPaint().setARGB(255, 255, 255, 255);

        innerCircle.setParentView(this);
        outerCircle.setParentView(this);


    }

    public void startAnimation() {
        this.setVisibility(VISIBLE);

        innerCircle.startRotationAnimation(getWidth() * 0.05f);
        outerCircle.startRotationAnimation(getWidth() * 0.08f);
        outerCircle.startPieAnimation();
    }

    public void endAnimation() {
        this.setVisibility(INVISIBLE);
        innerCircle.animationInProgress = false;
    }

    private void invalidateTextPaintAndMeasurements() {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.argb(0, 0, 0, 0));

        outerCircle.draw(canvas);
        innerCircle.draw(canvas);


    }


}
