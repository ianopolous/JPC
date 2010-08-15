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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IO device used for caching writes.
 * @author Ian Preston
 */
public class CachingSeekableIODevice implements SeekableIODevice
{

    private static final Logger LOGGING = Logger.getLogger(RawBlockDevice.class.getName());
    private SeekableIODevice parent;
    private long byteOffset;
    private HashMap<Long, byte[]> sectors = new HashMap<Long, byte[]>();

    public CachingSeekableIODevice(SeekableIODevice parent)
    {
        this.parent = parent;
    }

    public void seek(long offset) throws IOException
    {
        byteOffset = offset;
    }

    public int write(byte[] data, int offset, int length) throws IOException
    {
        if (byteOffset % BlockDevice.SECTOR_SIZE != 0)
        {
            LOGGING.log(Level.WARNING, "Trying to write off a sector boundary.");
            return 0;
        }
        long sector = byteOffset / BlockDevice.SECTOR_SIZE;
        int numsectors = length / BlockDevice.SECTOR_SIZE;
        for (int i = 0; i < numsectors; i++)
        {
            byte[] newSector = new byte[BlockDevice.SECTOR_SIZE];
            System.arraycopy(data, offset + i * BlockDevice.SECTOR_SIZE, newSector, 0, BlockDevice.SECTOR_SIZE);
            sectors.put(sector + i, newSector);
        }
        return length;
    }

    private void readFully(byte[] data, int dataOffset, int readOffset, int length) throws IOException
    {
        int pos = 0;
        if (readOffset > 0)
            parent.seek(byteOffset + (long) readOffset);
        while (true)
        {
            if (pos >= length)
                break;
            int read = parent.read(data, dataOffset + pos, length - pos);
            if (read < 0)
                break;
            pos += read;
            if (read == 0)
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException ex)
                {
                    Logger.getLogger(CachingSeekableIODevice.class.getName()).log(Level.SEVERE, null, ex);
                }
            parent.seek(byteOffset + (long) (pos + readOffset));
        }
    }

    public int read(byte[] data, int offset, int length) throws IOException
    {
        if (byteOffset % BlockDevice.SECTOR_SIZE != 0)
        {
            System.out.println("Trying to read off a sector boundary.");
            return 0;
        }
        long startSector = byteOffset / BlockDevice.SECTOR_SIZE;
        int numSectors = length / BlockDevice.SECTOR_SIZE;
        //check if any of the sectors have been written to
        int firstWritten = -1;
        for (int i = 0; i < numSectors; i++)
            if (sectors.containsKey(startSector + i))
                if (firstWritten < 0)
                    firstWritten = i;

        if (firstWritten < 0)
        {
            parent.seek(byteOffset);
            readFully(data, offset, 0, length);
            return length;
        }
        else
        {
            // read up until the first sector written to, then work sector by sector
            parent.seek(byteOffset);
            readFully(data, offset, 0, firstWritten * BlockDevice.SECTOR_SIZE);


            for (int i = firstWritten; i < numSectors; i++)
                if (sectors.containsKey(startSector + i))
                {
                    byte[] out = sectors.get(startSector + i);
                    System.arraycopy(out, 0, data, offset + i * BlockDevice.SECTOR_SIZE, BlockDevice.SECTOR_SIZE);
                }
                else
                    readFully(data, offset + i * BlockDevice.SECTOR_SIZE, i * BlockDevice.SECTOR_SIZE, BlockDevice.SECTOR_SIZE);
            int lastReadSize = length % BlockDevice.SECTOR_SIZE;
            if (lastReadSize > 0)
                //hopefully never get here because we read entire sectors at a time
                if (sectors.containsKey(startSector + numSectors))
                {
                    byte[] out = sectors.get(startSector + numSectors);
                    System.arraycopy(out, 0, data, offset + length - lastReadSize, lastReadSize);
                }
                else
                {
                    parent.seek(byteOffset + length - lastReadSize);
                    int pos = 0;
                    int toRead = lastReadSize;
                    while (true)
                    {
                        int read = parent.read(data, offset + length - lastReadSize + pos, toRead - pos);
                        if ((read < 0) || (pos >= toRead))
                            break;
                        pos += read;
                    }
                }
            return length;
        }
    }

    public long length()
    {
        return parent.length();
    }

    public boolean readOnly()
    {
        return parent.readOnly();
    }

    public void close() throws IOException
    {
        parent.close();
    }

    public void configure(String opts) throws IOException, IllegalArgumentException
    {
        parent.configure(opts);
    }

    public String toString()
    {
        if (parent == null)
            return "caching:null";
        return "caching: " + parent.toString();
    }
}
