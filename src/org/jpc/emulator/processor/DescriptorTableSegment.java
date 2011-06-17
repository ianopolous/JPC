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
class DescriptorTableSegment extends Segment
{
    private final int base;
    private final long limit;

    public DescriptorTableSegment(AddressSpace memory, int base, int limit)
    {
        super(memory);
        this.base = base;
        this.limit = 0xffffffffL & limit;
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeInt(2);
        output.writeInt(base);
        output.writeInt((int) limit);
    }

    public int getLimit()
    {
        return (int) limit;
    }

    public int getBase()
    {
        return base;
    }

    public int getSelector()
    {
        throw new IllegalStateException("No selector for a descriptor table segment");
    }

    public boolean setSelector(int selector)
    {
        throw new IllegalStateException("Cannot set a selector for a descriptor table segment");
    }

    public void checkAddress(int offset)
    {
        if ((0xffffffffL & offset) > limit)
        {
            System.out.println("Offset beyond end of Descriptor Table Segment: Offset=" + Integer.toHexString(offset) + ", limit=" + Long.toHexString(limit));
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, offset, true);
        }
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

    public int getDPL()
    {
        throw new IllegalStateException(getClass().toString());
    }

    public int getRPL()
    {
        throw new IllegalStateException(getClass().toString());
    }

    public void setRPL(int cpl)
    {
        throw new IllegalStateException(getClass().toString());
    }

    public boolean getDefaultSizeFlag()
    {
        throw new IllegalStateException(getClass().toString());
    }

    public int getType()
    {
        throw new IllegalStateException(getClass().toString());
    }

    public boolean isPresent()
    {
        return true;
    }

    public boolean isSystem()
    {
        return true;
    }

    public void printState()
    {
        System.out.println("Descriptor Table Segment");
        System.out.print("base: " + Integer.toHexString(base));
        System.out.println("limit: " + Long.toHexString(limit));
    }
}
