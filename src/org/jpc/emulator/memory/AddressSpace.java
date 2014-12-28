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

package org.jpc.emulator.memory;

import org.jpc.emulator.processor.Processor;

/**
 * Represents a complete 32-bit address-space composed of a sequence of blocks
 * <code>BLOCK_SIZE</code> long.
 * @author Ian Preston
 */
public abstract class AddressSpace extends AbstractMemory 
{
    public static final int BLOCK_SIZE = 4*1024;
    public static final int BLOCK_MASK = BLOCK_SIZE-1;
    public static final int INDEX_MASK = ~(BLOCK_MASK);
    public static final int INDEX_SHIFT = 12;
    public static final int INDEX_SIZE = 1 << (32 - INDEX_SHIFT);

    /**
     * Returns the size of the <code>AddressSpace</code> which is always 2<sup>32</sup>.
     * @return 2<sup>32</sup>
     */
    public final long getSize()
    {
        return 0x100000000l;
    }

    public boolean isAllocated()
    {
        return true;
    }
    
    /**
     * Get a <code>Memory</code> instance suitable for reading from this address.
     * @param offset address to be written to
     * @return block covering this address
     */
    protected abstract Memory getReadMemoryBlockAt(int offset);

    /**
     * Get a <code>Memory</code> instance suitable for writing to this address.
     * @param offset address to be written to
     * @return block covering this address
     */
    protected abstract Memory getWriteMemoryBlockAt(int offset);

    public abstract void clear();

    public abstract byte getByte(int offset);

    public abstract void setByte(int offset, byte data);

    public short getWord(int offset)
    {
        return super.getWord(offset);
    }

    public int getDoubleWord(int offset)
    {
        return super.getDoubleWord(offset);
    }

    public long getQuadWord(int offset)
    {
        try
        {
            return getReadMemoryBlockAt(offset).getQuadWord(offset & BLOCK_MASK);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            return super.getQuadWord(offset);
        }
    }

    public long getLowerDoubleQuadWord(int offset)
    {
        try
        {
            return getReadMemoryBlockAt(offset).getLowerDoubleQuadWord(offset & BLOCK_MASK);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            return super.getLowerDoubleQuadWord(offset);
        }
    }

    public long getUpperDoubleQuadWord(int offset)
    {
        try
        {
            return getReadMemoryBlockAt(offset).getUpperDoubleQuadWord(offset & BLOCK_MASK);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            return super.getUpperDoubleQuadWord(offset);
        }
    }

    public void setWord(int offset, short data)
    {
        super.setWord(offset, data);
    }

    public void setDoubleWord(int offset, int data)
    {
        super.setDoubleWord(offset, data);
    }

    public void setQuadWord(int offset, long data)
    {
        try
        {
            getWriteMemoryBlockAt(offset).setQuadWord(offset & BLOCK_MASK, data);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            super.setQuadWord(offset, data);
        }
    }

    public void setLowerDoubleQuadWord(int offset, long data)
    {
        try
        {
            getWriteMemoryBlockAt(offset).setLowerDoubleQuadWord(offset & BLOCK_MASK, data);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            super.setLowerDoubleQuadWord(offset, data);
        }
    }

    public void setUpperDoubleQuadWord(int offset, long data)
    {
//        System.out.println("Mem.setupperquad " + offset);
        try
        {
            getWriteMemoryBlockAt(offset).setUpperDoubleQuadWord(offset & BLOCK_MASK, data);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            super.setUpperDoubleQuadWord(offset, data);
        }
    }

    public void copyArrayIntoContents(int address, byte[] buffer, int off, int len)
    {
        do {
            int partialLength = Math.min(BLOCK_SIZE - (address & BLOCK_MASK), len);
            Memory block = getWriteMemoryBlockAt(address);
            if ((block == null) || block instanceof PhysicalAddressSpace.UnconnectedMemoryBlock)
                if (this instanceof PhysicalAddressSpace)
                {
                    block = new LazyCodeBlockMemory(BLOCK_SIZE, ((PhysicalAddressSpace) this).getCodeBlockManager());
                    ((PhysicalAddressSpace) this).mapMemory(address, block);
                }
            block.copyArrayIntoContents(address & BLOCK_MASK, buffer, off, partialLength);
            address += partialLength;
            off += partialLength;
            len -= partialLength;
        } while (len > 0);
    }

    public void copyContentsIntoArray(int address, byte[] buffer, int off, int len)
    {
	do {
	    int partialLength = Math.min(BLOCK_SIZE - (address & BLOCK_MASK), len);
	    getReadMemoryBlockAt(address).copyContentsIntoArray(address & BLOCK_MASK, buffer, off, partialLength);
	    address += partialLength;
	    off += partialLength;	    
	    len -= partialLength;
	} while (len > 0);
    }

    /**
     * Replace all references to <code>original</code> with references to
     * <code>replacement</code>.
     * @param original block to be replaced.
     * @param replacement block to be added.
     */
    protected abstract void replaceBlocks(Memory original, Memory replacement);

    public abstract int executeReal(Processor cpu, int address);
    public abstract int executeProtected(Processor cpu, int address);
    public abstract int executeVirtual8086(Processor cpu, int address);
}
