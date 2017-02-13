package jp.plen.plencheck;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;

/**
 * Created by yuki on 3/28/15.
 */
public class HorizontalSeekbar extends SeekBar{

    String TAG = "Seekbar";

    public HorizontalSeekbar(Context context) {
        super(context);
    }

    public HorizontalSeekbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HorizontalSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(TAG, "w: " + String.valueOf(w) + " h:" + String.valueOf(h) + " oldw:" + String.valueOf(oldw) + " oldh:" + String.valueOf(oldh));
        super.onSizeChanged(h, w, oldh, oldw);
    }


    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }


    protected void onDraw(Canvas c) {
        // c.rotate(-90);
        //c.translate(-getHeight(), 0);
        super.onDraw(c);
    }


    private OnSeekBarChangeListener onChangeListener;
    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener onChangeListener){
        this.onChangeListener = onChangeListener;
    }

    @Override
    public synchronized void setProgress(int progress){
        super.setProgress(progress);
        Log.d(TAG, String.valueOf(progress));

        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                int i = (int) (getMax() * event.getX() / getWidth());
                setProgress(i);
                onChangeListener.onProgressChanged(this, i, true);
                break;
            case MotionEvent.ACTION_UP:
                onChangeListener.onStopTrackingTouch(this);
                break;

            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }
}

