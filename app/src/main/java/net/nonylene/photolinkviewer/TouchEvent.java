package net.nonylene.photolinkviewer;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class TouchEvent implements GestureDetector.OnGestureListener {

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
