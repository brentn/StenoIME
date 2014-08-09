package com.brentandjody.stenoime.Input;

import android.app.Activity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created by brent on 09/08/14.
 */
public abstract class SwipeActivity extends Activity {
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        gestureDetector = new GestureDetector( new SwipeDetector() );
    }

    private class SwipeDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {

            // Check movement along the Y-axis. If it exceeds SWIPE_MAX_OFF_PATH,
            // then dismiss the swipe.
            if( Math.abs( e1.getY() - e2.getY() ) > SWIPE_MAX_OFF_PATH )
                return false;

            // Swipe from right to left.
            // The swipe needs to exceed a certain distance (SWIPE_MIN_DISTANCE)
            // and a certain velocity (SWIPE_THRESHOLD_VELOCITY).
            if( e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs( velocityX ) > SWIPE_THRESHOLD_VELOCITY ) {
                next();
                return true;
            }

            // Swipe from left to right.
            // The swipe needs to exceed a certain distance (SWIPE_MIN_DISTANCE)
            // and a certain velocity (SWIPE_THRESHOLD_VELOCITY).
            if( e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs( velocityX ) > SWIPE_THRESHOLD_VELOCITY ) {
                previous();
                return true;
            }

            return false;
        }
    }

    @Override
    public boolean dispatchTouchEvent( MotionEvent ev ) {
        // TouchEvent dispatcher.
        if( gestureDetector != null ) {
            if( gestureDetector.onTouchEvent( ev ) )
                // If the gestureDetector handles the event, a swipe has been
                // executed and no more needs to be done.
                return true;
        }
        return super.dispatchTouchEvent( ev );
    }

    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        return gestureDetector.onTouchEvent( event );
    }

    protected abstract void previous();

    protected abstract void next();
}
