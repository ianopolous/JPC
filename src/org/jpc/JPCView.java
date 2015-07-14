package org.jpc;

import android.content.Context;
import android.content.res.*;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.view.animation.*;
import android.os.*;
import android.app.*;
import android.inputmethodservice.*;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import java.util.logging.*;
import java.io.*;
import java.util.*;
import org.jpc.emulator.pci.peripheral.*;
import org.jpc.emulator.peripheral.*;
import org.jpc.emulator.*;
import org.jpc.j2se.*;

public class JPCView extends SurfaceView implements SurfaceHolder.Callback, KeyboardView.OnKeyboardActionListener
{

    private static final Logger LOGGING = Logger.getLogger(JPCView.class.getName());

    public static final int SCALING_ORIGINAL = 0;
    public static final int SCALING_PROPORTIONAL = 1;
    public static final int SCALING_STRETCH = 2;
    
    private org.jpc.emulator.peripheral.Keyboard keyboard;
    private volatile Updater updater;
    private volatile Executer executer;
    private volatile PC pc;
    private DefaultVGACard vga;
    private int scalingMode = SCALING_STRETCH;
    private volatile boolean clearBackground;
    private volatile boolean menuVisible = false;
    private android.os.Handler handler;
    private KeyboardView keyboardView;
    private android.inputmethodservice.Keyboard current, qwerty, symbols, pckeys, previous;
    public JPC act;
    public volatile float scale = 1.0f;
    public Set<Integer> shifted = new HashSet();
    
    public JPCView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public void initialiseGUI(Context context, KeyboardView keyview)
    {
        System.out.println("Initialising keyboard view");
        keyboardView = keyview;
        ((LatinKeyboardView)keyboardView).setJPC(this);
        qwerty = new LatinKeyboard(context, R.xml.qwerty_tablet);
        symbols = new LatinKeyboard(context, R.xml.symbols_tablet);
        pckeys = new LatinKeyboard(context, R.xml.pckeys_tablet);
        current = pckeys;
        keyboardView.setOnKeyboardActionListener((OnKeyboardActionListener) this);  

        keyboardView.setKeyboard(current);
        keyboardView.setVisibility(View.VISIBLE);
        keyboardView.requestLayout();

        final SurfaceHolder holder = getHolder();
        holder.setKeepScreenOn(true);
        holder.addCallback(this);

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        shifted.add(208);
        shifted.add(209);
        shifted.add(276);
        shifted.add(211);
        shifted.add(275);
        shifted.add(210);
        shifted.add(212);
        shifted.add(215);
        shifted.add(216);
        shifted.add(207);
    }

    public void setActivity(JPC j)
    {
        this.act = j;
    }

    public void setHandler(android.os.Handler handler)
    {
        this.handler = handler;
    }

    public synchronized void setPC(PC pc)
    {
        this.pc = pc;
        clearBackground = true;
        vga = (DefaultVGACard) pc.getComponent(VGACard.class);
        keyboard = (org.jpc.emulator.peripheral.Keyboard) pc.getComponent(org.jpc.emulator.peripheral.Keyboard.class);
        updater = new Updater();
        executer = new Executer(pc);
    }

    public void toggleKeyboardVisibility() {  
        int visibility = keyboardView.getVisibility();  
        switch (visibility) {  
        case View.VISIBLE:
            System.out.println("Hiding keyboard");
            keyboardView.setVisibility(View.INVISIBLE);
            bringToFront();
            AlphaAnimation alpha1 = new AlphaAnimation(0.0F, 0.0F);
            alpha1.setDuration(0); 
            alpha1.setFillAfter(true);
            keyboardView.startAnimation(alpha1);
            break;  
        case View.GONE:  
        case View.INVISIBLE:
            System.out.println("Showing keyboard");
            keyboardView.setVisibility(View.VISIBLE);
            keyboardView.bringToFront();
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            {
                System.out.println("Making keyboard translucent due to landscape mode");
                AlphaAnimation alpha = new AlphaAnimation(0.7F, 0.7F);
                alpha.setDuration(0); 
                alpha.setFillAfter(true);
                keyboardView.startAnimation(alpha);
            }
            else
            {
                AlphaAnimation alpha = new AlphaAnimation(1.0F, 1.0F);
                alpha.setDuration(0); 
                alpha.setFillAfter(true);
                keyboardView.startAnimation(alpha);
            }
            System.out.printf("keyboardView: visible=%d, x=%f, y=%f, w=%d, h=%d\n", keyboardView.getVisibility(), keyboardView.getX(), keyboardView.getY(), keyboardView.getWidth(), keyboardView.getHeight());
            keyboardView.requestLayout();
            break;
        default:
            System.out.println("Unknown visibility: " + visibility);
            break;
        }  
    }  

    public boolean isKeyboardVisible()
    {
        return keyboardView.getVisibility() == View.VISIBLE;
    }

    private void toggleSymbols()
    {
        if (current == qwerty)
        {
            current = symbols;
            keyboardView.setKeyboard(symbols);
        } else {
            current = qwerty;
            keyboardView.setKeyboard(qwerty);
        }
        current.setShifted(false);

        toggleKeyboardVisibility();
        toggleKeyboardVisibility();
    }

    private void togglePCKeys()
    {
        if (current == pckeys)
        {
            current = qwerty;
        } else if (current == qwerty)
        {
            current = symbols;
        } else
        {
            current = pckeys;
        }
        keyboardView.setKeyboard(current);
        current.setShifted(false);

        toggleKeyboardVisibility();
        toggleKeyboardVisibility();
    }

    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        return doKeyDown(keyCode, msg);
    }

    public boolean onKeyUp(int keyCode, KeyEvent msg) {
        return doKeyUp(keyCode, msg);
    }

    /**
     * Handles a key-down event.
     * 
     * @param keyCode the key that was pressed
     * @param msg the original event object
     * @return true
     */
    boolean doKeyDown(int keyCode, KeyEvent msg) {
        if (keyCode == KeyEvent.KEYCODE_MENU) //Menu
        {
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_SEARCH) // search
        {
            System.out.println("Search");
        }
        else if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            act.openOptionsMenu();
            return true;
        }
        keyboard.keyPressed(KeyMapping.getScancode(Integer.valueOf(keyCode)));
        return true;
    }

    /**
     * Handles a key-up event.
     * 
     * @param keyCode the key that was pressed
     * @param msg the original event object
     * @return true if the key was handled and consumed, or else false
     */
    boolean doKeyUp(int keyCode, KeyEvent msg) {
        if (keyCode == KeyEvent.KEYCODE_MENU) //Menu
            return false;
        
        keyboard.keyReleased(KeyMapping.getScancode(Integer.valueOf(keyCode)));
        return true;
    }

    public void onKey(int primaryCode, int[] keyCodes) 
    {
        //System.out.println("ONKEY " + primaryCode);
        //doKeyDown(primaryCode, null);
    }

    public void onPress(int primaryCode)
    {
        System.out.println("ONPRESS " + primaryCode);
        if (primaryCode == -1)
        {
            doKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT, null);
            if (!current.isShifted())
                current.setShifted(true);
            else
                current.setShifted(false);
            keyboardView.invalidateAllKeys();
        } else if (primaryCode == -2) {
            
        } else if (primaryCode == KeyEvent.KEYCODE_ALT_RIGHT)
        {
            //simulate a colon
            doKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT, null);
            doKeyDown(KeyEvent.KEYCODE_SEMICOLON, null);
            doKeyUp(KeyEvent.KEYCODE_SEMICOLON, null);
            doKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT, null);
        } else if ((current == symbols) && shifted.contains(primaryCode))
        {
            // -200 to get actual keycode (for uniqueness)
            doKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT, null);
            doKeyDown(primaryCode-200, null);
            doKeyUp(primaryCode-200, null);
            doKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT, null);
        } else if (!current.isShifted())
            doKeyDown(primaryCode, null);
        else
        {
            System.out.println("SHIFT DOWN KEY");
            doKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT, null);
            doKeyDown(primaryCode, null);
        }
    }

    public void onRelease(int primaryCode)
    {
        System.out.println("ONRELEASE " + primaryCode);
        if (primaryCode == -1)
        {
            doKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT, null);
        } else if (primaryCode == -2) {
            toggleSymbols();
        } else if (primaryCode == -3)
        {
            togglePCKeys();
        } else if (primaryCode == KeyEvent.KEYCODE_ALT_RIGHT)
        {
            
        } else {
            doKeyUp(primaryCode, null);
            if (current.isShifted()){System.out.println("SHIFT DOWN KEY RELEASE");
                doKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT, null);}
        }
    }

    public void onText(CharSequence text)
    {}

    public void swipeDown()
    {}

    public void swipeLeft()
    {}

    public void swipeRight()
    {}

    public void swipeUp()
    {}

    public boolean onTouchEvent(MotionEvent ev)
    {
        int[] tmp = new int[2];
        getLocationOnScreen(tmp);
        System.out.printf("Mouse EVENT: %d pointers, action %d\n", ev.getPointerCount(), ev.getAction());
        if (ev.getPointerCount() == 1)
        {
            int x = (int)((ev.getX()-tmp[0])/scale);
            int y = (int)((ev.getY()-tmp[1])/scale);
            if ((x < 0) || (y < 0) || (x > vga.getWidth()) || (y > vga.getHeight()))
                return false;
            int action = 0;
            if (ev.getAction() == MotionEvent.ACTION_DOWN)
                action = 0;
            else if (ev.getAction() == MotionEvent.ACTION_MOVE)
                action = 1;
            else if (ev.getAction() == MotionEvent.ACTION_UP)
                action = 2;
            if (action >= 0)
                sendMouseEvent(x, y, action);
        } else if (ev.getPointerCount() == 2)
        {
            int x1 = (int)((ev.getX(0)-tmp[0])/scale);
            int y1 = (int)((ev.getY(0)-tmp[1])/scale);
            int x2 = (int)((ev.getX(1)-tmp[0])/scale);
            int y2 = (int)((ev.getY(1)-tmp[1])/scale);

            if ((x1 < 0) || (y1 < 0) || (x1 > vga.getWidth()) || (y1 > vga.getHeight()))
                return false;
            System.out.println("Sending dual mouse event");
            int action = 0;
            if (ev.getAction() == MotionEvent.ACTION_POINTER_2_DOWN)
                action = 0;
            else if (ev.getAction() == MotionEvent.ACTION_MOVE)
                action = 1;
            else if (ev.getAction() == MotionEvent.ACTION_POINTER_1_UP)
            {
                action = 2;
                x = x2;
                y = y2;
            }
            else if (ev.getAction() == MotionEvent.ACTION_POINTER_2_UP)
            {
                action = 2;
                x = x1;
                y = y1;
            }
            if (action >= 0)
                sendDualMouseEvent(x1, y1, x2, y2, action);
        }
        return true;
    }

    private synchronized void sendDualMouseEvent(int x1, int y1, int x2, int y2, int action)
    {
        // send right mouse
        if (action == 0) // down
        {
            x = x1;
            y = y1;
            sendRightMouseState(true);
        }
        else if (action == 2) // up
        {
            lastAction = 2;
            sendRightMouseState(false);
        }
        else if (action == 1) // move
        {
            sendMouseEvent(x1, y1, 1);
        }
    }

    private int x,y, lastAction;
    private long lastTime;
    private boolean drag = false;
    private double mouseScale = 1.5;
    private int mouseButtons;

    private synchronized void sendMouseEvent(int dx, int dy, int action)
    {
        System.out.println("SendMouseEvent " + dx + ", " + dy + ", action " + action);
        if (action == 0)
        {
            x=dx;
            y=dy;
            lastAction = 0;
            lastTime = System.nanoTime();
            mouseButtons |= 1;
            drag = false;
            return;
        }
        if ((action == 2) && (lastAction == 0))
        {
            // send left button down and up
            lastAction = 2;
            x=dx;
            y=dy;
            mouseButtons &= ~1;
            sendLeftMouseState(true);
            sendLeftMouseState(false);
            
            lastTime = System.nanoTime();
            return;
        }
        else if (action == 1)
        {
            long time = System.nanoTime();
            if (!drag && (time - lastTime > 500000000L) && (lastAction == 0))
            {
                sendLeftMouseState(true);
                drag = true;
                Vibrator v = (Vibrator) act.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(50);
            }
            //
            keyboard.putMouseEvent((int)(mouseScale*(dx-x)), (int)(mouseScale*(dy-y)), 0, mouseButtons);
            x=dx;
            y=dy;
            lastAction = 1;
            lastTime = System.nanoTime();
        }
        else if (action == 2)
        {
            if (drag)
                sendLeftMouseState(false);
        }
    }

    private void sendLeftMouseState(boolean down)
    {System.out.println("Left mouse " + down);
        if (down)
            keyboard.putMouseEvent(0, 0, 0, 1);
        else
            keyboard.putMouseEvent(0, 0, 0, 0);
    }

    private void sendRightMouseState(boolean down)
    {System.out.println("Right mouse " + down);
        if (down)
            keyboard.putMouseEvent(0, 0, 0, 4);
        else
            keyboard.putMouseEvent(0, 0, 0, 0);
    }

    // SCREEN stuff

    public void setVGACard(DefaultVGACard v) {
        vga = v;
    }

    public void setScalingMode(int mode) {
        if (scalingMode != mode) {
            scalingMode = mode;
            requestLayout();
        }
    }

    public void onImageUpdate(int[] data, int xmin, int xmax, int ymin, int ymax) {
        SurfaceHolder holder = getHolder();
        if (holder == null)
            return;
        Canvas c = null;
        try
        {
            c = holder.lockCanvas(null);
            if (c != null)
                synchronized (holder) {
                    c.scale(scale, scale);
                    c.drawRGB(0, 0, 0);
                    c.drawBitmap(data, 0, vga.getDisplaySize()[0], 0, 0, vga.getDisplaySize()[0], vga.getDisplaySize()[1], false, null);
                }
        } finally
        {
            if (c != null)
                holder.unlockCanvasAndPost(c);
        }
    }

    public void surfaceCreated(SurfaceHolder holder)
    {
        System.out.println("JPCView.surfaceCreated");
        holder.setKeepScreenOn(true);
        holder.addCallback(this);
    }

    public void surfaceDestroyed(SurfaceHolder holder)
    {
        System.out.println("JPCView.Surface Destroyed");
        stopThreads();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        System.out.println("JPCView.surface changed: " + width + " by " + height);
        holder.setKeepScreenOn(true);
        holder.addCallback(this);
    }

    public void resizeDisplay(int width, int height)
    {
        System.out.println("JPCView.resizeDisplay("+width + ", " + height+")");
        act.runOnUiThread(new Runnable() {public void run(){
            requestLayout();
        }});
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int specHeight = MeasureSpec.getSize(heightMeasureSpec)- getPaddingTop() - getPaddingBottom();
        int w, h;
        
        System.out.println("JPCView.onMeasure entry** " + widthMeasureSpec +"," + heightMeasureSpec + " -> " + specWidth+"," + specHeight);
        System.out.printf("JPCView.onMeasure Padding l: %d, r: %d, t:%d, b: %d\n", getPaddingLeft(), getPaddingRight(), getPaddingTop(), getPaddingBottom());
        if (vga == null)
        {
            scale = 1.0f;
            setMeasuredDimension(specWidth, specHeight);
            return;
        }
        switch (scalingMode) {
        case SCALING_ORIGINAL:
            w = vga.getDisplaySize()[0];
            h = vga.getDisplaySize()[1];
            break;
        case SCALING_STRETCH:
            if (specWidth >= specHeight) {
                w = specWidth;
                h = specHeight;
                break;
            }
            // fall through
        case SCALING_PROPORTIONAL:
            h = specHeight;
            w = h * vga.getDisplaySize()[0] / vga.getDisplaySize()[1];
            if (w > specWidth) {
                w = specWidth;
                h = w * vga.getDisplaySize()[1] / vga.getDisplaySize()[0];
            }
            break;
        default:
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        System.out.println("JPCView.onMeasure---- setting to " + w + "," + h);
        setMeasuredDimension(w, h);
        scale = Math.min(((float)w)/vga.getDisplaySize()[0], ((float)h)/vga.getDisplaySize()[1]);
        System.out.println("JPCView.onMeasure Set scale to " + scale);
        //setMeasuredDimension(vga.getDisplaySize()[0], vga.getDisplaySize()[1]);
        //api 11
        //setScaleX(((float)specWidth)/vga.getDisplaySize()[0]);
        //setScaleY(((float)specHeight)/vga.getDisplaySize()[1]);
    }

    protected void onSizeChanged (int w, int h, int oldw, int oldh)
    {
        System.out.printf("JPCView size changed from (%d, %d) to (%d, %d)\n", oldw, oldh, w, h);
    }

    public synchronized void saveState(Bundle outstate)
    {
        int[] rawImageData = vga.getDisplayBuffer();
        byte[] dummy = new byte[rawImageData.length * 4];
        for (int i = 0, j = 0; i < rawImageData.length; i++) {
            int val = rawImageData[i];
            dummy[j++] = (byte) (val >> 24);
            dummy[j++] = (byte) (val >> 16);
            dummy[j++] = (byte) (val >> 8);
            dummy[j++] = (byte) (val);
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bout);
        try {
            output.writeInt(rawImageData.length);
            output.write(dummy);
            output.flush();
        } catch (IOException e) {e.printStackTrace();}
        
        outstate.putByteArray("vga", bout.toByteArray());
    }

    public void loadState(Bundle b)
    {
        if (b == null)
            return;
        try {
            byte[] screen = b.getByteArray("vga");
            InputStream in = new ByteArrayInputStream(screen);
            DataInputStream input = new DataInputStream(in);
            int len = input.readInt();
            int[] rawImageData = vga.getDisplayBuffer();
            if (len != rawImageData.length) {
                throw new IOException("Image size not consistent with saved image state");
            }
            byte[] dummy = new byte[len * 4];
            input.readFully(dummy);
            for (int i = 0, j = 0; i < len; i++) {
                int val = 0;
                val |= (0xff & dummy[j++]) << 24;
                val |= (0xff & dummy[j++]) << 16;
                val |= (0xff & dummy[j++]) << 8;
                val |= 0xff & dummy[j++];
                
                rawImageData[i] = val;
            }
        } catch (Exception e) {e.printStackTrace();}
    }

    public synchronized void startThreads() 
    {
        System.out.println("start threads");
        if (!updater.started)
            updater.start();
        else
        {
            System.out.println("Updater already started");
            //if (updater.running)
            //    updater.running = false; // just incase
            //else
            if (!updater.running) // this one terminated
            {
                System.out.println("Making new Updater");
                updater = new Updater();
                updater.start();
            }
        }
        if (!executer.started)
            executer.start();
        else
        {
            System.out.println("Executor already started");
            //if (executer.running)
            //    executer.running = false; // just incase
            //else
            if (!executer.running) // this one terminated
            {
                System.out.println("Making new executer");
                executer = new Executer(pc);
                executer.start();
            }
        }
    }

    public synchronized void stopThreads() 
    {
        System.out.println("stop threads");
        if (updater != null)
            updater.destroy();
        if (executer != null)
            executer.destroy();
    }

    public synchronized boolean isRunning() 
    {
        if (updater == null)
            return false;
        return updater.running;
    }

    class Executer extends Thread
    {
        private volatile boolean running = false, started = false;
        private PC pc;

        public Executer(PC pc)
        {
            super("PC Execution Thread");
            this.pc = pc;
        }

        public void run() 
        {
            LOGGING.log(Level.INFO, "Starting PC execution");
            running = true;
            started = true;
            pc.start();
            while (running)
            {
                try
                {
                    pc.execute();
                } catch (Exception e)
                {
                    e.printStackTrace();
                    pc.getProcessor().printState();
                    return;
                }
            }
            pc.stop();
            LOGGING.log(Level.INFO, "Ending PC execution loop");
        }

        public void halt() 
        {
            running = false;
            LOGGING.log(Level.INFO, "PC Stopped");
        }

        public void destroy()
        {
            LOGGING.log(Level.INFO, "Ending PC execution thread");
            if (started)
            {
                boolean retry = true;
                running = false;
                while (retry) {
                    try {
                        executer.interrupt();
                        executer.join();
                        retry = false;
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    class Updater extends Thread 
    {
        private volatile boolean running = false, started = false;

        public Updater() 
        {
            super("PC Screen Updater Task");
        }

        public void run() 
        {
            LOGGING.log(Level.INFO, "Starting screen updater");
            started = true;
            running = true;
            while (running) 
            {
                //vga.prepareUpdate();
                vga.updateDisplay();
                
                int xmin = vga.getXMin();
                int xmax = vga.getXMax();
                int ymin = vga.getYMin();
                int ymax = vga.getYMax();
                int[] buf = vga.getDisplayBuffer();
                if (buf != null)
                    onImageUpdate(vga.getDisplayBuffer(), xmin, xmax, ymin, ymax);
                else
                    System.out.println("Null VGA buffer");
                //repaint(xmin, ymin, xmax - xmin + 1, ymax - ymin + 1);
                try 
                {
                    Thread.sleep(20);
                } 
                catch (InterruptedException e) {}
            }
            LOGGING.log(Level.INFO, "Updater thread ended...");
        }

        public void destroy() 
        {
            boolean retry = true;
            running = false;
            while (retry) {
                try {
                    updater.interrupt();
                    updater.join();
                    retry = false;
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
