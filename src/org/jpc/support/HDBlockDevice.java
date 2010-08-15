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

import java.util.logging.*;

/**
 * Hard-drive block device implementation.
 * <p>
 * Subclass of <code>RawBlockDevice</code> with the ability to auto-detect drive
 * geometries from DOS paritition or fallback to guessing from the drive size.
 * @author Chris Dennis
 */
public class HDBlockDevice extends RawBlockDevice
{
    private static final Logger LOGGING = Logger.getLogger(HDBlockDevice.class.getName());

    private static final int PART_END_CHS = 0x5;
    private static final int PART_SIZE = 0xc;
    private static final int PART_1 = 0x1be;
    private static final int PART_MAGIC = 0x1fe;       
    
    private final int cylinders;
    private final int heads;
    private final int sectors;

    /**
     * Constructs an instance backed by the given <code>SeekableIODevice</code>.
     * @param data backing
     */
    public HDBlockDevice(SeekableIODevice data)
    {
        super(data);
        
        int detectedCylinders = 0;
        int detectedHeads = 0;
        int detectedSectors = 0;
       
        byte[] mbr = new byte[512];
        if ((read(0, mbr, 1) >= 0) && (mbr[PART_MAGIC] == (byte) 0x55) && (mbr[PART_MAGIC + 1] == (byte) 0xaa)) {
            for (int i = PART_1; i < PART_MAGIC; i += 0x10) {
                int numberSectors = (mbr[i + PART_SIZE] & 0xff) |
                        ((mbr[i + PART_SIZE + 1] & 0xff) << 8) |
                        ((mbr[i + PART_SIZE + 2] & 0xff) << 16) |
                        ((mbr[i + PART_SIZE + 3] & 0xff) << 24);
                if (numberSectors != 0) {
                    detectedHeads = 1 + (mbr[i + PART_END_CHS] & 0xff);
                    detectedSectors = mbr[i + PART_END_CHS + 1] & 0x3f;
                    if (detectedSectors == 0) continue;
                    detectedCylinders = (int) (this.getTotalSectors() / (detectedHeads * detectedSectors));
                    if (detectedCylinders < 1 || detectedCylinders > 16383) {
                        detectedCylinders = 0;
                        continue;
                    }
                }
            }
        }
        
        if (detectedCylinders == 0) { //no geometry information?
            //We'll use a standard LBA geometry
            detectedCylinders = (int) (this.getTotalSectors() / (16 * 63));
            if (detectedCylinders > 16383)
                detectedCylinders = 16383;
            else if (detectedCylinders < 2)
                detectedCylinders = 2;

            detectedHeads = 16;
            detectedSectors = 63;
            LOGGING.log(Level.INFO, "no geometry information, guessing CHS {0,number,integer}:{1,number,integer}:{2,number,integer}",
                    new Object[]{Integer.valueOf(detectedCylinders), Integer.valueOf(detectedHeads), Integer.valueOf(detectedSectors)
            });
        }
        
        cylinders = detectedCylinders;
        heads = detectedHeads;
        sectors = detectedSectors;        
    }

    public boolean isLocked()
    {
        return false;
    }

    public void setLock(boolean locked)
    {
    }

    public int getCylinders()
    {
        return cylinders;
    }

    public int getHeads()
    {
        return heads;
    }

    public int getSectors()
    {
        return sectors;
    }

    public Type getType()
    {
        return Type.HARDDRIVE;
    }
    
    public String toString()
    {
        return "HD: " + super.toString();
    }
}
