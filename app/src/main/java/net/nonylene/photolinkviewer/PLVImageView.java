package net.nonylene.photolinkviewer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;

public class PLVImageView extends ImageView implements GestureDetector.OnGestureListener {
    private  GestureDetector gestureDetector;
    public PLVImageView (Context context, AttributeSet attrs){
        super(context, attrs);
        gestureDetector = new GestureDetector(getContext(),this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){
        gestureDetector.onTouchEvent(e);
        return true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.v("INFO", "onSingleTapUp");
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        Log.v("INFO", "onSingleTapUp");
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.v("INFO", "onLongPress");

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.v("INFO", "onFling");
        return false;
    }

    @Override
    public boolean onDown(MotionEvent arg0) {
        Log.v("INFO", "onDown");
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.v("INFO", "onScroll");
        return false;
    }
}
