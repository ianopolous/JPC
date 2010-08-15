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

import java.io.*;

import org.jpc.emulator.*;
import org.jpc.emulator.memory.codeblock.CodeBlockManager;
import org.jpc.emulator.processor.Processor;

/**
 * Class that emulates the 32bit physical address space of the machine.  Mappings
 * between address and blocks are performed either using a single stage lookup on
 * the RAM area for speed, or a two stage lookup for the rest of the address space
 * for space-efficiency.
 * <p>
 * All addresses are initially mapped to an inner class instance that returns
 * <code>-1</code> on all reads (as all data lines float high).
 * @author Chris Dennis
 */
public final class PhysicalAddressSpace extends AddressSpace implements HardwareComponent {

    private static final int GATEA20_MASK = 0xffefffff;
    private static final int QUICK_INDEX_SIZE = PC.SYS_RAM_SIZE >>> INDEX_SHIFT;
    private static final int TOP_INDEX_BITS = (32 - INDEX_SHIFT) / 2;
    private static final int BOTTOM_INDEX_BITS = 32 - INDEX_SHIFT - TOP_INDEX_BITS;
    private static final int TOP_INDEX_SHIFT = 32 - TOP_INDEX_BITS;
    private static final int TOP_INDEX_SIZE = 1 << TOP_INDEX_BITS;
    private static final int TOP_INDEX_MASK = TOP_INDEX_SIZE - 1;
    private static final int BOTTOM_INDEX_SHIFT = 32 - TOP_INDEX_BITS - BOTTOM_INDEX_BITS;
    private static final int BOTTOM_INDEX_SIZE = 1 << BOTTOM_INDEX_BITS;
    private static final int BOTTOM_INDEX_MASK = BOTTOM_INDEX_SIZE - 1;
    private static final Memory UNCONNECTED = new UnconnectedMemoryBlock();
    private boolean gateA20MaskState;
    private Memory[] quickNonA20MaskedIndex,  quickA20MaskedIndex,  quickIndex;
    private Memory[][] nonA20MaskedIndex,  a20MaskedIndex,  index;
    private LinearAddressSpace linearAddr;
    private CodeBlockManager manager = null;

    /**
     * Constructs an address space which is initially empty.  All addresses are 
     * mapped to an instance of the inner class <code>UnconnectedMemoryBlock</code>
     * whose data lines float high.
     */
    public PhysicalAddressSpace(CodeBlockManager manager) {
        this.manager = manager;
        quickNonA20MaskedIndex = new Memory[QUICK_INDEX_SIZE];
        clearArray(quickNonA20MaskedIndex, UNCONNECTED);
        quickA20MaskedIndex = new Memory[QUICK_INDEX_SIZE];
        clearArray(quickA20MaskedIndex, UNCONNECTED);

        nonA20MaskedIndex = new Memory[TOP_INDEX_SIZE][];
        a20MaskedIndex = new Memory[TOP_INDEX_SIZE][];

        initialiseMemory();
        setGateA20State(false);
    }

    public void saveState(DataOutput output) throws IOException {
        output.writeBoolean(gateA20MaskState);
        dumpMemory(output, quickNonA20MaskedIndex, nonA20MaskedIndex);
    }

    private static void dumpMemory(DataOutput output, Memory[] quick, Memory[][] full) throws IOException {
        byte[] temp = new byte[0];
        output.writeInt(quick.length);
        for (Memory block : quick) {
            int blockLength = (int) block.getSize();
            if (block.isAllocated()) {
                try {
                    if (block instanceof MapWrapper) {
                        output.writeInt(0);
                    } else {
                        if (temp.length < blockLength) {
                            temp = new byte[blockLength];
                        }
                        block.copyContentsIntoArray(0, temp, 0, blockLength);
                        output.writeInt(blockLength);
                        output.write(temp);
                    }
                } catch (IllegalStateException e) {
                    output.writeInt(0);
                }
            } else {
                output.writeInt(0);
            }
        }

        output.writeInt(full.length);
        for (Memory[] chunk : full) {
            if (chunk == null) {
                output.writeInt(0);
                continue;
            }

            output.writeInt(chunk.length);
            for (Memory block : chunk) {
                if ((block == null) || (!block.isAllocated())) {
                    output.writeInt(0);
                    continue;
                }
                int blockLength = (int) block.getSize();
                if (block.isAllocated()) {
                    try {
                        if (block instanceof MapWrapper) {
                            output.writeInt(0);
                        } else {
                            if (temp.length < blockLength) {
                                temp = new byte[blockLength];
                            }
                            block.copyContentsIntoArray(0, temp, 0, blockLength);
                            output.writeInt(blockLength);
                            output.write(temp);
                        }
                    } catch (IllegalStateException e) {
                        output.writeInt(0);
                    }
                } else {
                    output.writeInt(0);
                }
            }
        }
    }

    private static void loadMemory(DataInput input, Memory[] quick, Memory[][] full) throws IOException {
        byte[] temp = new byte[0];
        int quickLength = input.readInt();
        for (int i = 0; i < quickLength; i++) {
            Memory block = quick[i];
            int blockLength = input.readInt();
            if (blockLength > 0) {
                if (blockLength > temp.length) {
                    temp = new byte[blockLength];
                }
                input.readFully(temp, 0, blockLength);
                block.loadInitialContents(0, temp, 0, blockLength);
            }
        }

        int fullLength = input.readInt();
        for (int i = 0; i < fullLength; i++) {
            Memory[] chunk = full[i];
            int chunkLength = input.readInt();

            for (int j = 0; j < chunkLength; j++) {
                Memory block = chunk[j];
                int blockLength = input.readInt();
                if (blockLength > 0) {
                    if (blockLength > temp.length) {
                        temp = new byte[blockLength];
                    }
                    input.readFully(temp, 0, blockLength);
                    block.loadInitialContents(0, temp, 0, blockLength);
                }
            }
        }
    }

    private void initialiseMemory()
    {
        for (int i = 0; i < PC.SYS_RAM_SIZE; i += AddressSpace.BLOCK_SIZE) {
            mapMemory(i, new LazyCodeBlockMemory(AddressSpace.BLOCK_SIZE, manager));
        }
        for (int i = 0; i < 32; i++)
        {
            mapMemory(0xd0000 + i * AddressSpace.BLOCK_SIZE, new PhysicalAddressSpace.UnconnectedMemoryBlock());
        }
    }

    public void loadState(DataInput input, CodeBlockManager manager) throws IOException {
        clearArray(quickA20MaskedIndex, UNCONNECTED);
        clearArray(quickNonA20MaskedIndex, UNCONNECTED);

        linearAddr = null;
        this.manager = manager;
        initialiseMemory();
        setGateA20State(input.readBoolean());
        loadMemory(input, quickNonA20MaskedIndex, nonA20MaskedIndex);

        for (int a = 0; a < TOP_INDEX_SIZE; a++)
        {
            if (nonA20MaskedIndex[a] == null)
                continue;

            for (int b = 0; b < BOTTOM_INDEX_SIZE; b++)
                try
                {
                    a20MaskedIndex[a][b] = nonA20MaskedIndex[a][b];
                }
                catch (NullPointerException n)
                {
                    a20MaskedIndex[a] = new Memory[BOTTOM_INDEX_SIZE];
                    a20MaskedIndex[a][b] = nonA20MaskedIndex[a][b];
                }
        }

            //fill in a20 masked full array
        for (int a = 0; a < TOP_INDEX_SIZE; a++)
        {
            if (nonA20MaskedIndex[a] == null)
                continue;
           
            for (int b = 0; b < BOTTOM_INDEX_SIZE; b++)
            {
                int i = (a << BOTTOM_INDEX_BITS) | b;

                if ((i & (GATEA20_MASK >>> INDEX_SHIFT)) == i)
                {
                    int modi = i | (~GATEA20_MASK >>> INDEX_SHIFT);
                    int moda = modi >>> BOTTOM_INDEX_BITS;
                    int modb = modi & BOTTOM_INDEX_BITS;
                    try
                    {
                        a20MaskedIndex[moda][modb] = nonA20MaskedIndex[a][b];
                    } 
                    catch (NullPointerException n)
                    {
                        a20MaskedIndex[moda] = new Memory[BOTTOM_INDEX_SIZE];
                        a20MaskedIndex[moda][modb] = nonA20MaskedIndex[a][b];
                    }
                }
            }
        }

        //fill in a20 masked quick array
        System.arraycopy(quickNonA20MaskedIndex, 0, quickA20MaskedIndex, 0, quickA20MaskedIndex.length);
        for (int i = 0; i < QUICK_INDEX_SIZE; i++)
        {
            if ((i & (GATEA20_MASK >>> INDEX_SHIFT)) == i)
            {
                quickA20MaskedIndex[i] = quickNonA20MaskedIndex[i];
                int modi = i | (~GATEA20_MASK >>> INDEX_SHIFT);
                quickA20MaskedIndex[modi] = quickNonA20MaskedIndex[i];
            }
        }
    }
    
    public void loadState(DataInput in) {}

    public CodeBlockManager getCodeBlockManager()
    {
        return manager;
    }

    /**
     * Enables or disables the 20th address line.
     * <p>
     * If set to <code>true</code> then the 20th address line is enabled and memory
     * access works conventionally.  If set to <code>false</code> then the line 
     * is held low, and therefore a memory wrapping effect emulating the behaviour
     * of and original 8086 is acheived.
     * @param value status of the A20 line.
     */
    public void setGateA20State(boolean value) {
        gateA20MaskState = value;
        if (value) {
            quickIndex = quickNonA20MaskedIndex;
            index = nonA20MaskedIndex;
        } else {
            quickIndex = quickA20MaskedIndex;
            index = a20MaskedIndex;
        }

        if ((linearAddr != null) && linearAddr.isPagingEnabled()) {
            linearAddr.flush();
        }
    }

    /**
     * Returns the status of the 20th address line.<p>
     * <p>
     * A <code>true</code> return indicates the 20th address line is enabled.  A
     * <code>false</code> return indicates that the 20th address line is held low
     * to emulate an 8086 memory system.
     * @return status of the A20 line.
     */
    public boolean getGateA20State() {
        return gateA20MaskState;
    }

    protected Memory getReadMemoryBlockAt(int offset) {
        return getMemoryBlockAt(offset);
    }

    protected Memory getWriteMemoryBlockAt(int offset) {
        return getMemoryBlockAt(offset);
    }

    public int executeReal(Processor cpu, int offset) {
        return getReadMemoryBlockAt(offset).executeReal(cpu, offset & AddressSpace.BLOCK_MASK);
    }

    public int executeProtected(Processor cpu, int offset) {
        throw new IllegalStateException("Cannot execute protected mode block in physical memory");
    }

    public int executeVirtual8086(Processor cpu, int offset) {
        throw new IllegalStateException("Cannot execute protected mode block in physical memory");
    }

    protected void replaceBlocks(Memory oldBlock, Memory newBlock) {
        for (int i = 0; i < quickA20MaskedIndex.length; i++) {
            if (quickA20MaskedIndex[i] == oldBlock) {
                quickA20MaskedIndex[i] = newBlock;
            }
        }
        for (int i = 0; i < quickNonA20MaskedIndex.length; i++) {
            if (quickNonA20MaskedIndex[i] == oldBlock) {
                quickNonA20MaskedIndex[i] = newBlock;
            }
        }
        for (Memory[] subArray : a20MaskedIndex) {
            if (subArray == null) {
                continue;
            }
            for (int j = 0; j < subArray.length; j++) {
                if (subArray[j] == oldBlock) {
                    subArray[j] = newBlock;
                }
            }
        }

        for (Memory[] subArray : nonA20MaskedIndex) {
            if (subArray == null) {
                continue;
            }
            for (int j = 0; j < subArray.length; j++) {
                if (subArray[j] == oldBlock) {
                    subArray[j] = newBlock;
                }
            }
        }
    }

    private static class MapWrapper implements Memory {

        private Memory memory;
        private int baseAddress;

        MapWrapper(Memory mem, int base) {
            baseAddress = base;
            memory = mem;
        }

        public long getSize() {
            return BLOCK_SIZE;
        }

        public boolean isAllocated() {
            return memory.isAllocated();
        }

        public void clear() {
            memory.clear(baseAddress, (int) getSize());
        }

        public void clear(int start, int length) {
            if (start + length > getSize()) {
                throw new ArrayIndexOutOfBoundsException("Attempt to clear outside of memory bounds");
            }
            start = baseAddress | start;
            memory.clear(start, length);
        }

        public void copyContentsIntoArray(int offset, byte[] buffer, int off, int len) {
            offset = baseAddress | offset;
            memory.copyContentsIntoArray(offset, buffer, off, len);
        }

        public void copyArrayIntoContents(int offset, byte[] buffer, int off, int len) {
            offset = baseAddress | offset;
            memory.copyArrayIntoContents(offset, buffer, off, len);
        }

        public byte getByte(int offset) {
            offset = baseAddress | offset;
            return memory.getByte(offset);
        }

        public short getWord(int offset) {
            offset = baseAddress | offset;
            return memory.getWord(offset);
        }

        public int getDoubleWord(int offset) {
            offset = baseAddress | offset;
            return memory.getDoubleWord(offset);
        }

        public long getQuadWord(int offset) {
            offset = baseAddress | offset;
            return memory.getQuadWord(offset);
        }

        public long getLowerDoubleQuadWord(int offset) {
            offset = baseAddress | offset;
            return memory.getQuadWord(offset);
        }

        public long getUpperDoubleQuadWord(int offset) {
            offset += 8;
            offset = baseAddress | offset;
            return memory.getQuadWord(offset);
        }

        public void setByte(int offset, byte data) {
            offset = baseAddress | offset;
            memory.setByte(offset, data);
        }

        public void setWord(int offset, short data) {
            offset = baseAddress | offset;
            memory.setWord(offset, data);
        }

        public void setDoubleWord(int offset, int data) {
            offset = baseAddress | offset;
            memory.setDoubleWord(offset, data);
        }

        public void setQuadWord(int offset, long data) {
            offset = baseAddress | offset;
            memory.setQuadWord(offset, data);
        }

        public void setLowerDoubleQuadWord(int offset, long data) {
            offset = baseAddress | offset;
            memory.setQuadWord(offset, data);
        }

        public void setUpperDoubleQuadWord(int offset, long data) {
            offset += 8;
            offset = baseAddress | offset;
            memory.setQuadWord(offset, data);
        }

        public int executeReal(Processor cpu, int offset) {
            offset = baseAddress | offset;
            return memory.executeReal(cpu, offset);
        }

        public int executeProtected(Processor cpu, int offset) {
            throw new IllegalStateException("Cannot execute protected mode block in physical memory");
        }

        public int executeVirtual8086(Processor cpu, int offset) {
            throw new IllegalStateException("Cannot execute protected mode block in physical memory");
        }

        public String toString() {
            return "Mapped Memory";
        }

        public void loadInitialContents(int address, byte[] buf, int off, int len) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public void clear() {
        for (Memory block : quickNonA20MaskedIndex) {
            block.clear();
        }
        for (Memory[] subArray : nonA20MaskedIndex) {
            if (subArray == null) {
                continue;
            }
            for (Memory block : subArray) {
                try {
                    block.clear();
                } catch (NullPointerException e) {
                }
            }
        }
    }

    /**
     * Clears all mapping in the given address range.
     * <p>
     * The corresponding blocks are pointed to an unconnected memory block whose
     * data lines all float high.  If the supplied range is not of <code>BLOCK_SIZE</code>
     * granularity then an <code>IllegalStateException</code> is thrown.
     * @param start inclusive lower bound
     * @param length number of addresses to clear
     * @throws java.lang.IllegalStateException if range is not of <code>BLOCK_SIZE</code> granularity
     */
    public void unmap(int start, int length) {
        if ((start % BLOCK_SIZE) != 0) {
            throw new IllegalStateException("Cannot deallocate memory starting at " + Integer.toHexString(start) + "; this is not block aligned at " + BLOCK_SIZE + " boundaries");
        }
        if ((length % BLOCK_SIZE) != 0) {
            throw new IllegalStateException("Cannot deallocate memory in partial blocks. " + length + " is not a multiple of " + BLOCK_SIZE);
        }
        for (int i = start; i < start + length; i += BLOCK_SIZE) {
            setMemoryBlockAt(i, UNCONNECTED);
        }
    }

    /**
     * Maps the given address range to the <code>underlying</code> object.
     * <p>
     * This will throw <code>IllegalStateException</code> if either the region is
     * not of <code>BLOCK_SIZE</code> granularity, or <code>underlying</code> is
     * not as long as the specified region.
     * @param underlying memory block to be mapped.
     * @param start inclusive start address.
     * @param length size of mapped region.
     * @throws java.lang.IllegalStateException if there is an error in the mapping.
     */
    public void mapMemoryRegion(Memory underlying, int start, int length) {
        if (underlying.getSize() < length) {
            throw new IllegalStateException("Underlying memory (length=" + underlying.getSize() + ") is too short for mapping into region " + length + " bytes long");
        }
        if ((start % BLOCK_SIZE) != 0) {
            throw new IllegalStateException("Cannot map memory starting at " + Integer.toHexString(start) + "; this is not aligned to " + BLOCK_SIZE + " blocks");
        }
        if ((length % BLOCK_SIZE) != 0) {
            throw new IllegalStateException("Cannot map memory in partial blocks: " + length + " is not a multiple of " + BLOCK_SIZE);
        }
        unmap(start, length);

        long s = 0xFFFFFFFFl & start;
        for (long i = s; i < s + length; i += BLOCK_SIZE) {
            Memory w = new MapWrapper(underlying, (int) (i - s));
            setMemoryBlockAt((int) i, w);
        }
    }

    /**
     * Maps the given block into the given address.
     * <p>
     * The supplied block must be <code>BLOCK_SIZE</code> long, and the start
     * address must be <code>BLOCK_SIZE</code> granularity otherwise an
     * <code>IllegalStateException</code> is thrown.
     * @param start address for beginning of <code>block</code>.
     * @param block object to be mapped.
     * @throws java.lang.IllegalStateException if there is an error in the mapping.
     */
    public void mapMemory(int start, Memory block) {
        if ((start % BLOCK_SIZE) != 0) {
            throw new IllegalStateException("Cannot allocate memory starting at " + Integer.toHexString(start) + "; this is not aligned to " + BLOCK_SIZE + " bytes");
        }
        if (block.getSize() != BLOCK_SIZE) {
            throw new IllegalStateException("Can only allocate memory in blocks of " + BLOCK_SIZE);
        }
        unmap(start, BLOCK_SIZE);

        long s = 0xFFFFFFFFl & start;
        setMemoryBlockAt((int) s, block);
    }

    public static final class UnconnectedMemoryBlock implements Memory {

        public boolean isAllocated() {
            return false;
        }

        public void clear() {
        }

        public void clear(int start, int length) {
        }

        public void copyContentsIntoArray(int address, byte[] buffer, int off, int len) {
        }

        public void copyArrayIntoContents(int address, byte[] buffer, int off, int len) {
            throw new IllegalStateException("Cannot load array into unconnected memory block");
        }

        public long getSize() {
            return BLOCK_SIZE;
        }

        public byte getByte(int offset) {
            return (byte) -1;
        }

        public short getWord(int offset) {
            return (short) -1;
        }

        public int getDoubleWord(int offset) {
            return -1;
        }

        public long getQuadWord(int offset) {
            return -1l;
        }

        public long getLowerDoubleQuadWord(int offset) {
            return -1l;
        }

        public long getUpperDoubleQuadWord(int offset) {
            return -1l;
        }

        public void setByte(int offset, byte data) {
        }

        public void setWord(int offset, short data) {
        }

        public void setDoubleWord(int offset, int data) {
        }

        public void setQuadWord(int offset, long data) {
        }

        public void setLowerDoubleQuadWord(int offset, long data) {
        }

        public void setUpperDoubleQuadWord(int offset, long data) {
        }

        public int executeReal(Processor cpu, int offset) {
            throw new IllegalStateException("Trying to execute in Unconnected Block @ 0x" + Integer.toHexString(offset));
        }

        public int executeProtected(Processor cpu, int offset) {
            throw new IllegalStateException("Trying to execute in Unconnected Block @ 0x" + Integer.toHexString(offset));
        }

        public int executeVirtual8086(Processor cpu, int offset) {
            throw new IllegalStateException("Trying to execute in Unconnected Block @ 0x" + Integer.toHexString(offset));
        }

        public String toString() {
            return "Unconnected Memory";
        }

        public void loadInitialContents(int address, byte[] buf, int off, int len) {
        }
    }

    public void reset() {
        clear();
        setGateA20State(false);
        linearAddr = null;
    }

    public boolean updated() {
        return true;
    }

    public void updateComponent(HardwareComponent component) {
    }

    public boolean initialised() {
        return (linearAddr != null);
    }

    public void acceptComponent(HardwareComponent component) {
        if (component instanceof LinearAddressSpace) {
            linearAddr = (LinearAddressSpace) component;
        }
    }

    public String toString() {
        return "Physical Address Bus";
    }

    private Memory getMemoryBlockAt(int i) {
        try {
            return quickIndex[i >>> INDEX_SHIFT];
        } catch (ArrayIndexOutOfBoundsException e) {
            try {
                return index[i >>> TOP_INDEX_SHIFT][(i >>> BOTTOM_INDEX_SHIFT) & BOTTOM_INDEX_MASK];
            } catch (NullPointerException n) {
                return UNCONNECTED;
            }
        }
    }

    private void setMemoryBlockAt(int i, Memory b) {
        try {
            int idx = i >>> INDEX_SHIFT;
            quickNonA20MaskedIndex[idx] = b;
            if ((idx & (GATEA20_MASK >>> INDEX_SHIFT)) == idx) {
                quickA20MaskedIndex[idx] = b;
                quickA20MaskedIndex[idx | ((~GATEA20_MASK) >>> INDEX_SHIFT)] = b;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            try {
                nonA20MaskedIndex[i >>> TOP_INDEX_SHIFT][(i >>> BOTTOM_INDEX_SHIFT) & BOTTOM_INDEX_MASK] = b;
            } catch (NullPointerException n) {
                nonA20MaskedIndex[i >>> TOP_INDEX_SHIFT] = new Memory[BOTTOM_INDEX_SIZE];
                nonA20MaskedIndex[i >>> TOP_INDEX_SHIFT][(i >>> BOTTOM_INDEX_SHIFT) & BOTTOM_INDEX_MASK] = b;
            }

            if ((i & GATEA20_MASK) == i) {
                try {
                    a20MaskedIndex[i >>> TOP_INDEX_SHIFT][(i >>> BOTTOM_INDEX_SHIFT) & BOTTOM_INDEX_MASK] = b;
                } catch (NullPointerException n) {
                    a20MaskedIndex[i >>> TOP_INDEX_SHIFT] = new Memory[BOTTOM_INDEX_SIZE];
                    a20MaskedIndex[i >>> TOP_INDEX_SHIFT][(i >>> BOTTOM_INDEX_SHIFT) & BOTTOM_INDEX_MASK] = b;
                }

                int modi = i | ~GATEA20_MASK;
                try {
                    a20MaskedIndex[modi >>> TOP_INDEX_SHIFT][(modi >>> BOTTOM_INDEX_SHIFT) & BOTTOM_INDEX_MASK] = b;
                } catch (NullPointerException n) {
                    a20MaskedIndex[modi >>> TOP_INDEX_SHIFT] = new Memory[BOTTOM_INDEX_SIZE];
                    a20MaskedIndex[modi >>> TOP_INDEX_SHIFT][(modi >>> BOTTOM_INDEX_SHIFT) & BOTTOM_INDEX_MASK] = b;
                }
            }
        }
    }

    public void loadInitialContents(int address, byte[] buf, int off, int len) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
