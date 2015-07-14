package org.jpc.j2se;

import java.util.*;
import android.view.KeyEvent;
import tv.ouya.console.api.*;

public class KeyMapping
{
    private static Map<Integer, Byte> scancodeTable = new HashMap<Integer, Byte>();
    private static Map<Integer, Integer> symScancodeTable = new HashMap<Integer, Integer>();
    
    static 
    {
	scancodeTable.put(Integer.valueOf(111), Byte.valueOf((byte)0x01)); // only api 11+ has ESCAPE
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_1), Byte.valueOf((byte)0x02));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_2), Byte.valueOf((byte)0x03));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_3), Byte.valueOf((byte)0x04));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_4), Byte.valueOf((byte)0x05));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_5), Byte.valueOf((byte)0x06));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_6), Byte.valueOf((byte)0x07));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_7), Byte.valueOf((byte)0x08));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_8), Byte.valueOf((byte)0x09));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_9), Byte.valueOf((byte)0x0a));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_0), Byte.valueOf((byte)0x0b));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_MINUS), Byte.valueOf((byte)0x0c));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_EQUALS), Byte.valueOf((byte)0x0d));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_DEL), Byte.valueOf((byte)0x0e)); //back space

	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_TAB), Byte.valueOf((byte)0xf));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_Q), Byte.valueOf((byte)0x10));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_W), Byte.valueOf((byte)0x11));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_E), Byte.valueOf((byte)0x12));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_R), Byte.valueOf((byte)0x13));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_T), Byte.valueOf((byte)0x14));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_Y), Byte.valueOf((byte)0x15));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_U), Byte.valueOf((byte)0x16));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_I), Byte.valueOf((byte)0x17));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_O), Byte.valueOf((byte)0x18));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_P), Byte.valueOf((byte)0x19));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_LEFT_BRACKET), Byte.valueOf((byte)0x1a));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_RIGHT_BRACKET), Byte.valueOf((byte)0x1b));

	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_ENTER), Byte.valueOf((byte)0x1c));
	
	scancodeTable.put(Integer.valueOf(113), Byte.valueOf((byte)0x1d)); // only android api level 11 added KEYCODE_CTRL_LEFT

	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_A), Byte.valueOf((byte)0x1e));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_S), Byte.valueOf((byte)0x1f));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_D), Byte.valueOf((byte)0x20));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_F), Byte.valueOf((byte)0x21));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_G), Byte.valueOf((byte)0x22));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_H), Byte.valueOf((byte)0x23));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_J), Byte.valueOf((byte)0x24));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_K), Byte.valueOf((byte)0x25));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_L), Byte.valueOf((byte)0x26));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_SEMICOLON), Byte.valueOf((byte)0x27));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_APOSTROPHE), Byte.valueOf((byte)0x28));

	//scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_BACK_QUOTE), Byte.valueOf((byte)0x29));

	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_SHIFT_LEFT), Byte.valueOf((byte)0x2a));
	
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_BACKSLASH), Byte.valueOf((byte)0x2b));
	
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_Z), Byte.valueOf((byte)0x2c));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_X), Byte.valueOf((byte)0x2d));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_C), Byte.valueOf((byte)0x2e));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_V), Byte.valueOf((byte)0x2f));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_B), Byte.valueOf((byte)0x30));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_N), Byte.valueOf((byte)0x31));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_M), Byte.valueOf((byte)0x32));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_COMMA), Byte.valueOf((byte)0x33));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_PERIOD), Byte.valueOf((byte)0x34));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_SLASH), Byte.valueOf((byte)0x35));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_SHIFT_RIGHT), Byte.valueOf((byte)0x36));

	//37 KPad *

	//38 Missing L-Alt - Java does not pickup, but android does :-)
        scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_ALT_LEFT), Byte.valueOf((byte)0x38));
	//scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_ALT_RIGHT), Byte.valueOf((byte)0x38)); used for colon
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_SPACE), Byte.valueOf((byte)0x39));

        scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_AT), Byte.valueOf((byte)0x3f)); //same as F5 key
	//scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_CAPS_LOCK), Byte.valueOf((byte)0x3a));

        // Function keys
	scancodeTable.put(Integer.valueOf(131), Byte.valueOf((byte)0x3b));
	scancodeTable.put(Integer.valueOf(132), Byte.valueOf((byte)0x3c));
	scancodeTable.put(Integer.valueOf(133), Byte.valueOf((byte)0x3d));
	scancodeTable.put(Integer.valueOf(134), Byte.valueOf((byte)0x3e));
	scancodeTable.put(Integer.valueOf(135), Byte.valueOf((byte)0x3f));
	scancodeTable.put(Integer.valueOf(136), Byte.valueOf((byte)0x40));
	scancodeTable.put(Integer.valueOf(137), Byte.valueOf((byte)0x41));
	scancodeTable.put(Integer.valueOf(138), Byte.valueOf((byte)0x42));
	scancodeTable.put(Integer.valueOf(139), Byte.valueOf((byte)0x43));
	scancodeTable.put(Integer.valueOf(140), Byte.valueOf((byte)0x44));

	//45 Missing Num-Lock - Java does not pickup

	//scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_SCROLL_LOCK), Byte.valueOf((byte)0x46));
	
	//47-53 are Numpad keys
        
	//54-56 are not used

	scancodeTable.put(Integer.valueOf(141), Byte.valueOf((byte)0x57)); // F11
	scancodeTable.put(Integer.valueOf(142), Byte.valueOf((byte)0x58)); // F12

	//59-ff are unused (for normal keys)

	//Extended Keys
	//e0,1c KPad Enter
	//e0,1d R-Ctrl
	//e0,2a fake L-Shift
	//e0,35 KPad /
	//e0,36 fake R-Shift
	//e0,37 Ctrl + Print Screen
	//scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_ALT_GRAPH), Byte.valueOf((byte)(0x38 | 0x80)));
	//e0,46 Ctrl + Break
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_HOME), Byte.valueOf((byte)(0x47 | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_DPAD_UP), Byte.valueOf((byte)(0x48 | 0x80)));
	//scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_PAGE_UP), Byte.valueOf((byte)(0x49 | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_DPAD_LEFT), Byte.valueOf((byte)(0x4b | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_DPAD_RIGHT), Byte.valueOf((byte)(0x4d | 0x80)));
	//scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_END), Byte.valueOf((byte)(0x4f | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_DPAD_DOWN), Byte.valueOf((byte)(0x50 | 0x80)));
	//scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_PAGE_DOWN), Byte.valueOf((byte)(0x51 | 0x80)));
	//scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_INSERT), Byte.valueOf((byte)(0x52 | 0x80)));
	//scancodeTable.put(Integer.valueOf(KeyEvent.KEYCODE_DEL), Byte.valueOf((byte)(0x53 | 0x80)));
	//e0,5b L-Win
	//e0,5c R-Win
	//e0,5d Context-Menu

        // OUYA controller keys
        scancodeTable.put(Integer.valueOf(tv.ouya.console.api.OuyaController.BUTTON_L1), Byte.valueOf((byte)0x1d)); // ctrl
        scancodeTable.put(Integer.valueOf(tv.ouya.console.api.OuyaController.BUTTON_U), Byte.valueOf((byte)0x39)); // space
        scancodeTable.put(Integer.valueOf(tv.ouya.console.api.OuyaController.BUTTON_Y), Byte.valueOf((byte)0x15)); // y
        scancodeTable.put(Integer.valueOf(tv.ouya.console.api.OuyaController.BUTTON_R2), Byte.valueOf((byte)0x2a)); // shift
        scancodeTable.put(Integer.valueOf(tv.ouya.console.api.OuyaController.BUTTON_R1), Byte.valueOf((byte)0x38)); // alt
        scancodeTable.put(Integer.valueOf(tv.ouya.console.api.OuyaController.BUTTON_A), Byte.valueOf((byte)0x01)); // Esc
        scancodeTable.put(Integer.valueOf(tv.ouya.console.api.OuyaController.BUTTON_O), Byte.valueOf((byte)0x1c)); // Enter

	//scancodeTable.put(Integer.valueOf(19), Byte.valueOf((byte)0xFF)); //Pause
    }

    static 
    {
        //symScancodeTable.put(new Integer(1), );
    }

    private static Integer last = null;
    public static final Integer SYM = new Integer(KeyEvent.KEYCODE_SYM);

    public static Integer lookupSym(Integer key)
    {
        try {
	    return symScancodeTable.get(key);
	} catch (NullPointerException e) {
            System.out.println("Unknown SYM key: " + key);
	    return new Integer(0);
	}
    }

    public static byte getScancode(Integer keyCode)
    {
	try {
            if ((last != null) && (last.equals(SYM)))
            {
                last = keyCode;
                return scancodeTable.get(lookupSym(keyCode)).byteValue();
            }
            last = keyCode;
	    return scancodeTable.get(keyCode).byteValue();
	} catch (NullPointerException e) {
            System.out.println("Unknown key: " + keyCode);
	    return (byte)0x00;
	}
    }

    public static void addScancode(Integer keycode, byte result)
    {
        scancodeTable.put(keycode, result);
    }

    private KeyMapping()
    {
    }
}
