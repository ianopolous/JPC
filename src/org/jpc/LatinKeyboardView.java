package org.jpc;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import android.view.*;
import android.os.*;
import java.util.*;

public class LatinKeyboardView extends KeyboardView {

    static final int KEYCODE_OPTIONS = -100;
    private JPCView jpc;
    Vibrator v;

    public LatinKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LatinKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void onSizeChanged (int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        //System.out.printf("KeyboardView.onSizeChanged(%d, %d, %d, %d)\n", w, h, oldw, oldh);
    }

    public void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //System.out.printf("Keyboardview.onMeasure(%d, %d)\n", widthMeasureSpec, heightMeasureSpec);
        onSizeChanged(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec), 0, 0);
    }

    public void setJPC(JPCView jpc)
    {
        this.jpc = jpc;
        v = (Vibrator) jpc.act.getSystemService(Context.VIBRATOR_SERVICE);
    }

    protected boolean onLongPress(Key key) {
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else {
            return super.onLongPress(key);
        }
    }

    // allow multi-touch - multiple keys pressed at once
    List<Point> points = new ArrayList();

    private Point getClosest(int x, int y)
    {
        if (points.size() == 0)
            return null;
        Point p = null;
        for (Point r: points)
            if (distance(x, y, r) < distance(x, y, p))
                p = r;
        return p;
    }

    private double distance(int x, int y, Point p)
    {
        if (p == null)
            return Double.MAX_VALUE;
        return Math.sqrt((x-p.x)*(x-p.x) + (y-p.y)*(y-p.y));
    }

    private int getClosestIndex(int x, int y, int[] keys, List<Keyboard.Key> list)
    {
        int index = 0;
        int dsquared = Integer.MAX_VALUE;
        for (int i=0; i < keys.length; i++)
        {
            Keyboard.Key key = list.get(keys[i]);
            // centre of key
            if (key.squaredDistanceFrom(x, y) < dsquared)
            {
                index = keys[i];
                dsquared = key.squaredDistanceFrom(x, y);
            }
        }
        return index;
    }

    private void sendKey(int x, int y, int[] keyIndices, List<Keyboard.Key> keys, boolean down)
    {
        if (down)
        {
            try { // to keep code same for OUYA dumb arses catch a security exception
                v.vibrate(20);
            } catch (SecurityException e) {}
            jpc.onPress(keys.get(getClosestIndex(x, y, keyIndices, keys)).codes[0]);
        }
        else
            jpc.onRelease(keys.get(getClosestIndex(x, y, keyIndices, keys)).codes[0]);
    }

    public boolean onTouchEvent(MotionEvent e)
    {
        int x = (int) e.getX(0);
        int y = (int) e.getY(0);
        Keyboard kb = getKeyboard();
        List<Keyboard.Key> keys = kb.getKeys();
        
        if (e.getPointerCount() == 1)
        {
            if (e.getAction() == MotionEvent.ACTION_DOWN)
            {
                points.add(new Point(x, y));
                sendKey(x, y, kb.getNearestKeys(x, y), keys, true);
            }
            else
            {
                Point p = getClosest(x, y);
                if (p != null)
                {
                    points.remove(p);
                    sendKey(p.x, p.y, kb.getNearestKeys(p.x, p.y), keys, false);
                }
            }
            return true;
        } else if (e.getPointerCount() == 2)
        {
            int x2 = (int) e.getX(1);
            int y2 = (int) e.getY(1);
            if (e.getAction() == MotionEvent.ACTION_POINTER_2_DOWN)
            {
                points.add(new Point(x2, y2));
                sendKey(x2, y2, kb.getNearestKeys(x2, y2), keys, true);
                return true;
            }
            else if (e.getAction() == MotionEvent.ACTION_POINTER_1_UP)
            {
                Point p = getClosest(x, y);
                if (p != null)
                {
                    points.remove(p);
                    sendKey(p.x, p.y, kb.getNearestKeys(p.x, p.y), keys, false);
                }
                return true;
            }
            else if (e.getAction() == MotionEvent.ACTION_POINTER_2_UP)
            {
                Point p = getClosest(x2, y2);
                if (p != null)
                {
                    points.remove(p);
                    sendKey(p.x, p.y, kb.getNearestKeys(p.x, p.y), keys, false);
                }
                return true;
            }
            else
            {
                System.out.println("Unknown action in keyboard view: " + e.getAction());
                return super.onTouchEvent(e);
            }
        }
        else if (e.getPointerCount() == 3)
        {
            return false;
        }
        else if (e.getPointerCount() == 4)
        {
            return false;
        }
        else
            return false;
    }

    class Point
    {
        int x, y;

        Point(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
    }
}
