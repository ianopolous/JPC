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

import org.jpc.emulator.motherboard.InterruptController;
import org.jpc.emulator.HardwareComponent;

import java.io.*;

/** 
 * Emulates the PCI-ISA bridge functionality of the Intel 82371SB PIIX3
 * southbridge.
 * <p>
 * The key function of this component is the interfacing between the PCI 
 * components and the interrupt controller that forms part of the southbridge.
 * @author Chris Dennis
 */
public class PCIISABridge extends AbstractPCIDevice
{
    private int irqLevels[][];
    private InterruptController irqDevice;

    /**
     * Constructs the (singleton) PCI-ISA bridge instance for attachment to a
     * PCI bus.
     */
    public PCIISABridge()
    {
	irqLevels = new int[4][2];

        putConfigWord(PCI_CONFIG_VENDOR_ID, (short)0x8086); // Intel
        putConfigWord(PCI_CONFIG_DEVICE_ID, (short)0x7000); // 82371SB PIIX3 PCI-to-ISA bridge (Step A1)
        putConfigWord(PCI_CONFIG_CLASS_DEVICE, (short)0x0601); // ISA Bridge
	putConfigByte(PCI_CONFIG_HEADER, (byte)0x80); // PCI_multifunction

        this.internalReset();
    }

    public void saveState(DataOutput output) throws IOException
    {
        super.saveState(output);
        output.writeInt(irqLevels.length);
        output.writeInt(irqLevels[0].length);
        for (int[] inner : irqLevels)
            for (int level : inner)
                output.writeInt(level);
    }

    public void loadState(DataInput input) throws IOException
    {
        super.reset();
        super.loadState(input);
        int len = input.readInt();
        int width = input.readInt();
        irqLevels = new int[len][width];
        for (int i = 0; i < len; i++)
            for (int j = 0; j < width; j++)
                irqLevels[i][j] = input.readInt();
    }

    private void internalReset()
    {
        putConfigWord(PCI_CONFIG_COMMAND, (short)0x0007); // master, memory and I/O
        putConfigWord(PCI_CONFIG_STATUS, (short)0x0200); // PCI_status_devsel_medium
	putConfigByte(0x4c, (byte)0x4d);
	putConfigByte(0x4e, (byte)0x03);
	putConfigByte(0x4f, (byte)0x00);
	putConfigByte(0x60, (byte)0x80);
	putConfigByte(0x69, (byte)0x02);
	putConfigByte(0x70, (byte)0x80);
	putConfigByte(0x76, (byte)0x0c);
	putConfigByte(0x77, (byte)0x0c);
	putConfigByte(0x78, (byte)0x02);
	putConfigByte(0x79, (byte)0x00);
	putConfigByte(0x80, (byte)0x00);
	putConfigByte(0x82, (byte)0x00);
	putConfigByte(0xa0, (byte)0x08);
	putConfigByte(0xa2, (byte)0x00);
	putConfigByte(0xa3, (byte)0x00);
	putConfigByte(0xa4, (byte)0x00);
	putConfigByte(0xa5, (byte)0x00);
	putConfigByte(0xa6, (byte)0x00);
	putConfigByte(0xa7, (byte)0x00);
	putConfigByte(0xa8, (byte)0x0f);
	putConfigByte(0xaa, (byte)0x00);
	putConfigByte(0xab, (byte)0x00);
	putConfigByte(0xac, (byte)0x00);
	putConfigByte(0xae, (byte)0x00);
    }
    
    private void setIRQ(PCIDevice device, int irqNumber, int level)
    {
	irqNumber = this.slotGetPIRQ(device, irqNumber);
	int irqIndex = device.getIRQIndex();
	int shift = (irqIndex & 0x1f);
	int p = irqLevels[irqNumber][irqIndex >> 5];
	irqLevels[irqNumber][irqIndex >> 5] = (p & ~(1 << shift)) | (level << shift);
	
	/* now we change the pic irq level according to the piix irq mappings */
	int picIRQ = this.configReadByte(0x60 + irqNumber); //short/int/long?
	if (picIRQ < 16) {
	    /* the pic level is the logical OR of all the PCI irqs mapped to it */
	    int picLevel = 0;
	    for (int i = 0; i < 4; i++) {
		if (picIRQ == this.configReadByte(0x60 + i))
		    picLevel |= getIRQLevel(i);
	    }
	    irqDevice.setIRQ(picIRQ, picLevel);
	}
    }


    private int getIRQLevel(int irqNumber)
    {
	for(int i = 0; i < PCIBus.PCI_IRQ_WORDS; i++) {
	    if (irqLevels[irqNumber][i] != 0) {
		return 1;
	    }
	}
	return 0;	
    }

    IRQBouncer makeBouncer(PCIDevice device)
    {
	return new DefaultIRQBouncer();
    }

    int slotGetPIRQ(PCIDevice device, int irqNumber)
    {
	int slotAddEnd;
	slotAddEnd = (device.getDeviceFunctionNumber() >> 3);
	return (irqNumber + slotAddEnd) & 0x3;
    }

    private class DefaultIRQBouncer implements IRQBouncer
    {
	DefaultIRQBouncer()
	{
	}

	public void setIRQ(PCIDevice device, int irqNumber, int level)
	{
            PCIISABridge.this.setIRQ(device, irqNumber, level);
	}
    }

    public IORegion getIORegion(int index)
    {
	return null;
    }

    public IORegion[] getIORegions()
    {
	return null;
    }

    public void reset()
    {
	irqDevice = null;

        putConfigWord(PCI_CONFIG_VENDOR_ID, (short)0x8086); // Intel
        putConfigWord(PCI_CONFIG_DEVICE_ID, (short)0x7000); // 82371SB PIIX3 PCI-to-ISA bridge (Step A1)
        putConfigWord(PCI_CONFIG_CLASS_DEVICE, (short)0x0601); // ISA Bridge
	putConfigByte(PCI_CONFIG_HEADER, (byte)0x80); // PCI_multifunction
	internalReset();

	super.reset();
    }

    public boolean initialised()
    {
	return (irqDevice != null) && super.initialised();
    }

    public void acceptComponent(HardwareComponent component)
    {
	if ((component instanceof InterruptController) && component.initialised())
	    irqDevice = (InterruptController)component;

	super.acceptComponent(component);
    }

    public boolean updated()
    {
	return (irqDevice.updated()) && super.updated();
    }

    public void updateComponent(HardwareComponent component)
    {
        //	if ((component instanceof InterruptController)
        //	    && component.updated())
        //	    irqDevice = (InterruptController)component;

	super.acceptComponent(component);
    }

    public String toString()
    {
	return "Intel 82371SB PIIX3 PCI ISA Bridge";
    }
}

