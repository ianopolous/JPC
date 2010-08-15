/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.4

    A project from the Physics Dept, The University of Oxford

    Copyright (C) 2007-2010 The University of Oxford

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 
    Details (including contact information) can be found at: 

    jpc.sourceforge.net
    or the developer website
    sourceforge.net/projects/jpc/

    Conceived and Developed by:
    Rhys Newman, Ian Preston, Chris Dennis

    End of licence header
*/

package org.jpc.j2se;

import java.util.*;
import java.awt.event.KeyEvent;

/**
 * 
 * @author Chris Dennis
 */
public class KeyMapping
{
    private static Map<Integer, Byte> scancodeTable = new HashMap<Integer, Byte>();
    
    static 
    {
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_ESCAPE), Byte.valueOf((byte)0x01));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_1), Byte.valueOf((byte)0x02));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_2), Byte.valueOf((byte)0x03));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_3), Byte.valueOf((byte)0x04));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_4), Byte.valueOf((byte)0x05));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_5), Byte.valueOf((byte)0x06));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_6), Byte.valueOf((byte)0x07));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_7), Byte.valueOf((byte)0x08));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_8), Byte.valueOf((byte)0x09));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_9), Byte.valueOf((byte)0x0a));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_0), Byte.valueOf((byte)0x0b));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_MINUS), Byte.valueOf((byte)0x0c));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_EQUALS), Byte.valueOf((byte)0x0d));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_BACK_SPACE), Byte.valueOf((byte)0x0e));

	scancodeTable.put(Integer.valueOf(KeyEvent.VK_TAB), Byte.valueOf((byte)0xf));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_Q), Byte.valueOf((byte)0x10));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_W), Byte.valueOf((byte)0x11));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_E), Byte.valueOf((byte)0x12));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_R), Byte.valueOf((byte)0x13));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_T), Byte.valueOf((byte)0x14));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_Y), Byte.valueOf((byte)0x15));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_U), Byte.valueOf((byte)0x16));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_I), Byte.valueOf((byte)0x17));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_O), Byte.valueOf((byte)0x18));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_P), Byte.valueOf((byte)0x19));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_OPEN_BRACKET), Byte.valueOf((byte)0x1a));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_CLOSE_BRACKET), Byte.valueOf((byte)0x1b));

	scancodeTable.put(Integer.valueOf(KeyEvent.VK_ENTER), Byte.valueOf((byte)0x1c));
	
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_CONTROL), Byte.valueOf((byte)0x1d));

	scancodeTable.put(Integer.valueOf(KeyEvent.VK_A), Byte.valueOf((byte)0x1e));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_S), Byte.valueOf((byte)0x1f));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_D), Byte.valueOf((byte)0x20));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_F), Byte.valueOf((byte)0x21));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_G), Byte.valueOf((byte)0x22));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_H), Byte.valueOf((byte)0x23));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_J), Byte.valueOf((byte)0x24));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_K), Byte.valueOf((byte)0x25));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_L), Byte.valueOf((byte)0x26));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_SEMICOLON), Byte.valueOf((byte)0x27));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_QUOTE), Byte.valueOf((byte)0x28));

	scancodeTable.put(Integer.valueOf(KeyEvent.VK_BACK_QUOTE), Byte.valueOf((byte)0x29));

	scancodeTable.put(Integer.valueOf(KeyEvent.VK_SHIFT), Byte.valueOf((byte)0x2a));
	
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_BACK_SLASH), Byte.valueOf((byte)0x2b));
	
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_Z), Byte.valueOf((byte)0x2c));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_X), Byte.valueOf((byte)0x2d));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_C), Byte.valueOf((byte)0x2e));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_V), Byte.valueOf((byte)0x2f));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_B), Byte.valueOf((byte)0x30));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_N), Byte.valueOf((byte)0x31));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_M), Byte.valueOf((byte)0x32));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_COMMA), Byte.valueOf((byte)0x33));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_PERIOD), Byte.valueOf((byte)0x34));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_SLASH), Byte.valueOf((byte)0x35));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_SHIFT), Byte.valueOf((byte)0x36));

	//37 KPad *

	//38 Missing L-Alt - Java does not pickup
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_ALT), Byte.valueOf((byte)0x38));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_SPACE), Byte.valueOf((byte)0x39));

	scancodeTable.put(Integer.valueOf(KeyEvent.VK_CAPS_LOCK), Byte.valueOf((byte)0x3a));

	scancodeTable.put(Integer.valueOf(KeyEvent.VK_F1), Byte.valueOf((byte)0x3b));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_F2), Byte.valueOf((byte)0x3c));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_F3), Byte.valueOf((byte)0x3d));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_F4), Byte.valueOf((byte)0x3e));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_F5), Byte.valueOf((byte)0x3f));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_F6), Byte.valueOf((byte)0x40));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_F7), Byte.valueOf((byte)0x41));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_F8), Byte.valueOf((byte)0x42));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_F9), Byte.valueOf((byte)0x43));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_F10), Byte.valueOf((byte)0x44));

	//45 Missing Num-Lock - Java does not pickup

	scancodeTable.put(Integer.valueOf(KeyEvent.VK_SCROLL_LOCK), Byte.valueOf((byte)0x46));
	
	//47-53 are Numpad keys

	//54-56 are not used

	scancodeTable.put(Integer.valueOf(122), Byte.valueOf((byte)0x57)); // F11
	scancodeTable.put(Integer.valueOf(123), Byte.valueOf((byte)0x58)); // F12

	//59-ff are unused (for normal keys)

	//Extended Keys
	//e0,1c KPad Enter
	//e0,1d R-Ctrl
	//e0,2a fake L-Shift
	//e0,35 KPad /
	//e0,36 fake R-Shift
	//e0,37 Ctrl + Print Screen
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_ALT_GRAPH), Byte.valueOf((byte)(0x38 | 0x80)));
	//e0,46 Ctrl + Break
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_HOME), Byte.valueOf((byte)(0x47 | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_UP), Byte.valueOf((byte)(0x48 | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_PAGE_UP), Byte.valueOf((byte)(0x49 | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_LEFT), Byte.valueOf((byte)(0x4b | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_RIGHT), Byte.valueOf((byte)(0x4d | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_END), Byte.valueOf((byte)(0x4f | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_DOWN), Byte.valueOf((byte)(0x50 | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_PAGE_DOWN), Byte.valueOf((byte)(0x51 | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_INSERT), Byte.valueOf((byte)(0x52 | 0x80)));
	scancodeTable.put(Integer.valueOf(KeyEvent.VK_DELETE), Byte.valueOf((byte)(0x53 | 0x80)));
	//e0,5b L-Win
	//e0,5c R-Win
	//e0,5d Context-Menu


	scancodeTable.put(Integer.valueOf(19), Byte.valueOf((byte)0xFF)); //Pause
    }

    public static byte getScancode(Integer keyCode)
    {
	try {
	    return scancodeTable.get(keyCode).byteValue();
	} catch (NullPointerException e) {
	    return (byte)0x00;
	}
    }

    private KeyMapping()
    {
    }
}
