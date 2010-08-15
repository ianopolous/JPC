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

package org.jpc.emulator.processor;

import java.io.*;

import org.jpc.emulator.memory.AddressSpace;

/**
 * 
 * @author Chris Dennis
 */
final class RealModeSegment extends Segment
{
    private int selector;
    private int base;
    private int type;
    private long limit;
    private int rpl;
    private boolean defaultSize = false;
    private boolean segment = true;
    private boolean present = true;

    public RealModeSegment(AddressSpace memory, int selector)
    {
        super(memory);
        this.selector = selector;
        base = selector << 4;
        limit = 0xffffL;
        rpl = 0;
        type = ProtectedModeSegment.TYPE_DATA_WRITABLE | ProtectedModeSegment.TYPE_ACCESSED;
    }

    public RealModeSegment(AddressSpace memory, Segment ancestor)
    {
        super(memory);
        selector = ancestor.getSelector();
        base = ancestor.getBase();
        type = ancestor.getType();
        limit = 0xffffffffL & ancestor.getLimit();
        defaultSize = ancestor.getDefaultSizeFlag();
        segment = !ancestor.isSystem();
        present = ancestor.isPresent();
        rpl = ancestor.getRPL();
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeInt(0);
        output.writeInt(selector);
        output.writeInt(type);
        output.writeInt(rpl);
        output.writeLong(limit);
        output.writeBoolean(defaultSize);
        output.writeBoolean(segment);
        output.writeBoolean(present);
    }

    public void loadState(DataInput input) throws IOException
    {
        type = input.readInt();
        rpl = input.readInt();
        limit = input.readLong();
        defaultSize = input.readBoolean();
        segment = input.readBoolean();
        present = input.readBoolean();
    }

    public boolean getDefaultSizeFlag()
    {
        return defaultSize;
    }

    public int getLimit()
    {
        return (int)limit;
    }

    public int getBase()
    {
        return base;
    }

    public int getSelector()
    {
        return selector;
    }

    public boolean setSelector(int selector)
    {
        this.selector = selector;
        base = selector << 4;
        type = ProtectedModeSegment.TYPE_DATA_WRITABLE | ProtectedModeSegment.TYPE_ACCESSED;
        return true;
    }

    public void checkAddress(int offset)
    {
        if ((0xffffffffL & offset) > limit)
        {
            System.out.println("RM Segment Limit exceeded: offset=" + Integer.toHexString(offset) + ", limit=" + Long.toHexString(limit));
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        }
    }

    public int translateAddressRead(int offset)
    {
        //checkAddress(offset);
        return base + offset;
    }

    public int translateAddressWrite(int offset)
    {
        //checkAddress(offset);
        return base + offset;
    }

    public int getRPL()
    {
        return rpl;
    }

    public int getType()
    {
        return type;
    }

    public boolean isPresent()
    {
        return present;
    }

    public boolean isSystem()
    {
        return !segment;
    }

    public int getDPL()
    {
        throw new IllegalStateException(getClass().toString());
    }

    public void setRPL(int cpl)
    {
        throw new IllegalStateException(getClass().toString());
    }

    public void printState()
    {
        System.out.println("RM Segment");
        System.out.println("selector: " + Integer.toHexString(selector));
        System.out.println("base: " + Integer.toHexString(base));
        System.out.println("rpl: " + Integer.toHexString(rpl));
        System.out.println("limit: " + Long.toHexString(limit));
        System.out.println("type: " + Integer.toHexString(type));
        System.out.println("defaultSize: " + defaultSize);
        System.out.println("segment: " + segment);
        System.out.println("present: " + present);
    }
}
