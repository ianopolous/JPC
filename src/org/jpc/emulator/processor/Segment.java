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

import java.io.DataInput;
import java.io.IOException;
import org.jpc.emulator.memory.AddressSpace;
import org.jpc.emulator.Hibernatable;

/**
 * 
 * @author Chris Dennis
 */
public abstract class Segment implements Hibernatable
{
    protected AddressSpace memory;

    public Segment(AddressSpace memory)
    {
        this.memory = memory;
    }

    public final void setAddressSpace(AddressSpace memory)
    {
        this.memory = memory;
    }

    public abstract boolean isPresent();

    public abstract boolean isSystem();

    public abstract int getType();

    public abstract int getSelector();

    public abstract int getLimit();

    public abstract int getBase();

    public abstract boolean getDefaultSizeFlag();
    
    public abstract int getRPL();

    public abstract void setRPL(int cpl);
    
    public abstract int getDPL();
    
    public abstract boolean setSelector(int selector);

    public abstract void checkAddress(int offset) throws ProcessorException;

    public abstract int translateAddressRead(int offset);

    public abstract int translateAddressWrite(int offset);

    public abstract void printState();

    public byte getByte(int offset)
    {
        return memory.getByte(translateAddressRead(offset));
    }

    public short getWord(int offset)
    {
        return memory.getWord(translateAddressRead(offset));
    }

    public int getDoubleWord(int offset)
    {
        return memory.getDoubleWord(translateAddressRead(offset));
    }

    public long getQuadWord(int offset)
    {
        int off = translateAddressRead(offset);
        long result = 0xFFFFFFFFl & memory.getDoubleWord(off);
        off = translateAddressRead(offset + 4);
        result |= (((long) memory.getDoubleWord(off)) << 32);
        return result;
    }

    public void setByte(int offset, byte data)
    {
        memory.setByte(translateAddressWrite(offset), data);
    }

    public void setWord(int offset, short data)
    {
        memory.setWord(translateAddressWrite(offset), data);
    }

    public void setDoubleWord(int offset, int data)
    {
        memory.setDoubleWord(translateAddressWrite(offset), data);
    }

    public void setQuadWord(int offset, long data)
    {
        int off = translateAddressWrite(offset);
        memory.setDoubleWord(off, (int) data);
        off = translateAddressWrite(offset + 4);
        memory.setDoubleWord(off, (int) (data >>> 32));
    }
    
    public void loadState(DataInput input) throws IOException
    {
        throw new UnsupportedOperationException();
    }
}
