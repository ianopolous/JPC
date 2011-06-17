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

//Do not even think about adding an import line to this class - especially not import java.net.*!
import java.io.IOException;
import java.io.DataOutput;
import java.io.DataInput;
import java.util.logging.*;

import org.jpc.emulator.AbstractHardwareComponent;

/**
 * Represents the set of disk drive devices associated with this emulator
 * instance.
 * @author Chris Dennis
 */
public class DriveSet extends AbstractHardwareComponent
{
    private static final Logger LOGGING = Logger.getLogger(DriveSet.class.getName());
    
    public static enum BootType {FLOPPY, HARD_DRIVE, CDROM;}

    private static enum Devices {
        DEFAULT("org.jpc.support.FileBackedSeekableIODevice"),
        
        dir("org.jpc.support.TreeBlockDevice"),
        mem("org.jpc.support.ArrayBackedSeekableIODevice"),
        net("org.jpc.support.RemoteSeekableIODevice");

        private final String clazzname;
        
        Devices(String clazz)
        {
            clazzname = clazz;
        }
        
        public Object getInstance()
        {
            try {
                Class clazz = Class.forName(clazzname);
                return clazz.newInstance();
            } catch (ClassNotFoundException e) {
                LOGGING.log(Level.WARNING, "Drive device class not found", e);
                return null;
            } catch (InstantiationException e) {
                LOGGING.log(Level.WARNING, "Drive device couldn't be instantiated", e);
                return null;
            } catch (IllegalAccessException e) {
                LOGGING.log(Level.WARNING, "Drive device couldn't be instantiated", e);
                return null;
            }
        }        
    }
    
    private BootType bootType;
    private BlockDevice[] floppies;
    private BlockDevice[] ides;
    private String[] initialArgs;

    /**
     * Constructs a driveset with one hard disk, one floppy disk and the
     * specified boot device type.
     * @param boot boot device
     * @param floppyDrive first floppy device
     * @param hardDrive primary master hard disk device
     */
    public DriveSet(BootType boot, BlockDevice floppyDrive, BlockDevice hardDrive)
    {
        this(boot, floppyDrive, null, hardDrive, null, null, null);
    }

    /**
     * Constructs a driveset with all parameters specified.
     * <p>
     * A drive set can be composed of at most four ide devices and two floppy
     * drive devices.
     * @param boot boot device
     * @param floppyDriveA first floppy device
     * @param floppyDriveB second floppy device
     * @param hardDriveA primary master hard disk
     * @param hardDriveB primary slave hard disk
     * @param hardDriveC secondary master hard disk
     * @param hardDriveD secondary slave hard disk
     */
    public DriveSet(BootType boot, BlockDevice floppyDriveA, BlockDevice floppyDriveB, BlockDevice hardDriveA, BlockDevice hardDriveB, BlockDevice hardDriveC, BlockDevice hardDriveD)
    {
        this.bootType = boot;

        floppies = new BlockDevice[2];
        floppies[0] = floppyDriveA;
        floppies[1] = floppyDriveB;

        ides = new BlockDevice[4];
        ides[0] = hardDriveA;
        ides[1] = hardDriveB;
        ides[2] = (hardDriveC == null) ? new CDROMBlockDevice() : hardDriveC;
        ides[3] = hardDriveD;
    }

    private void setInitialArgs(String[] init)
    {
        initialArgs = init;
    }

    /**
     * Returns the i'th hard drive device.
     * <p>
     * Devices are numbered from 0 to 3 inclusive in order: primary master,
     * primary slave, secondary master, secondary slave.
     * @param index drive index
     * @return hard drive block device
     */
    public BlockDevice getHardDrive(int index)
    {
        return ides[index];
    }

    public void setHardDrive(int index, BlockDevice device)
    {
        ides[index] = device;
    }

    /**
     * Returns the i'th floppy drive device.
     * <p>
     * The drives are numbered sequentially A:, B:.
     * @param index floppy drive index
     * @return floppy drive block device
     */
    public BlockDevice getFloppyDrive(int index)
    {
        return floppies[index];
    }

    /**
     * Returns the current boot device as determined by the boot type parameter.
     * @return boot block device
     */
    public BlockDevice getBootDevice()
    {
        switch (bootType) {
            case FLOPPY:
                return floppies[0];
            case CDROM:
                return ides[2];
            case HARD_DRIVE:
                return ides[0];
            default:
                return null;
        }
    }

    /**
     * Returns the boot type being used by this driveset.
     * @return boot type
     */
    public BootType getBootType()
    {
        return bootType;
    }

    private static Object createDevice(String spec)
    {
        if (spec == null) {
            return null;
        }

	if ((spec.indexOf("\"") == 0) && (spec.indexOf("\"", 1) > 0))
	    spec = spec.substring(1, spec.length()-2);

	

        int colon = spec.indexOf(':');
        String deviceKey = "DEFAULT";
	String deviceSpec = spec;
        if ((colon >= 0) && (spec.indexOf("\\") != colon + 1)) {
            deviceKey = spec.substring(0, colon);
	    deviceSpec = spec.substring(colon + 1);
        }

        Object device;
        if (deviceKey.startsWith("caching")) {
            deviceKey = "DEFAULT";
            int secondcolon = deviceSpec.indexOf(':');
            if ((secondcolon > 0) && (deviceSpec.indexOf("\\") != secondcolon + 1)) {
                deviceSpec = deviceSpec.substring(secondcolon + 1);
                deviceKey = deviceSpec.substring(0, secondcolon);
            }
            device = Devices.valueOf(deviceKey).getInstance();
            device = new CachingSeekableIODevice((SeekableIODevice) device);
        } else
            device = Devices.valueOf(deviceKey).getInstance();

        if (device instanceof SeekableIODevice) {
            try {
                ((SeekableIODevice) device).configure(deviceSpec);
            } catch (IOException e) {
                return null;
            }
            return device;
        } else if (device instanceof BlockDevice) {
            try {
                ((BlockDevice) device).configure(deviceSpec);
            } catch (IOException e) {
                return null;
            }
            return device;
        } else {
            return device;
        }
    }

    private static BlockDevice createFloppyBlockDevice(String spec)
    {
        Object device = createDevice(spec);

        if (device instanceof SeekableIODevice)
            return new FloppyBlockDevice((SeekableIODevice) device);
        else
            return (BlockDevice) device;
    }

    private static BlockDevice createHardDiskBlockDevice(String spec)
    {
        Object device = createDevice(spec);

        if (device instanceof SeekableIODevice)
            return new HDBlockDevice((SeekableIODevice) device);
        else
            return (BlockDevice) device;
    }

    private static BlockDevice createCdRomBlockDevice(String spec)
    {
        Object device = createDevice(spec);

        if (device instanceof SeekableIODevice)
            return new CDROMBlockDevice((SeekableIODevice) device);
        else
            return (BlockDevice) device;
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeInt(initialArgs.length);
        for (int i = 0; i < initialArgs.length; i++)
            output.writeUTF(initialArgs[i]);
    }

    public void loadState(DataInput input) throws IOException
    {
        int len = input.readInt();
        String[] newArgs = new String[len];
        for (int i = 0; i < len; i++)
            newArgs[i] = input.readUTF();
        initialArgs = newArgs;

        //do not load drives
//        DriveSet temp = buildFromArgs(initialArgs);
//
//        floppies = new BlockDevice[2];
//        for (int i = 0; i < floppies.length; i++)
//            floppies[i] = temp.getFloppyDrive(i);
//
//        for (int i = 0; i < ides.length; i++)
//            ides[i] = temp.getHardDrive(i);
    }

    /**
     * Constructs a driveset instance by parsing the given command line
     * arguments.
     * @param args command line argument array
     * @return resultant <code>DriveSet</code>
     */
    public static DriveSet buildFromArgs(String[] args)
    {
        String[] initialArgs = args.clone();

        BlockDevice floppyA = createFloppyBlockDevice(ArgProcessor.findVariable(args, "-fda", null));
        BlockDevice floppyB = createFloppyBlockDevice(ArgProcessor.findVariable(args, "-fdb", null));

        BlockDevice hardDiskA = createHardDiskBlockDevice(ArgProcessor.findVariable(args, "-hda", null));
        BlockDevice hardDiskB = createHardDiskBlockDevice(ArgProcessor.findVariable(args, "-hdb", null));
        BlockDevice hardDiskC = createHardDiskBlockDevice(ArgProcessor.findVariable(args, "-hdc", null));
        BlockDevice hardDiskD = createHardDiskBlockDevice(ArgProcessor.findVariable(args, "-hdd", null));

        String cdromSpec = ArgProcessor.findVariable(args, "-cdrom", null);
        if (cdromSpec != null)
            hardDiskC = createCdRomBlockDevice(cdromSpec);

        BootType boot = BootType.FLOPPY;
        String bootArg = ArgProcessor.findVariable(args, "-boot", null);
        if ("fda".equalsIgnoreCase(bootArg))
            boot = BootType.FLOPPY;
        else if ("hda".equalsIgnoreCase(bootArg))
            boot = BootType.HARD_DRIVE;
        else if ("cdrom".equalsIgnoreCase(bootArg))
            boot = BootType.CDROM;
        else if (hardDiskA != null)
            boot = BootType.HARD_DRIVE;
        else if (hardDiskC instanceof CDROMBlockDevice)
            boot = BootType.CDROM;

        DriveSet temp = new DriveSet(boot, floppyA, floppyB, hardDiskA, hardDiskB, hardDiskC, hardDiskD);
        temp.setInitialArgs(initialArgs);
        return temp;
    }
}
