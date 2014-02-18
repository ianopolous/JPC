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

package org.jpc.emulator.pci.peripheral;

import org.jpc.emulator.pci.*;
import org.jpc.emulator.motherboard.*;
import org.jpc.support.*;
import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.memory.PhysicalAddressSpace;

import java.io.*;
import java.util.logging.*;

/**
 * 
 * @author Chris Dennis
 */
public class PIIX3IDEInterface extends AbstractPCIDevice
{
    private static final Logger LOGGING = Logger.getLogger(PIIX3IDEInterface.class.getName());
    
    private InterruptController irqDevice;
    private IDEChannel[] channels;
    private boolean drivesUpdated;

    private BMDMAIORegion[] bmdmaRegions;

    private BlockDevice[] drives;

    public PIIX3IDEInterface()
    {
	devfnSet = false;
	ioportRegistered = false;
	pciRegistered = false;
	dmaRegistered = false;

	this.assignDeviceFunctionNumber(-1);

        putConfigWord(PCI_CONFIG_VENDOR_ID, (short)0x8086); // Intel
        putConfigWord(PCI_CONFIG_DEVICE_ID, (short)0x7010);
	putConfigByte(0x09, (byte)0x80); // legacy ATA mode
        putConfigWord(PCI_CONFIG_CLASS_DEVICE, (short)0x0101); // PCI IDE
	putConfigByte(PCI_CONFIG_HEADER, (byte)0x00); // header_type


	channels = new IDEChannel[2];

	bmdmaRegions = new BMDMAIORegion[2];
	//Run BMDMARegion constructors Remember 0=1 and 2=3
	bmdmaRegions[1] = new BMDMAIORegion(null);
	bmdmaRegions[0] = new BMDMAIORegion(bmdmaRegions[1]);
    }

    public void saveState(DataOutput output) throws IOException
    {
        channels[0].saveState(output);
        channels[1].saveState(output);
        for (BMDMAIORegion bmdma : bmdmaRegions) {
            if (bmdma != null) 
                bmdma.saveState(output);
        }
    }

    public void loadState(DataInput input) throws IOException
    {
        channels[0].loadState(input);
        channels[1].loadState(input);
        bmdmaRegions[0].loadState(input);
        bmdmaRegions[1].loadState(input);
    }

    public void loadIOPorts(IOPortHandler ioportHandler, DataInput input) throws IOException
    {
        drivesUpdated = false;
        devfnSet = true;
        pciRegistered = false;
        ioportRegistered = false;
        dmaRegistered = false;
        loadState(input);
        ioportHandler.registerIOPortCapable(channels[0]);
        ioportHandler.registerIOPortCapable(channels[1]);
        if (!(bmdmaRegions[0].ioPortsRequested()[0] == -1))
            ioportHandler.registerIOPortCapable(bmdmaRegions[0]);
        if (!(bmdmaRegions[1].ioPortsRequested()[0] == -1))
            ioportHandler.registerIOPortCapable(bmdmaRegions[1]);
    }

    public boolean autoAssignDeviceFunctionNumber()
    {
	return false;
    }

    public void deassignDeviceFunctionNumber()
    {
        LOGGING.log(Level.WARNING, "PCI device/function number conflict.");
    }



    public IORegion[] getIORegions()
    {
	return new IORegion[]{bmdmaRegions[0]};
    }
    public IORegion getIORegion(int index)
    {
	if (index == 4) {
	    return bmdmaRegions[0];
	} else {
	    return null;
	}
    }


    private boolean ioportRegistered;
    private boolean pciRegistered;
    private boolean dmaRegistered;

    public boolean initialised()
    {
	return ioportRegistered && pciRegistered && dmaRegistered && (irqDevice != null) && (drives != null);
    }

    public void reset()
    {
	devfnSet = false;
	ioportRegistered = false;
	pciRegistered = false;

	this.assignDeviceFunctionNumber(-1);

        putConfigWord(PCI_CONFIG_VENDOR_ID, (short)0x8086); // Intel
        putConfigWord(PCI_CONFIG_DEVICE_ID, (short)0x7010);
	putConfigByte(0x09, (byte)0x80); // legacy ATA mode
        putConfigWord(PCI_CONFIG_CLASS_DEVICE, (short)0x0101); // PCI IDE
	putConfigByte(PCI_CONFIG_HEADER, (byte)0x00); // header_type

	channels = new IDEChannel[2];

	dmaRegistered = false;
	bmdmaRegions = new BMDMAIORegion[2];
	//Run BMDMARegion constructors Remember 0=1 and 2=3
	bmdmaRegions[1] = new BMDMAIORegion(null);
	bmdmaRegions[0] = new BMDMAIORegion(bmdmaRegions[1]);

	irqDevice = null;
	drives = null;

	super.reset();
    }

    private boolean devfnSet;

    public boolean updated()
    {
	return ioportRegistered && pciRegistered && dmaRegistered && irqDevice.updated() && drivesUpdated;
    }

    public void updateComponent(HardwareComponent component) {
        if ((component instanceof IOPortHandler) && irqDevice.updated() && drivesUpdated) {
            //Run IDEChannel Constructors
            //            channels[0] = new IDEChannel(14, irqDevice, 0x1f0, 0x3f6, new BlockDevice[]{drives[0], drives[1]}, bmdmaRegions[0]);
            //            channels[1] = new IDEChannel(15, irqDevice, 0x170, 0x376, new BlockDevice[]{drives[2], drives[3]}, bmdmaRegions[1]);
            channels[0].setDrives(new BlockDevice[]{drives[0], drives[1]});
            channels[1].setDrives(new BlockDevice[]{drives[2], drives[3]});
            ((IOPortHandler) component).registerIOPortCapable(channels[0]);
            ((IOPortHandler) component).registerIOPortCapable(channels[1]);
            ioportRegistered = true;
        }

        if ((component instanceof PCIBus) && component.updated() && !pciRegistered && devfnSet) {
            pciRegistered = ((PCIBus) component).registerDevice(this);
        }

        if ((component instanceof PCIISABridge) && component.updated()) {
            this.assignDeviceFunctionNumber(((PCIDevice) component).getDeviceFunctionNumber() + 1);
            devfnSet = true;
        }

        if ((component instanceof DriveSet) && component.updated()) {
            //	    drives = new BlockDevice[4];
            drives[0] = ((DriveSet) component).getHardDrive(0);
            drives[1] = ((DriveSet) component).getHardDrive(1);
            drives[2] = ((DriveSet) component).getHardDrive(2);
            drives[3] = ((DriveSet) component).getHardDrive(3);
            drivesUpdated = true;
        }

        if (component instanceof PhysicalAddressSpace) {
            dmaRegistered = true;
            bmdmaRegions[0].setAddressSpace((PhysicalAddressSpace) component);
            bmdmaRegions[1].setAddressSpace((PhysicalAddressSpace) component);
        }
    }

    public void acceptComponent(HardwareComponent component)
    {
	if ((component instanceof InterruptController) && component.initialised())
	    irqDevice = (InterruptController)component;

        if ((component instanceof IOPortHandler) && component.initialised()
            && (irqDevice != null) && (drives != null)) {
            //Run IDEChannel Constructors
            channels[0] = new IDEChannel(14, irqDevice, 0x1f0, 0x3f6, new BlockDevice[]{drives[0], drives[1]}, bmdmaRegions[0]);
            channels[1] = new IDEChannel(15, irqDevice, 0x170, 0x376, new BlockDevice[]{drives[2], drives[3]}, bmdmaRegions[1]);
            ((IOPortHandler)component).registerIOPortCapable(channels[0]);
            ((IOPortHandler)component).registerIOPortCapable(channels[1]);
            ioportRegistered = true;
        }
	
	if ((component instanceof PCIBus) && component.initialised() && !pciRegistered && devfnSet) {
	    pciRegistered = ((PCIBus)component).registerDevice(this);
	}

	if ((component instanceof PCIISABridge) && component.initialised()) {
	    this.assignDeviceFunctionNumber(((PCIDevice)component).getDeviceFunctionNumber() + 1);
	    devfnSet = true;
	}

	if ((component instanceof DriveSet)
	    && component.initialised()) {
	    drives = new BlockDevice[4];
	    drives[0] = ((DriveSet)component).getHardDrive(0);
	    drives[1] = ((DriveSet)component).getHardDrive(1);
	    drives[2] = ((DriveSet)component).getHardDrive(2);
	    drives[3] = ((DriveSet)component).getHardDrive(3);
	}

	if (component instanceof PhysicalAddressSpace) {
	    dmaRegistered = true;
	    bmdmaRegions[0].setAddressSpace((PhysicalAddressSpace)component);
	    bmdmaRegions[1].setAddressSpace((PhysicalAddressSpace)component);
	}
    }

    public String toString()
    {
	return "Intel PIIX3 IDE Interface";
    }
}
