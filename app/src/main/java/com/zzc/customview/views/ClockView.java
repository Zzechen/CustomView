package com.zzc.customview.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.zzc.customview.R;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2017/3/1.
 */

public class ClockView extends View {
    private static final String TAG = "MiClockView";
    public static final int DEFAULT_COLOR = Color.WHITE;
    public static final int DEFAULT_SIZE = 100;
    public static final int DEFAULT_NUMBER_SIZE = 12;
    public static final int MSG_FLUSH = 1;
    private boolean supportDrag;
    private int mLastX;
    private int mLastY;
    private Paint mPanelPaint;
    private Paint mNeedlePaint;
    private Paint mTextPaint;
    private int mColor = DEFAULT_COLOR;
    private int mSize;
    private int currentH;
    private int currentM;
    private int currentS;
    private int numberSize;
    private float offsetX;
    private float offsetY;
    private Path mSecondPath;
    private Camera camera;
    private Matrix matrix3D;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            flush();
            handler.sendEmptyMessageDelayed(MSG_FLUSH, 1000);
        }
    };

    public ClockView(Context context) {
        this(context, null);
    }

    public ClockView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ClockView);
        supportDrag = typedArray.getBoolean(R.styleable.ClockView_support_drag, false);
        numberSize = dp2px((int) typedArray.getDimension(R.styleable.ClockView_number_size, DEFAULT_NUMBER_SIZE));
        typedArray.recycle();
        init();
    }

    private void init() {
        mPanelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPanelPaint.setColor(mColor);
        mPanelPaint.setStrokeWidth(1);

        mNeedlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mNeedlePaint.setColor(mColor);
        mNeedlePaint.setStrokeWidth(3);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setStrokeWidth(1);
        mTextPaint.setColor(mColor);
        mTextPaint.setTextSize(numberSize);
        Rect rect = new Rect();
        mTextPaint.getTextBounds("3", 0, 1, rect);
        offsetX = rect.width();
        offsetY = rect.height();
        mSecondPath = new Path();
        matrix3D = new Matrix();
        camera = new Camera();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.sendEmptyMessage(MSG_FLUSH);
    }

    /**
     * 每秒刷新界面
     */
    private void flush() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH-mm-ss");
        String format = sdf.format(date);
        String[] split = format.split("-");
        int len = split.length;
        currentH = Integer.parseInt(split[0]);
        currentM = Integer.parseInt(split[1]);
        currentS = Integer.parseInt(split[2]);
        invalidate();
    }

    /**
     * 必须为正方形
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        final int size;
        if (wMode == MeasureSpec.EXACTLY && hMode == MeasureSpec.EXACTLY) {
            size = Math.min(wSize, hSize);
        } else if (wMode == MeasureSpec.EXACTLY || hMode == MeasureSpec.EXACTLY) {
            size = wMode == MeasureSpec.EXACTLY ? wSize : hSize;
        } else {
            size = dp2px(DEFAULT_SIZE);
        }
        setMeasuredDimension(size, size);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mSize = w;
        mSecondPath.moveTo(w / 2, w / 8);
        mSecondPath.lineTo(w / 2 + w / 20, w / 8 + w / 20);
        mSecondPath.lineTo(w / 2 - w / 20, w / 8 + w / 20);
        mSecondPath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawPanel(canvas);
        drawNeedle(canvas);
    }

    /**
     * 绘制表针
     *
     * @param canvas
     */
    private void drawNeedle(Canvas canvas) {
        final int size = mSize;
        final int curH = currentH;
        final int curM = currentM;
        final int curS = currentS;
        float hPercent = curH % 12 * 1f / 12;
        float mPercent = curM * 1f / 60;
        float sPercent = curS * 1f / 60;
        float hDegree = hPercent * 360 + mPercent * 30;
        float mDegree = mPercent * 360 + 6 * sPercent;
        float sDegree = sPercent * 360;
        //画中心
        canvas.drawCircle(size >> 1, size >> 1, size / 20, mNeedlePaint);
        //画时针
        canvas.save();
        canvas.rotate(hDegree, size >> 1, size >> 1);
        canvas.drawLine(size >> 1, size / 4, size >> 1, size * 9 / 20, mNeedlePaint);
        canvas.restore();
        //画分针
        canvas.save();
        canvas.rotate(mDegree, size >> 1, size >> 1);
        canvas.drawLine(size >> 1, size / 6, size >> 1, size * 9 / 20, mNeedlePaint);
        canvas.restore();
        //画秒针
        canvas.save();
        canvas.rotate(sDegree, size >> 1, size >> 1);
        canvas.drawPath(mSecondPath, mNeedlePaint);
        canvas.restore();
    }

    /**
     * 绘制表盘
     *
     * @param canvas
     */
    private void drawPanel(Canvas canvas) {
        final int size = mSize;
        //画秒格
        canvas.save();
        for (int i = 0; i < 60; i++) {
            canvas.drawLine(size >> 1, size >> 3, size >> 1, size >> 4, mPanelPaint);
            canvas.rotate(6, size >> 1, size >> 1);
        }
        canvas.restore();
        //画外圈
        canvas.drawCircle(size >> 1, size >> 1, (size >> 1) - 10, mTextPaint);
        //画数字
        canvas.drawText(String.valueOf(3), size - 10 - offsetX / 2, size >> 1, mTextPaint);
        canvas.drawText(String.valueOf(6), size >> 1, size - 10 + offsetY / 2, mTextPaint);
        canvas.drawText(String.valueOf(9), 10 - offsetX / 2, size >> 1, mTextPaint);
        canvas.drawText(String.valueOf(12), size >> 1, 10 + offsetY / 2, mTextPaint);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeMessages(MSG_FLUSH);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (supportDrag) {
            return drag3d(event);
        }
        return super.onTouchEvent(event);
    }

    @Nullable
    private boolean drag3d(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        final float x = event.getX();
        final float y = event.getY();
        float dx = x - mLastX;
        float dy = y - mLastY;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastX = (int) x;
                mLastY = (int) y;
                break;
            case MotionEvent.ACTION_MOVE:
                matrix3D.reset();
                final int size = mSize;
                float percent = Math.max(dx, dy) / size;
                if (dx * dx > dy * dy) {
                    ViewCompat.animate(this).rotationX(percent * 360);
                } else {
                    ViewCompat.animate(this).rotationY(percent * 360);
                }
                break;
            case MotionEvent.ACTION_UP:
                ViewCompat.animate(this).rotationX(0);
                ViewCompat.animate(this).rotationY(0);
                break;
            default:
                super.onTouchEvent(event);
        }
        return true;
    }

    private int dp2px(int dp) {
        return (int) (getResources().getDisplayMetrics().density * dp + 0.5);
    }
}
