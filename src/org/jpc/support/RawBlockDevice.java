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

package org.jpc.support;

import java.io.*;
import java.util.logging.*;

/**
 * A generic block device backed by a <code>SeekableIODevice</code> instance.
 * @author Chris Dennis
 */
public abstract class RawBlockDevice implements BlockDevice
{
    private static final Logger LOGGING = Logger.getLogger(RawBlockDevice.class.getName());
    
    private SeekableIODevice data;
    private long totalSectors;

    /**
     * Constructs an instance backed by the given <code>SeekableIODevice</code>.
     * @param data device backing
     */
    protected RawBlockDevice(SeekableIODevice data)
    {
        setData(data);
    }

    public int read(long sectorNumber, byte[] buffer, int size)
    {
        Integer t;
        try {
            
            data.seek(sectorNumber * SECTOR_SIZE);
            int pos = 0;
            int toRead = Math.min(buffer.length, SECTOR_SIZE * size);
            while (true) {
                if (pos >= toRead)
                    return pos;
                int read = data.read(buffer, pos, toRead - pos);
                if (read < 0)
                    return pos;

                pos += read;
            }
        } catch (IOException e) {
            LOGGING.log(Level.WARNING, "error reading sector " + sectorNumber + ", size = " +size, e);
            return -1;
        }
    }

    public int write(long sectorNumber, byte[] buffer, int size)
    {
        try {
            data.seek(sectorNumber * SECTOR_SIZE);
            data.write(buffer, 0, size * SECTOR_SIZE);
        } catch (IOException e) {
            LOGGING.log(Level.WARNING, "error waiting", e);
            return -1;
        }
        return 0;
    }

    public long getTotalSectors()
    {
        return totalSectors;
    }

    public boolean isInserted()
    {
        return (data != null);
    }

    public boolean isReadOnly()
    {
        return data.readOnly();
    }
    
    public void close()
    {
        try {
            data.close();
        } catch (IOException e) {
            LOGGING.log(Level.INFO, "Couldn't close device", e);
        }
    }
    
    public void configure(String specs) throws IOException
    {
        data.configure(specs);
    }

    /**
     * Changes the backing for this device.
     * @param data new backing device
     */
    protected final void setData(SeekableIODevice data)
    {
        this.data = data;
        if (data == null)
            totalSectors = 0;
        else
            totalSectors = data.length() / SECTOR_SIZE;
    }
    
    public String toString()
    {
        if (data == null)
            return "<empty>";
        else
            return data.toString();
    }
}
