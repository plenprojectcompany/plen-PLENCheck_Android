package jp.plen.utils.activities;

import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * Created by Kazu on 17/02/07.
 */
public class RepeatListner implements OnTouchListener {

    String TAG = "RepeatLisner";
    private Handler handler = new Handler();

    private int initialInterval;
    private int repeatCount;
    private int normalInterval;
    private final View.OnClickListener clickListener;

    private View downView;

    private Runnable handlerRunnable = new Runnable(){
        @Override
        public void run(){
            int interval = (int)(Math.round(normalInterval*Math.pow(2, -0.015*repeatCount)));
            if(interval <= 0) interval = 0;

            repeatCount += 1;
            Log.d(TAG, String.valueOf(interval));
            Log.d(TAG, "repeatCount: " + String.valueOf(repeatCount));
            handler.postDelayed(this, interval);
            clickListener.onClick(downView);
        }
    };

    public RepeatListner(int initialInterval, int normalInterval, View.OnClickListener clickListener){
        if (clickListener == null) {
            throw new IllegalArgumentException("null runnable");
        }
        if ( initialInterval < 0 || normalInterval < 0){
            throw new IllegalArgumentException("negative interval");
        }

        this.initialInterval = initialInterval;
        this.normalInterval = normalInterval;
        this.clickListener = clickListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handler.removeCallbacks(handlerRunnable);
                handler.postDelayed(handlerRunnable, initialInterval);
                downView = v;
                downView.setPressed(true);
                this.repeatCount = 0;
                clickListener.onClick(v);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(handlerRunnable);
                downView.setPressed(false);
                downView = null;
                return true;
        }
        return false;
    }
}
