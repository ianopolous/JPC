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

package org.jpc.emulator.motherboard;

import java.io.*;
import java.nio.charset.Charset;
import java.util.logging.*;

import org.jpc.emulator.*;

/**
 * This class provides a <code>Bios</code> implementation for the emulated
 * machines system bios. The system bios is loaded so that it runs up to address
 * 0x100000 (1M).
 * <p>
 * IO ports <code>0x400-0x403</code> are registered for debugging output.  Byte
 * writes cause ASCII characters to be written to standard output, and word
 * writes indicate a BIOS panic at the written value line number.
 * <p>
 * IO port <code>0x8900</code> is registered for system shutdown requests.
 * Currently this triggers a debugging output, but does not actually shutdown
 * the machine.
 * @author Chris Dennis
 */
public class SystemBIOS extends Bios implements IOPortCapable
{
    private static final Logger LOGGING = Logger.getLogger(SystemBIOS.class.getName());

    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private boolean ioportRegistered;

    /**
     * Loads the system bios from the resource <code>image</code>.
     * @param image system bios resource name.
     * @throws java.io.IOException propogated from the resource load.
     * @throws java.util.MissingResourceException propogated from the resource load
     */
    public SystemBIOS(String image) throws IOException
    {
        super(image);
        ioportRegistered = false;

    }

    public void loadState(DataInput input) throws IOException
    {
        super.loadState(input);
        ioportRegistered = false;
    }

    public int[] ioPortsRequested()
    {
        return new int[]{0x400, 0x401, 0x402, 0x403, 0x8900};
    }

    public void ioPortWriteByte(int address, int data)
    {
        switch (address) {
            /* Bochs BIOS Messages */
            case 0x402:
            case 0x403:
                print(new String(new byte[]{(byte) data}, US_ASCII));
                break;
            case 0x8900:
                LOGGING.log(Level.INFO, "attempted shutdown");
                break;
            default:
        }
    }

    public void ioPortWriteWord(int address, int data)
    {
        switch (address) {
            /* Bochs BIOS Messages */
            case 0x400:
            case 0x401:
                LOGGING.log(Level.SEVERE, "panic in rombios.c at line {0,number,integer}", Integer.valueOf(data));
        }
    }

    public int ioPortReadByte(int address)
    {
        return 0xff;
    }

    public int ioPortReadWord(int address)
    {
        return 0xffff;
    }

    public int ioPortReadLong(int address)
    {
        return 0xffffffff;
    }

    public void ioPortWriteLong(int address, int data)
    {
    }
    
    protected int loadAddress()
    {
        return 0x100000 - length();
    }

    public boolean updated()
    {
        return super.updated() && ioportRegistered;
    }

    public void updateComponent(HardwareComponent component)
    {
        super.updateComponent(component);

        if ((component instanceof IOPortHandler) && component.updated()) {
            ((IOPortHandler) component).registerIOPortCapable(this);
            ioportRegistered = true;
        }
    }

    public boolean initialised()
    {
        return super.initialised() && ioportRegistered;
    }

    public void acceptComponent(HardwareComponent component)
    {
        super.acceptComponent(component);

        if ((component instanceof IOPortHandler) && component.initialised()) {
            ((IOPortHandler) component).registerIOPortCapable(this);
            ioportRegistered = true;
        }
    }

    public void reset()
    {
        super.reset();
        ioportRegistered = false;
    }
}
