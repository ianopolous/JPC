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

/**
 * Object which provides data backing for a disk device.  Currently this
 * includes IDE devices and floppy drives.
 * @author Chris Dennis
 */
public interface BlockDevice
{
    /**
     * Size of a sector unit in bytes.
     */
    public static final int SECTOR_SIZE = 512;
    
    /**
     * Enumeration representing the possible types of a block device.
     * <p>
     * Possible values are <code>HARDDRIVE</code>, <code>CDROM</code> and
     * <code>FLOPPY</code>.
     */
    public static enum Type {HARDDRIVE, CDROM, FLOPPY};

    /**
     * Closes the current device.  Once <code>close</code> has been called any further reads
     * from or writes to the device will most likely fail.
     */
    public void close();

    /**
     * Reads <code>size</code> sectors starting at <code>sectorNumber</code>
     * into the given array.  Returns a negative value on failure.
     * @param sectorNumber offset of the first sector to read
     * @param buffer array to write data into
     * @param size number of sectors to read.
     * @return negative on failure
     */
    public int read(long sectorNumber, byte[] buffer, int size);
    
    /**
     * Writes <code>size</code> sectors starting at <code>sectorNumber</code>
     * from the given array.  Returns a negative value on failure
     * @param sectorNumber offset of the first sector to write
     * @param buffer array to read data from
     * @param size number of sectors to write
     * @return negative on failure
     */
    public int write(long sectorNumber, byte[] buffer, int size);

    /**
     * Returns <code>true</code> if something is 'inserted' in this device.
     * This only has meaning for CD-ROM and floppy drives which return 
     * <code>true</code> if a disk in inserted.
     * @return <code>true</code> if the device media is inserted
     */
    public boolean isInserted();
    
    /**
     * Returns <code>true</code> if this device is 'locked'.  For a CD-ROM
     * device, locked means that a call to <code>eject</code> will fail to eject
     * the device.
     * @return <code>true</code> if the device media is locked
     */
    public boolean isLocked();
    
    /**
     * Returns <code>true</code> if this device is read-only.  Writes to
     * read-only devices may either fail silently, or throw exceptions.
     */
    public boolean isReadOnly();

    /**
     * Attempts to lock or unlock this device.  Success or failure can only be tested by
     * a subsequent call to <code>isLocked</code>.
     * @param locked whether to lock (<code>true</code>) or unlock (<code>false</code>)
     */
    public void setLock(boolean locked);

    /**
     * Returns the total size of this device in sectors.
     * @return total size in sectors
     */
    public long getTotalSectors();

    /**
     * Returns the number of cylinders on the device.  May or may not have any
     * physical meaning relating to the geometry of the media.
     * @return number of cylinders
     */
    public int getCylinders();
    
    /**
     * Returns the number of heads on the device.  May or may not have any
     * physical meaning relating to the geometry of the media.
     * @return number of heads
     */
    public int getHeads();
    
    /**
     * Returns the number of sectors on the device.  May or may not have any
     * physical meaning relating to the geometry of the media.
     * @return number of sectors
     */
    public int getSectors();

    /**
     * Returns this device type.  This is either: <code>TYPE_HD</code>,
     * <code>TYPE_CDROM</code> or <code>TYPE_FLOPPY</code>.
     * @return type constant
     */
    public Type getType();

    /**
     * Configure the device with given string configuration information.
     * @param spec configuration information
     * @throws java.io.IOException if configuration failed for I/O reasons
     * @throws java.lang.IllegalArgumentException if the configuration information is invalid
     */
    public void configure(String spec) throws IOException, IllegalArgumentException;
}
