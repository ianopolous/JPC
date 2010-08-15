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

package org.jpc.emulator.peripheral;

import static org.jpc.emulator.peripheral.FloppyController.DriveType;
/**
 * 
 * @author Chris Dennis
 */
enum FloppyFormat
{
    /* First entry is default format */
    /* 1.44 MB 3"1/2 floppy disks */
    DS_1440_312(DriveType.DRIVE_144, DiskType.DISK_144, 18, 80, 1, "1.44 MB 3\"1/2"),
    DS_1600_312(DriveType.DRIVE_144, DiskType.DISK_144, 20, 80, 1,  "1.6 MB 3\"1/2" ),
    DS_1680_312(DriveType.DRIVE_144, DiskType.DISK_144, 21, 80, 1, "1.68 MB 3\"1/2" ),
    DS_1722_312(DriveType.DRIVE_144, DiskType.DISK_144, 21, 82, 1, "1.72 MB 3\"1/2" ),
    DS_1743_312(DriveType.DRIVE_144, DiskType.DISK_144, 21, 83, 1, "1.74 MB 3\"1/2" ),
    DS_1760_312(DriveType.DRIVE_144, DiskType.DISK_144, 22, 80, 1, "1.76 MB 3\"1/2" ),
    DS_1840_312(DriveType.DRIVE_144, DiskType.DISK_144, 23, 80, 1, "1.84 MB 3\"1/2" ),
    DS_1920_312(DriveType.DRIVE_144, DiskType.DISK_144, 24, 80, 1, "1.92 MB 3\"1/2" ),
    /* 2.88 MB 3"1/2 floppy disks */
    DS_2880_312(DriveType.DRIVE_288, DiskType.DISK_288, 36, 80, 1, "2.88 MB 3\"1/2" ),
    DS_3120_312(DriveType.DRIVE_288, DiskType.DISK_288, 39, 80, 1, "3.12 MB 3\"1/2" ),
    DS_3200_312(DriveType.DRIVE_288, DiskType.DISK_288, 40, 80, 1,  "3.2 MB 3\"1/2" ),
    DS_3520_312(DriveType.DRIVE_288, DiskType.DISK_288, 44, 80, 1, "3.52 MB 3\"1/2" ),
    DS_3840_312(DriveType.DRIVE_288, DiskType.DISK_288, 48, 80, 1, "3.84 MB 3\"1/2" ),
    /* 720 kB 3"1/2 floppy disks */
    DS_720_312(DriveType.DRIVE_144, DiskType.DISK_720, 9, 80, 1,  "720 kB 3\"1/2" ),
    DS_800_312(DriveType.DRIVE_144, DiskType.DISK_720, 10, 80, 1,  "800 kB 3\"1/2" ),
    DS_820_312(DriveType.DRIVE_144, DiskType.DISK_720, 10, 82, 1,  "820 kB 3\"1/2" ),
    DS_830_312(DriveType.DRIVE_144, DiskType.DISK_720, 10, 83, 1,  "830 kB 3\"1/2" ),
    DS_1040_312(DriveType.DRIVE_144, DiskType.DISK_720, 13, 80, 1, "1.04 MB 3\"1/2" ),
    DS_1120_312(DriveType.DRIVE_144, DiskType.DISK_720, 14, 80, 1, "1.12 MB 3\"1/2" ),
    /* 1.2 MB 5"1/4 floppy disks */
    DS_1200_514(DriveType.DRIVE_120, DiskType.DISK_288, 15, 80, 1,  "1.2 kB 5\"1/4" ),
    DS_1440_514(DriveType.DRIVE_120, DiskType.DISK_288, 18, 80, 1, "1.44 MB 5\"1/4" ),
    DS_1476_514(DriveType.DRIVE_120, DiskType.DISK_288, 18, 82, 1, "1.48 MB 5\"1/4" ),
    DS_1494_514(DriveType.DRIVE_120, DiskType.DISK_288, 18, 83, 1, "1.49 MB 5\"1/4" ),
    DS_1600_514(DriveType.DRIVE_120, DiskType.DISK_288, 20, 80, 1,  "1.6 MB 5\"1/4" ),
    /* 720 kB 5"1/4 floppy disks */
    DS_720_514(DriveType.DRIVE_120, DiskType.DISK_288, 9, 80, 1,  "720 kB 5\"1/4" ),
    DS_880_514(DriveType.DRIVE_120, DiskType.DISK_288, 11, 80, 1,  "880 kB 5\"1/4" ),
    /* 360 kB 5"1/4 floppy disks */
    DS_360_514(DriveType.DRIVE_120, DiskType.DISK_288, 9, 40, 1,  "360 kB 5\"1/4" ),
    SS_180_514(DriveType.DRIVE_120, DiskType.DISK_288, 9, 40, 0,  "180 kB 5\"1/4" ),
    DS_410_514(DriveType.DRIVE_120, DiskType.DISK_288, 10, 41, 1,  "410 kB 5\"1/4" ),
    DS_420_514(DriveType.DRIVE_120, DiskType.DISK_288, 10, 42, 1,  "420 kB 5\"1/4" ),
    /* 320 kB 5"1/4 floppy disks */ 
    DS_320_514(DriveType.DRIVE_120, DiskType.DISK_288, 8, 40, 1,  "320 kB 5\"1/4" ),
    SS_160_514(DriveType.DRIVE_120, DiskType.DISK_288, 8, 40, 0,  "160 kB 5\"1/4" ),
    /* 360 kB must match 5"1/4 better than 3"1/2... */
    SS_360_312(DriveType.DRIVE_144, DiskType.DISK_720, 9, 80, 0,  "360 kB 3\"1/2" ),
    /* end */
    EMPTY(DriveType.DRIVE_NONE, DiskType.DISK_NONE, -1, -1, 0, "" );
    
    private static enum DiskType {DISK_288, DISK_144, DISK_720, DISK_USER, DISK_NONE};
    
    private final DriveType drive;
    private final DiskType disk;
    private final int lastSector;
    private final int maxTrack;
    private final int maxHead;
    private final String description;
    
    private FloppyFormat(DriveType drive, DiskType disk, int lastSector, int maxTrack, int maxHead, String description)
    {
	this.drive = drive;
	this.disk = disk;
	this.lastSector = lastSector;
	this.maxTrack = maxTrack;
	this.maxHead = maxHead;
	this.description = description;
    }

    public int heads()
    {
	return maxHead + 1;
    }

    public int tracks()
    {
	return maxTrack;
    }

    public int sectors()
    {
	return lastSector;
    }

    public DriveType drive()
    {
	return drive;
    }

    public long length()
    {
	return heads() * tracks() * sectors() * 512L;
    }

    public String toString()
    {
	return description;
    }

    public static FloppyFormat findFormat(long size, DriveType drive)
    {
	FloppyFormat firstMatch = null;
        for (FloppyFormat f : values()) {
	    if (f.drive() == DriveType.DRIVE_NONE)
		break;
	    if ((drive == f.drive()) || (drive == DriveType.DRIVE_NONE)) {
		if (f.length() == size) {
		    return f;
		}
		if (firstMatch == null)
		    firstMatch = f;
		
	    }
	}
	if (firstMatch == null)
	    return values()[0]; // Should this return the NULL format?
	else
	    return firstMatch;
    }
}
