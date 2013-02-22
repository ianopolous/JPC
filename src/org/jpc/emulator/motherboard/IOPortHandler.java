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

import org.jpc.emulator.AbstractHardwareComponent;

/**
 * This class holds the map between ioport addresses and <code>IODevice</code>
 * objects.  Unmapped ports are redirected to an unconnected inner class instance
 * whose data lines float high, and on which writes fail silently.
 * @author Chris Dennis
 */
public class IOPortHandler extends AbstractHardwareComponent implements IODevice
{
    private static final int MAX_IOPORTS = 65536;
    private static final IODevice defaultDevice = new UnconnectedIOPort();
    private IODevice[] ioPortDevice;

    /**
     * Constructs a new <code>IOPortHandler</code> with an initially empty ioport
     * mapping.  All ioports map to the unconnected instance.
     */
    public IOPortHandler()
    {
        ioPortDevice = new IODevice[MAX_IOPORTS];
        for (int i = 0; i < ioPortDevice.length; i++)
            ioPortDevice[i] = defaultDevice;
    }

    public int ioPortRead8(int address)
    {
        return ioPortDevice[address].ioPortRead8(address);
    }

    public int ioPortRead16(int address)
    {
        return ioPortDevice[address].ioPortRead16(address);
    }

    public int ioPortRead32(int address)
    {
        return ioPortDevice[address].ioPortRead32(address);
    }

    public void ioPortWrite8(int address, int data)
    {
        ioPortDevice[address].ioPortWrite8(address, data);
    }

    public void ioPortWrite16(int address, int data)
    {
        ioPortDevice[address].ioPortWrite16(address, data);
    }

    public void ioPortWrite32(int address, int data)
    {
        ioPortDevice[address].ioPortWrite32(address, data);
    }

    public int[] ioPortsRequested()
    {
        return null;
    }

    /**
     * Map an <code>IODevice</code> device into this handler.
     * <p>
     * The range of ioports requested by this device are registered with the
     * handler.  Each individual port is registered only if that port is
     * currently unconnected.
     * @param device object to be mapped.
     */
    public void registerIOPortCapable(IODevice device)
    {
        int[] portArray = device.ioPortsRequested();
        if (portArray == null) return;
        for (int port : portArray) {
            if (ioPortDevice[port] == defaultDevice)
                ioPortDevice[port] = device;
        }
    }

    /**
     * Unmap an <code>IODevice</code> device from this handler.
     * <p>
     * Ports are only unmapped from the handler if they are currently to the
     * supplied object.  References to other objects at the addresses claimed
     * are not cleared.
     * @param device object to be unmapped.
     */
    public void deregisterIOPortCapable(IODevice device)
    {
        int[] portArray = device.ioPortsRequested();
        for (int port : portArray) {
            if (ioPortDevice[port] == device)
                ioPortDevice[port] = defaultDevice;
        }
    }

    public void reset()
    {
        ioPortDevice = new IODevice[MAX_IOPORTS];
        for (int i = 0; i < ioPortDevice.length; i++)
            ioPortDevice[i] = defaultDevice;
    }

    public String toString()
    {
        return "IOPort Bus";
    }

    private static class UnconnectedIOPort implements IODevice
    {

        public int ioPortRead8(int address)
        {
            return 0xff;
        }

        public int ioPortRead16(int address)
        {
            return 0xffff;
        }

        public int ioPortRead32(int address)
        {
            return 0xffffffff;
        }

        public void ioPortWrite8(int address, int data)
        {
        }

        public void ioPortWrite16(int address, int data)
        {
        }

        public void ioPortWrite32(int address, int data)
        {
        }

        public int[] ioPortsRequested()
        {
            return null;
        }
    }
}
