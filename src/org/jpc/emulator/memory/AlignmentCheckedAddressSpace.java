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
import org.jpc.emulator.processor.ProcessorException;

/**
 * Class that implements an alignment checking skin on another <code>AddressSpace</code>
 * instance.  Access that are not alignment on the correct granularity will
 * trigger a {@link org.jpc.emulator.processor.ProcessorException} with the
 * appropriate vector value.
 * @author Chris Dennis
 */
public class AlignmentCheckedAddressSpace extends AddressSpace
{
    private final AddressSpace addressSpace;

    /**
     * Constructs an address space wrapping the supplied target.
     * @param target address space to be wrapped.
     */
    public AlignmentCheckedAddressSpace(AddressSpace target)
    {
        addressSpace = target;
    }

    protected Memory getReadMemoryBlockAt(int offset)
    {
        return addressSpace.getReadMemoryBlockAt(offset);
    }

    protected Memory getWriteMemoryBlockAt(int offset)
    {
        return addressSpace.getWriteMemoryBlockAt(offset);
    }

    protected void replaceBlocks(Memory oldBlock, Memory newBlock)
    {
        addressSpace.replaceBlocks(oldBlock, newBlock);
    }

    public int executeReal(Processor cpu, int offset)
    {
        return addressSpace.executeReal(cpu, offset);
    }

    public int executeProtected(Processor cpu, int offset)
    {
        return addressSpace.executeReal(cpu, offset);
    }

    public int executeVirtual8086(Processor cpu, int offset)
    {
        return addressSpace.executeReal(cpu, offset);
    }

    public void clear()
    {
        addressSpace.clear();
    }

    public byte getByte(int offset)
    {
        return addressSpace.getByte(offset);
    }

    public void setByte(int offset, byte data)
    {
        addressSpace.setByte(offset, data);
    }

    /**
     * Throws a <code>ProcessorException</code> if the access is not aligned to
     * a word (two-byte) boundary.
     * @param offset address to be read.
     * @return short value read.
     */
    public short getWord(int offset)
    {
        if ((offset & 0x1) != 0)
            throw ProcessorException.ALIGNMENT_CHECK_0;

        return addressSpace.getWord(offset);
    }

    /**
     * Throws a <code>ProcessorException</code> if the access is not aligned to
     * a doubleword (four-byte) boundary.
     * @param offset address to be read.
     * @return int value read.
     */
    public int getDoubleWord(int offset)
    {
        if ((offset & 0x3) != 0)
            throw ProcessorException.ALIGNMENT_CHECK_0;

        return addressSpace.getDoubleWord(offset);
    }

    /**
     * Throws a <code>ProcessorException</code> if the access is not aligned to
     * a quad-word (eight-byte) boundary.
     * @param offset address to be read.
     * @return int value read.
     */
    public long getQuadWord(int offset)
    {
        if ((offset & 0x7) != 0)
            throw ProcessorException.ALIGNMENT_CHECK_0;

        return addressSpace.getQuadWord(offset);
    }

    /**
     * Throws a <code>ProcessorException</code> if the access is not aligned to
     * a octa-word (sixteen-byte) boundary.
     * @param offset address to be read.
     * @return long value read.
     */
    public long getLowerDoubleQuadWord(int offset)
    {
        if ((offset & 0xF) != 0)
            throw ProcessorException.ALIGNMENT_CHECK_0;

        return addressSpace.getLowerDoubleQuadWord(offset);
    }

    public long getUpperDoubleQuadWord(int offset)
    {
        return addressSpace.getUpperDoubleQuadWord(offset);
    }

    /**
     * Throws a <code>ProcessorException</code> if the access is not aligned to
     * a word (two-byte) boundary.
     * @param offset address to be read.
     * @param data value to write.
     */
    public void setWord(int offset, short data)
    {
        if ((offset & 0x1) != 0)
            throw ProcessorException.ALIGNMENT_CHECK_0;

        addressSpace.setWord(offset, data);
    }

    /**
     * Throws a <code>ProcessorException</code> if the access is not aligned to
     * a doubleword (four-byte) boundary.
     * @param offset address to be read.
     * @param data value to write.
     */
    public void setDoubleWord(int offset, int data)
    {
        if ((offset & 0x3) != 0)
            throw ProcessorException.ALIGNMENT_CHECK_0;

        addressSpace.setDoubleWord(offset, data);
    }

    /**
     * Throws a <code>ProcessorException</code> if the access is not aligned to
     * a quadword (eight-byte) boundary.
     * @param offset address to be read.
     * @param data value to write.
     */
    public void setQuadWord(int offset, long data)
    {
        if ((offset & 0x7) != 0)
            throw ProcessorException.ALIGNMENT_CHECK_0;

        addressSpace.setQuadWord(offset, data);
    }

    /**
     * Throws a <code>ProcessorException</code> if the access is not aligned to
     * a octa-word (sixteen-byte) boundary.
     * @param offset address to be read.
     * @param data value to write.
     */
    public void setLowerDoubleQuadWord(int offset, long data)
    {
        if ((offset & 0xF) != 0)
            throw ProcessorException.GENERAL_PROTECTION_0;

        addressSpace.setLowerDoubleQuadWord(offset, data);
    }

    public void setUpperDoubleQuadWord(int offset, long data)
    {
        addressSpace.setUpperDoubleQuadWord(offset, data);
    }

    public void copyArrayIntoContents(int address, byte[] buffer, int off, int len)
    {
        addressSpace.copyArrayIntoContents(address, buffer, off, len);
    }

    public void copyContentsIntoArray(int address, byte[] buffer, int off, int len)
    {
        addressSpace.copyContentsIntoArray(address, buffer, off, len);
    }

    public void loadInitialContents(int address, byte[] buf, int off, int len) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
