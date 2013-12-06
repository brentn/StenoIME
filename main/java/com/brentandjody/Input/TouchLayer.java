package com.brentandjody.Input;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.brentandjody.R;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by brentn on 21/11/13.
 * Intercepts touches, and implements a dual-swipe-like interface for activating keys
 */
public class TouchLayer extends RelativeLayout {

    private static final int NUMBER_OF_FINGERS=2;
    private static FrameLayout LOADING_SPINNER;
    private static Paint PAINT;

    private List<TextView> keys = new ArrayList<TextView>();
    private boolean loading;
    private Path[] paths = new Path[NUMBER_OF_FINGERS];
    private int[] fingerIds = new int[NUMBER_OF_FINGERS];

    public TouchLayer(Context context) {
        super(context);
        initialize();
    }

    public TouchLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public TouchLayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    private OnStrokeListener onStrokeListener;
    public interface OnStrokeListener {
        public void onStroke(Set<String> stroke);
    }
    public void setOnStrokeListener(OnStrokeListener listener) {
        onStrokeListener = listener;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        keys.clear();
        enumerate_keys(this);
        LOADING_SPINNER = (FrameLayout) this.findViewById(R.id.overlay);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (LOADING_SPINNER == null) return;
        if (loading)
            LOADING_SPINNER.setVisibility(VISIBLE);
        else {
            LOADING_SPINNER.setVisibility(INVISIBLE);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        for (int i=0; i<NUMBER_OF_FINGERS; i++) {
            canvas.drawPath(paths[i], PAINT);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x, y;
        int i;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: case MotionEvent.ACTION_POINTER_DOWN: {
                i = event.getActionIndex();
                if (i >= NUMBER_OF_FINGERS) break;
                x = event.getX(i);
                y = event.getY(i);
                fingerIds[i] = event.getPointerId(i);
                paths[i].reset();
                paths[i].moveTo(x, y);
                toggleKeyAt(x, y);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                selectKeys(event);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: case MotionEvent.ACTION_POINTER_UP: {
                i = event.getActionIndex();
                if (i >= NUMBER_OF_FINGERS) break;
                int count = event.getPointerCount();
                if (count == 1) { //TODO: only complete if keys are selected
                    paths[0].reset();
                    invalidate();
                    if (anyKeysSelected()) {
                        onStrokeListener.onStroke(getStroke());
                    }
                }
                for (int n=0; n<NUMBER_OF_FINGERS; n++) {
                    if (event.getPointerId(i) == fingerIds[n]) {
                        paths[n].reset();
                        invalidate();
                    }
                }
                break;
            }
        }
        return true;
    }

    public void setLoading() {
        loading=true;
        invalidate();
    }

    public void clearLoading() {
        loading=false;
        invalidate();
    }

    private Set<String> getStroke() {
        // Reads stroke from keyboard, and resets the keys
        List<String> selected_keys = new LinkedList<String>();
        String name;
        for (TextView key : keys) {
            if (key.isSelected()) {
                if (key.getHint()!= null) {
                    name = key.getHint().toString();
                    selected_keys.add(name);
                }
                key.setSelected(false);
            }
        }
        return new LinkedHashSet<String>(selected_keys);
    }

    public boolean anyKeysSelected() {
        for (TextView key : keys) {
            if (key.isSelected()) return true;
        }
        return false;
    }

    private void initialize() {
        loading=false;
        setWillNotDraw(false);
        for (int x=0; x<NUMBER_OF_FINGERS; x++) {
            paths[x] = new Path();
        }
        PAINT = new Paint();
        if (getResources() != null)
            PAINT.setColor(getResources().getColor(android.R.color.background_light));
        else
            PAINT.setColor(Color.parseColor("#33B5E5"));
        PAINT.setStyle(Paint.Style.STROKE);
        PAINT.setStrokeJoin(Paint.Join.ROUND);
        PAINT.setStrokeCap(Paint.Cap.ROUND);
        PAINT.setStrokeWidth(6);
    }

    private void enumerate_keys(View v) {
        keys.add((TextView) v.findViewById(R.id.num));
        keys.add((TextView) v.findViewById(R.id.S));
        keys.add((TextView) v.findViewById(R.id.T));
        keys.add((TextView) v.findViewById(R.id.K));
        keys.add((TextView) v.findViewById(R.id.P));
        keys.add((TextView) v.findViewById(R.id.W));
        keys.add((TextView) v.findViewById(R.id.H));
        keys.add((TextView) v.findViewById(R.id.R));
        keys.add((TextView) v.findViewById(R.id.A));
        keys.add((TextView) v.findViewById(R.id.O));
        keys.add((TextView) v.findViewById(R.id.star));
        keys.add((TextView) v.findViewById(R.id.e));
        keys.add((TextView) v.findViewById(R.id.u));
        keys.add((TextView) v.findViewById(R.id.f));
        keys.add((TextView) v.findViewById(R.id.r));
        keys.add((TextView) v.findViewById(R.id.p));
        keys.add((TextView) v.findViewById(R.id.b));
        keys.add((TextView) v.findViewById(R.id.l));
        keys.add((TextView) v.findViewById(R.id.g));
        keys.add((TextView) v.findViewById(R.id.t));
        keys.add((TextView) v.findViewById(R.id.s));
        keys.add((TextView) v.findViewById(R.id.d));
        keys.add((TextView) v.findViewById(R.id.z));
    }

    private void toggleKeyAt(float x, float y) {
        Point pointer = new Point();
        Point offset = getScreenOffset(this);
        pointer.set((int)x+offset.x, (int)y+offset.y);
        for (TextView key : keys) {
            if (pointerOnKey(pointer, key)) {
                key.setSelected(! key.isSelected());
                return;
            }
        }
    }

    private void selectKeys(MotionEvent e) {
        Point pointer = new Point();
        Point offset = getScreenOffset(this);
        for (int i=0; i< e.getPointerCount(); i++) {
            if (i < NUMBER_OF_FINGERS) {
                for (int h=0; h<e.getHistorySize(); h++) {
                    pointer.set((int) e.getHistoricalX(i, h)+offset.x, (int) e.getHistoricalY(i, h)+offset.y);
                    if (paths[i].isEmpty()) {
                        paths[i].moveTo(e.getHistoricalX(i, h), e.getHistoricalY(i, h));
                    } else {
                        paths[i].lineTo(e.getHistoricalX(i, h), e.getHistoricalY(i, h));
                    }
                    for (TextView key : keys) {
                        if (pointerOnKey(pointer, key)) {
                            key.setSelected(true);
                        }
                    }
                }
            }
        }
    }

    private Point getScreenOffset(View v) {
        Point offset = new Point();
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        offset.set(location[0], location[1]);
        return offset;
    }

    private Boolean pointerOnKey(Point p, View key) {
        Point bottomRight = new Point();
        Point topLeft = getScreenOffset(key);
        bottomRight.set(topLeft.x+key.getWidth(), topLeft.y+key.getHeight());
        return !((p.x < topLeft.x) || (p.x > bottomRight.x) || (p.y < topLeft.y) || (p.y > bottomRight.y));
    }

    public void test_keys(String keystring) {
        ArrayList<View> keyViews = new ArrayList<View>();
        for (String key : keystring.split("/")) {
            findViewsWithText(keyViews, key.replace("-",""), FIND_VIEWS_WITH_TEXT);
            for (View keyView : keys) {
                if (((TextView) keyView).getHint()!=null)
                    if (((TextView) keyView).getHint().toString().equals(key))
                        keyView.setSelected(true);
            }
        }
        onStrokeListener.onStroke(getStroke());
    }

}
