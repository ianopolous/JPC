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


package org.jpc.debugger;

import java.lang.reflect.*;
import java.awt.GridLayout;
import java.awt.event.*;

import javax.swing.*;

import org.jpc.emulator.processor.*;
import org.jpc.emulator.memory.*;

public class LinearMemoryViewer extends MemoryViewer implements ActionListener
{
    private PhysicalAddressSpace physicalMemory;
    private LinearAddressSpace linearMemory;

    private JRadioButton asRead, asWrite, asSupervisor, asUser;
    private Memory[] wu, ws, ru, rs;

    public LinearMemoryViewer(String title)
    {
        super(title);

        asRead = new JRadioButton("Reading", true);
        asRead.addActionListener(this);
        asWrite = new JRadioButton("Writing");
        asWrite.addActionListener(this);
        
        ButtonGroup group = new ButtonGroup();
        group.add(asRead);
        group.add(asWrite);
        
        asSupervisor = new JRadioButton("Supervisor", true);
        asSupervisor.addActionListener(this);
        asUser = new JRadioButton("User");
        asUser.addActionListener(this);

        group = new ButtonGroup();
        group.add(asSupervisor);
        group.add(asUser);

        JPanel controlPanel = new JPanel(new GridLayout(1, 0, 5, 5));
        controlPanel.add(asRead);
        controlPanel.add(asWrite);
        controlPanel.add(asSupervisor);
        controlPanel.add(asUser);

        //add("South", controlPanel);
    }

    public void actionPerformed(ActionEvent evt)
    {
        refreshDetails();
        
    }

    protected MemoryViewPanel createMemoryViewPanel()
    {
        return new LinearMemoryViewPanel();
    }

    private Memory[] getIndexArray(String name) throws Exception
    {
        Field f = LinearAddressSpace.class.getDeclaredField(name);
        f.setAccessible(true);
        return (Memory[]) f.get(linearMemory);
    }

    protected void getAddressSpace()
    {
        physicalMemory = (PhysicalAddressSpace) JPC.getObject(PhysicalAddressSpace.class);
        linearMemory = (LinearAddressSpace) JPC.getObject(LinearAddressSpace.class);
        memory = linearMemory;

        try
        {
            ru = getIndexArray("readUserIndex");
            rs = getIndexArray("readSupervisorIndex");
            wu = getIndexArray("writeUserIndex");
            ws = getIndexArray("writeSupervisorIndex");
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    class LinearMemoryViewPanel extends MemoryViewPanel
    {
        protected Object formatMemoryDisplay(int address)
        {
            controllable.lower.setBorder(BorderFactory.createTitledBorder("View Parameters : Physical address = " + Integer.toHexString(translateLinearAddressToInt(physicalMemory, processor, startAddress))));
            StringBuffer buf = new StringBuffer("<HTML>");
            for (int i=0; i<4; i++, address++)
            {
                Memory mem = translateLinearAddress(physicalMemory, processor, address);
                int index = address >>> AddressSpace.INDEX_SHIFT;
                String colourString = "009900";
                if (((rs != null) && (rs[index] != null)) || ((ru != null) && (ru[index] != null)) || 
                    ((ws != null) && (ws[index] != null)) || ((wu != null) && (wu[index] != null)))
                    colourString = "090290";

                if (mem == null)
                    buf.append("<font color='#FF0000'>N</font>");
                else
                {
                    int val = mem.getByte(address & AddressSpace.BLOCK_MASK);
                    buf.append("<font color='#"+colourString+"'>"+zeroPadHex(0xFF & val, 2)+"</font>");
                }
            }
            buf.append("</HTML>");
            return buf;
        }
        
        protected Object formatAsciiDisplay(int address)
        {
            StringBuffer buffer = new StringBuffer();
            for (int i=0; i<16; i++, address++)
            {
                Memory mem = translateLinearAddress(physicalMemory, processor, address);
                if (mem == null)
                    buffer.append(' ');
                else
                    buffer.append(getASCII(mem.getByte(address & AddressSpace.BLOCK_MASK)));
            }
        
            return buffer;
        }
    }

    public int translateLinearAddressToInt(PhysicalAddressSpace physical, Processor proc, int offset) {
        if ((proc.getCR0() & 0x80000000) == 0)
            return offset;

        int baseAddress = proc.getCR3() & 0xFFFFF000;
        int idx = offset >>> AddressSpace.INDEX_SHIFT;
        int directoryAddress = baseAddress | (0xFFC & (offset >>> 20)); // This should be (offset >>> 22) << 2.
        int directoryRawBits = physical.getDoubleWord(directoryAddress); 
        
        boolean directoryPresent = (0x1 & directoryRawBits) != 0;
        if (!directoryPresent) 
            return -1;

        int tableIndex = (0xFFC00000 & offset) >>> 12; 
        boolean directoryIs4MegPage = ((0x80 & directoryRawBits) != 0) && ((proc.getCR4() & 0x10) != 0);

        if (directoryIs4MegPage)
        {
            int fourMegPageStartAddress = 0xFFC00000 & directoryRawBits;
            return fourMegPageStartAddress | (offset & 0x3FFFFF);
        }
        else 
        {
            tableIndex = (0xFFFFF000 & offset) >>> 12;
	    int directoryBaseAddress = directoryRawBits & 0xFFFFF000;
            int tableAddress = directoryBaseAddress | ((offset >>> 10) & 0xFFC);
            int tableRawBits = physical.getDoubleWord(tableAddress); 
        
            boolean tablePresent = (0x1 & tableRawBits) != 0;
            if (!tablePresent)
                return -1;

            int fourKStartAddress = tableRawBits & 0xFFFFF000;
            return fourKStartAddress;
	}
    }
    
    public static Memory translateLinearAddress(PhysicalAddressSpace physical, Processor proc, int offset)
    {
        if ((proc.getCR0() & 0x80000000) == 0)
            return MemoryViewer.getReadMemoryBlockAt(physical, offset);

        int baseAddress = proc.getCR3() & 0xFFFFF000;
        int idx = offset >>> AddressSpace.INDEX_SHIFT;
        int directoryAddress = baseAddress | (0xFFC & (offset >>> 20)); // This should be (offset >>> 22) << 2.
        int directoryRawBits = physical.getDoubleWord(directoryAddress); 
        
        boolean directoryPresent = (0x1 & directoryRawBits) != 0;
        if (!directoryPresent) 
            return null;

        int tableIndex = (0xFFC00000 & offset) >>> 12; 
        boolean directoryIs4MegPage = ((0x80 & directoryRawBits) != 0) && ((proc.getCR4() & 0x10) != 0);

        if (directoryIs4MegPage)
        {
            int fourMegPageStartAddress = 0xFFC00000 & directoryRawBits;
            return MemoryViewer.getReadMemoryBlockAt(physical, fourMegPageStartAddress | (offset & 0x3FFFFF));
        }
        else 
        {
            tableIndex = (0xFFFFF000 & offset) >>> 12;
	    int directoryBaseAddress = directoryRawBits & 0xFFFFF000;
            int tableAddress = directoryBaseAddress | ((offset >>> 10) & 0xFFC);
            int tableRawBits = physical.getDoubleWord(tableAddress); 
        
            boolean tablePresent = (0x1 & tableRawBits) != 0;
            if (!tablePresent)
                return null;

            int fourKStartAddress = tableRawBits & 0xFFFFF000;
            return MemoryViewer.getReadMemoryBlockAt(physical, fourKStartAddress);
	}
    }
}
