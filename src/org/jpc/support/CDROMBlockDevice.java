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

/**
 * A <code>RawBlockDevice</code> instance representing a cdrom device.
 * Instances of this class will report cdrom-like geometries and behaviours.  In
 * particular it supports locking and ejecting of drives, and will always be
 * read-only.
 * @author Chris Dennis
 */
public class CDROMBlockDevice extends RawBlockDevice
{
    private boolean locked;

    /**
     * Create a device backed by the given <code>SeekableIODevice</code> object.
     * @param data backing device
     */
    public CDROMBlockDevice(SeekableIODevice data)
    {
        super(data);
    }

    /**
     * Create a device with no backing storage.  This is the equivalent of a
     * cdrom drive with no disc inserted.
     */
    public CDROMBlockDevice()
    {
        this(null);
    }

    public void close()
    {
        super.close();
	eject();
    }

    public boolean isLocked()
    {
	return locked;
    }

    public boolean isReadOnly()
    {
	return true;
    }

    /**
     * Locks or unlocks the drive to prevents or allow the ejection or insertion
     * of discs.
     * @param locked <code>true</code> to lock the device
     */
    public void setLock(boolean locked)
    {
	this.locked = locked;
    }

    /**
     * Inserts the given media into this device.  If the drive contains a disc
     * then this is first ejected.  If the drive is locked then insertion will
     * fail.
     * @param media disc to insert
     * @return <code>true</code> if insertion was successful
     */
    public boolean insert(SeekableIODevice media)
    {
        if (!eject())
            return false;

        setData(media);

	return true;
    }

    /**
     * Ejects the current disc (if any).  If the drive is locked then ejection
     * will fail regardless of whether there is a disc in the drive.
     * @return <code>true</code> if ejection was successful
     */
    public boolean eject()
    {
	if (isLocked())
	    return false;

        setData(null);
	return true;
    }

    /**
     * Hard coded to return the type constant for a cdrom device.
     * @return <code>TYPE_CDROM</code>
     */
    public Type getType()
    {
	return Type.CDROM;
    }
    
    public int getCylinders()
    {
        return 2;
    }

    public int getHeads()
    {
        return 16;
    }

    public int getSectors()
    {
        return 63;
    }
    
    public String toString()
    {
        return "CDROM: " + super.toString();
    }
}
