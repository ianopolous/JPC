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
 * A region of memory that can be read from and written to in all sizes from byte to
 * quad-word.  Also supports execution from an arbitrary point on a given processor state.
 * 
 * @author Chris Dennis
 */
public interface Memory
{

    /**
     * Returns true if this <code>Memory</code> object has had heap allocated for
     * it.
     * <p>
     * For most memory objects a <code>true</code> return implies that there are
     * some non-zero values stored.
     * @return <code>true</code> if heap is allocated for this object.
     */
    public boolean isAllocated();

    /**
     * Clears the entire memory object so that all bytes are zero.
     */
    public void clear();

    /**
     * Sets <code>length</code> bytes to zero from <code>start</code> (inclusive)
     * to <code>(start + length)</code> (exclusive).
     * @param start index of first byte to be cleared.
     * @param length number of bytes to clear.
     */
    public void clear(int start, int length);

    /**
     * Returns the size of this memory object in bytes as a long
     * @return size of this memory object.
     */
    public long getSize();

    /**
     * Gets the value of the specified byte.
     * @param offset index of the byte
     * @return byte value at <code>offset</code>
     */
    public byte getByte(int offset);

    /**
     * Gets the word value starting at <code>offset</code> in little endian format.
     * @param offset index of the first byte
     * @return word value at <code>offset</code>
     */
    public short getWord(int offset);

    /**
     * Gets the doubleword value starting at <code>offset</code> in little endian format.
     * @param offset index of the first byte
     * @return doubleword value at <code>offset</code>
     */
    public int getDoubleWord(int offset);

    /**
     * Gets the quadword value starting at <code>offset</code> in little endian format.
     * @param offset index of the first byte
     * @return quadword value at <code>offset</code>
     */
    public long getQuadWord(int offset);

    /**
     * Gets the least significant 64bits of an octa-word value starting at <code>offset</code> in little endian format.
     * @param offset index of the first byte
     * @return lowest 64bits of the octaword value starting at <code>offset</code>
     */
    public long getLowerDoubleQuadWord(int offset);

    /**
     * Gets the most significant 64bits of an octa-word value starting at <code>offset</code> in little endian format.
     * @param offset index of the first byte
     * @return highest 64bits of the octaword value starting at <code>offset</code>
     */
    public long getUpperDoubleQuadWord(int offset);

    /**
     * Sets the value of the specified byte.
     * @param offset index of the byte.
     * @param data new value.
     */
    public void setByte(int offset, byte data);

    /**
     * Sets the word value starting at <code>index</code> in little-endian format.
     * @param offset index of the first byte.
     * @param data word value as a short.
     */
    public void setWord(int offset, short data);

    /**
     * Sets the doubleword value starting at <code>index</code> in little-endian format.
     * @param offset index of the first byte.
     * @param data doubleword value as an int.
     */
    public void setDoubleWord(int offset, int data);

    /**
     * Sets the quadword value starting at <code>index</code> in little-endian format.
     * @param offset index of the first byte.
     * @param data quadword value as a long.
     */
    public void setQuadWord(int offset, long data);

    /**
     * Sets the least significant 64bits of an octa-word value starting at <code>index</code>
     * in little-endian format.
     * @param offset index of the first byte.
     * @param data lowest 64bits of the octa-word value as a long.
     */
    public void setLowerDoubleQuadWord(int offset, long data);

    /**
     * Sets the most significant 64bits of an octa-word value starting at
     * <code>index</code> in little-endian format.
     * @param offset index of the first byte.
     * @param data highest 64bits of the octa-word value as a long.
     */
    public void setUpperDoubleQuadWord(int offset, long data);

    /**
     * Copies <code>len</code> bytes starting at <code>address</code> from this
     * memory object into <code>buffer</code>.
     * @param address start address to copy from.
     * @param buffer array to copy data into.
     * @param off start address to copy to.
     * @param len number of bytes to copy.
     */
    public void copyContentsIntoArray(int address, byte[] buffer, int off, int len);

    /**
     * Copies <code>len</code> bytes starting at <code>off</code> from
     * <code>buffer</code> into this memory object at <code>address</code>.
     * @param address start address to copy to.
     * @param buffer array to copy data from.
     * @param off start address to copy from.
     * @param len number of bytes to copy.
     */
    public void copyArrayIntoContents(int address, byte[] buffer, int off, int len);

    /**
     * Copies <code>len</code> bytes starting at <code>off</code> from
     * <code>buffer</code> into this memory object at <code>address</code>, but does
     * not initialise code block arrays.
     * @param address start address to copy to.
     * @param buf array to copy data from.
     * @param off start address to copy from.
     * @param len number of bytes to copy.
     */
    public void loadInitialContents(int address, byte[] buf, int off, int len);
    
    /**
     * Execute the x86 instructions starting at <code>address</code> on the
     * specified processor context.
     * @param cpu processor on which to operate.
     * @param address start address to execute from.
     * @return number of x86 instructions executed.
     */
    public int executeReal(Processor cpu, int address);

    /**
     * Execute the x86 instructions starting at <code>address</code> on the
     * specified processor context.
     * @param cpu processor on which to operate.
     * @param address start address to execute from.
     * @return number of x86 instructions executed.
     */
    public int executeProtected(Processor cpu, int address);

    /**
     * Execute the x86 instructions starting at <code>address</code> on the
     * specified processor context.
     * @param cpu processor on which to operate.
     * @param address start address to execute from.
     * @return number of x86 instructions executed.
     */
    public int executeVirtual8086(Processor cpu, int address);
}
