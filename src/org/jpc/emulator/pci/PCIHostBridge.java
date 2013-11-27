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

package org.jpc.emulator.pci;

import org.jpc.emulator.memory.PhysicalAddressSpace;
import org.jpc.emulator.motherboard.*;
import org.jpc.emulator.HardwareComponent;

import java.io.*;
import java.util.logging.*;

/**
 * Emulation of an Intel i440FX PCI Host Bridge.
 * <p>
 * The host bridge is the PCI device that provides the processor with access to
 * the PCI bus and the rest if its devices.
 * @author Chris Dennis
 */
public class PCIHostBridge extends AbstractPCIDevice implements IODevice
{
    private static final Logger LOGGING = Logger.getLogger(PCIHostBridge.class.getName());
    
    private PCIBus attachedBus;
    private PhysicalAddressSpace memory;

    private int configRegister;    

    /**
     * Constructs the (singleton) host bridge for a pci bus.
     */
    public PCIHostBridge()
    {
	ioportRegistered = false;

	assignDeviceFunctionNumber(0);

        putConfigWord(PCI_CONFIG_VENDOR_ID, (short)0x8086); // vendor_id
        putConfigWord(PCI_CONFIG_DEVICE_ID, (short)0x1237); // device_id
        putConfigByte(PCI_CONFIG_REVISION, (byte)0x02); // revision
        putConfigWord(PCI_CONFIG_CLASS_DEVICE, (short)0x0600); // pci host bridge
	putConfigByte(PCI_CONFIG_HEADER, (byte)0x00); // header_type
    }

    public void saveState(DataOutput output) throws IOException
    {
        super.saveState(output);
        output.writeInt(configRegister);
    }
    
    public void loadState(DataInput input) throws IOException
    {
        super.loadState(input);
        ioportRegistered = false;
        pciRegistered = false;
        configRegister = input.readInt();
    }

    public boolean autoAssignDeviceFunctionNumber()
    {
	return false;
    }

    public void deassignDeviceFunctionNumber()
    {
        LOGGING.log(Level.WARNING, "PCI device/function number conflict.");
    }

    /* BEGIN PCIDevice Methods */
    //IOPort Registration Aids
    public IORegion[] getIORegions()
    {
	return null;
    }
    public IORegion getIORegion(int index)
    {
	return null;
    }

    public int[] ioPortsRequested()
    {
	return new int[]{0xcf8, 0xcf9, 0xcfa, 0xcfb, 0xcfc, 0xcfd, 0xcfe, 0xcff};
    }

    public void ioPortWrite8(int address, int data)
    {
	switch (address) {
	case 0xcfc:
	case 0xcfd:
	case 0xcfe:
	case 0xcff:
	    if ((configRegister & (1 << 31)) != 0)
		attachedBus.writePCIDataByte(configRegister | (address & 0x3), (byte)data);
	    break;
	default:
	}
    }

    public void ioPortWrite16(int address, int data)
    {
	switch(address) {
	case 0xcfc:
	case 0xcfd:
	case 0xcfe:
	case 0xcff:
	    if ((configRegister & (1 << 31)) != 0)
		attachedBus.writePCIDataWord(configRegister | (address & 0x3), (short)data);
	    break;
	default:
	}
    }

    public void ioPortWrite32(int address, int data)
    {
	switch(address) {
	case 0xcf8:
	case 0xcf9:
	case 0xcfa:
	case 0xcfb:
	    configRegister = data;
	    break;
	case 0xcfc:
	case 0xcfd:
	case 0xcfe:
	case 0xcff:
	    if ((configRegister & (1 << 31)) != 0)
		attachedBus.writePCIDataLong(configRegister | (address & 0x3), data);
	    break;
	default:
	}
    }

    public int ioPortRead8(int address)
    {
	switch(address) {
	case 0xcfc:
	case 0xcfd:
	case 0xcfe:
	case 0xcff:
	    if ((configRegister & (1 << 31)) == 0)
		return 0xff;
	    else
		return 0xff & attachedBus.readPCIDataByte(configRegister | (address & 0x3));

	default:
	    return 0xff;
	}
    }

    public int ioPortRead16(int address)
    {
	switch(address) {
	case 0xcfc:
	case 0xcfd:
	case 0xcfe:
	case 0xcff:
	    if ((configRegister & (1 << 31)) == 0)
		return 0xffff;
	    else
		return 0xffff & attachedBus.readPCIDataWord(configRegister | (address & 0x3));
	default:
	    return 0xffff;
	}
    }

    public int ioPortRead32(int address)
    {
	switch(address) {
	case 0xcf8:
	case 0xcf9:
	case 0xcfa:
	case 0xcfb:
	    return configRegister;
	case 0xcfc:
	case 0xcfd:
	case 0xcfe:
	case 0xcff:
	    if ((configRegister & (1 << 31)) == 0)
		return 0xffffffff;
	    else
		return attachedBus.readPCIDataLong(configRegister | (address & 0x3));
	default:
	    return 0xffffffff;
	}
    }

    /* END IODevice Methods */

    public boolean configWriteByte(int address, byte data)
    {
        byte prior = super.configReadByte(address);
        boolean res = super.configWriteByte(address, data);
        if (data != prior)
        {
            if (address == 0x59)
            {
                boolean w = (data & (1<< 5)) != 0;
                boolean r = (data & (1<< 4)) != 0;
                for (int page = 0xF0000; page < 0x100000; page += 0x1000)
                {
                    memory.setEpromWritable(page, w);
                    memory.setEpromReadable(page, r);
                }
            }
            else if ((address > 0x59) && (address <= 0x5F))
            {
                boolean w1 = (data & (1<< 0)) != 0;
                boolean r1 = (data & (1<< 1)) != 0;
                int page = (address - 0x5a)*2*0x4000 + 0xC0000;
                for (int pa = page; pa < page + 0x4000; pa += 0x1000)
                {
                    memory.setEpromWritable(pa, w1);
                    memory.setEpromReadable(pa, r1);
                }
                boolean w2 = (data & (1<< 5)) != 0;
                boolean r2 = (data & (1<< 4)) != 0;
                page += 0x4000;
                for (int pa = page; pa < page + 0x4000; pa += 0x1000)
                {
                    memory.setEpromWritable(pa, w2);
                    memory.setEpromReadable(pa, r2);
                }
            }
            else
                System.out.println("SMM RAM control needs to be implemented..");
        }
        return res;
    }

    private boolean ioportRegistered;
    private boolean pciRegistered;

    public boolean initialised()
    {
        return ioportRegistered && pciRegistered && (memory != null);
    }

    public void reset()
    {
	attachedBus = null;
	pciRegistered = false;
	ioportRegistered = false;

	assignDeviceFunctionNumber(0);

        putConfigWord(PCI_CONFIG_VENDOR_ID, (short)0x8086); // Intel
        putConfigWord(PCI_CONFIG_DEVICE_ID, (short)0x1237); // device_id
        putConfigByte(PCI_CONFIG_REVISION, (byte)0x02); // revision
        putConfigWord(PCI_CONFIG_CLASS_DEVICE, (short)0x0600); // pci host bridge
	putConfigByte(PCI_CONFIG_HEADER, (byte)0x00); // header_type
    }

    public void acceptComponent(HardwareComponent component)
    {
        if ((component instanceof PCIBus) && component.initialised() && !pciRegistered) {
            attachedBus = (PCIBus)component;
            pciRegistered = attachedBus.registerDevice(this);
        }

        if ((component instanceof IOPortHandler)
                && component.initialised()) {
            ((IOPortHandler)component).registerIOPortCapable(this);
            ioportRegistered = true;
        }

        if ((component instanceof PhysicalAddressSpace) && component.initialised())
            memory = (PhysicalAddressSpace) component;
    }

    public boolean updated()
    {
        return ioportRegistered && pciRegistered;
    }

    public void updateComponent(HardwareComponent component)
    {
        if ((component instanceof PCIBus) && component.updated() && !pciRegistered)
        {
            //	    attachedBus = (PCIBus)component;
            pciRegistered = attachedBus.registerDevice(this);
        }

        if ((component instanceof IOPortHandler) && component.updated())
        {
            ((IOPortHandler)component).registerIOPortCapable(this);
            ioportRegistered = true;
        }
    }

    public String toString()
    {
        return "Intel i440FX PCI-Host Bridge";
    }
}
