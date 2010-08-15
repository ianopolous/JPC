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

import java.io.DataOutput;
import java.io.IOException;
import org.jpc.emulator.memory.AddressSpace;

/**
 *
 * @author Ian Preston
 */
public class Virtual8086ModeSegment extends Segment {
    private int selector;
    private int base;
    private int type;
    private int dpl, rpl;
    private long limit;

    public Virtual8086ModeSegment(AddressSpace memory, int selector, boolean isCode)
    {
        super(memory);
        this.selector = selector;
        base = selector << 4;
        limit = 0xffffL;
        dpl = 3;
        rpl = 3;
        if (isCode)
            type = ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_READABLE | ProtectedModeSegment.TYPE_ACCESSED;
        else
            type = ProtectedModeSegment.TYPE_DATA_WRITABLE | ProtectedModeSegment.TYPE_ACCESSED;
    }

    public Virtual8086ModeSegment(AddressSpace memory, Segment ancestor)
    {
        super(memory);
        selector = ancestor.getSelector();
        base = ancestor.getBase();
        type = ancestor.getType();
        limit = 0xffffffffL & ancestor.getLimit();
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeInt(1);
        output.writeInt(selector);
        if (type == (ProtectedModeSegment.TYPE_CODE | ProtectedModeSegment.TYPE_CODE_READABLE | ProtectedModeSegment.TYPE_ACCESSED))
            output.writeBoolean(true);
        else
            output.writeBoolean(false);
        output.writeInt(rpl);
    }

    public boolean getDefaultSizeFlag()
    {
        return false;
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
            throw ProcessorException.GENERAL_PROTECTION_0;
    }

    public int translateAddressRead(int offset)
    {
        checkAddress(offset);
        return base + offset;
    }

    public int translateAddressWrite(int offset)
    {
        checkAddress(offset);
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
        return true;
    }

    public boolean isSystem()
    {
        return false;
    }

    public int getDPL()
    {
        return dpl;
    }

    public void setRPL(int cpl)
    {
        rpl = cpl;
    }

    public void printState()
    {
        System.out.println("VM86 Segment");
        System.out.println("selector: " + Integer.toHexString(selector));
        System.out.println("base: " + Integer.toHexString(base));
        System.out.println("dpl: " + Integer.toHexString(dpl));
        System.out.println("rpl: " + Integer.toHexString(rpl));
        System.out.println("limit: " + Long.toHexString(limit));
        System.out.println("type: " + Integer.toHexString(type));
    }
}
