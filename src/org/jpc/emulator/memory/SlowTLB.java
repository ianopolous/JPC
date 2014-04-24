/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

    Copyright (C) 2012-2013 Ian Preston

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

    End of licence header
*/

package org.jpc.emulator.memory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SlowTLB extends TLB
{

    private static final byte FOUR_M = (byte) 0x01;
    private static final byte FOUR_K = (byte) 0x00;

    private final Set<Integer> nonGlobalPages;
    private Memory[] readUserIndex, readSupervisorIndex, writeUserIndex, writeSupervisorIndex, readIndex, writeIndex;
    private byte[] pageSize;
    private boolean globalPagesEnabled;

    public SlowTLB()
    {
        globalPagesEnabled = false;
        nonGlobalPages = new HashSet<Integer>();

        pageSize = new byte[AddressSpace.INDEX_SIZE];
        for (int i=0; i < AddressSpace.INDEX_SIZE; i++)
            pageSize[i] = FOUR_K;
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeInt(pageSize.length);
        output.write(pageSize);
        output.writeInt(nonGlobalPages.size());
        for (Integer value : nonGlobalPages)
            output.writeInt(value.intValue());
    }

    public void loadState(DataInput input) throws IOException
    {
        int len = input.readInt();
        pageSize = new byte[len];
        input.readFully(pageSize,0,len);
        nonGlobalPages.clear();
        int count = input.readInt();
        for (int i=0; i < count; i++)
            nonGlobalPages.add(Integer.valueOf(input.readInt()));
    }

    public void setSupervisor(boolean isSupervisor)
    {
        if (isSupervisor)
        {
            readIndex = readSupervisorIndex;
            writeIndex = writeSupervisorIndex;
        }
        else
        {
           readIndex = readUserIndex;
           writeIndex = writeUserIndex;
        }
    }

    public void setWriteProtectPages(boolean value)
    {
        if (value) {
            if (writeSupervisorIndex != null)
                for (int i = 0; i < AddressSpace.INDEX_SIZE; i++)
                    nullIndex(writeSupervisorIndex, i);
        }
    }

    private Memory[] createReadIndex(boolean isSupervisor)
    {
        if (isSupervisor)
            return (readIndex = readSupervisorIndex = new Memory[AddressSpace.INDEX_SIZE]);
        else
            return (readIndex = readUserIndex = new Memory[AddressSpace.INDEX_SIZE]);
    }

    private Memory[] createWriteIndex(boolean isSupervisor)
    {
        if (isSupervisor)
            return (writeIndex = writeSupervisorIndex = new Memory[AddressSpace.INDEX_SIZE]);
        else
            return (writeIndex = writeUserIndex = new Memory[AddressSpace.INDEX_SIZE]);
    }

    /**
     * Invalidate any entries for this address in the translation cache.
     * <p>
     * This will cause the next request for an address within the same page to
     * have to walk the translation tables in memory.
     * @param offset address within the page to be invalidated.
     */
    public void invalidateTLBEntry(int offset)
    {
        int index = offset >>> AddressSpace.INDEX_SHIFT;
        if (pageSize[index] == FOUR_K) {
            nullIndex(readSupervisorIndex, index);
            nullIndex(writeSupervisorIndex, index);
            nullIndex(readUserIndex, index);
            nullIndex(writeUserIndex, index);
            nonGlobalPages.remove(Integer.valueOf(index));
        } else {
            index &= 0xFFC00;
            for (int i = 0; i < 1024; i++, index++) {
                nullIndex(readSupervisorIndex, index);
                nullIndex(writeSupervisorIndex, index);
                nullIndex(readUserIndex, index);
                nullIndex(writeUserIndex, index);
                nonGlobalPages.remove(Integer.valueOf(index));
            }
        }
    }

    private static void nullIndex(Memory[] array, int index)
    {
        try {
            array[index] = null;
        } catch (NullPointerException e) {}
    }

    public void flush()
    {
        for (int i = 0; i < AddressSpace.INDEX_SIZE; i++)
            pageSize[i] = FOUR_K;

        nonGlobalPages.clear();

        readUserIndex = null;
        writeUserIndex = null;
        readSupervisorIndex = null;
        writeSupervisorIndex = null;
        readIndex = null;
        writeIndex = null;
    }

    public void flushNonGlobal()
    {
        if (globalPagesEnabled) {
            for (Integer value : nonGlobalPages) {
                int index = value.intValue();
                nullIndex(readSupervisorIndex, index);
                nullIndex(writeSupervisorIndex, index);
                nullIndex(readUserIndex, index);
                nullIndex(writeUserIndex, index);
                pageSize[index] = FOUR_K;
            }
            nonGlobalPages.clear();
        } else
            flush();
    }

    @Override
    public void setGlobalPages(boolean enabled) {
        globalPagesEnabled = enabled;
    }

    public void addNonGlobalPage(int addr) {
        nonGlobalPages.add(addr >>> AddressSpace.INDEX_SHIFT);
    }

    @Override
    public boolean globalPagesEnabled() {
        return globalPagesEnabled;
    }

    @Override
    public Memory getReadMemoryBlockAt(boolean isSupervisor, int addr) {
        return getReadIndexValue(isSupervisor, addr >>> AddressSpace.INDEX_SHIFT);
    }

    @Override
    public void setReadMemoryBlockAt(boolean isSupervisor, int addr, Memory value) {
        int index = addr >>> AddressSpace.INDEX_SHIFT;
        try {
            readIndex[index] = value;
        } catch (NullPointerException e) {
            createReadIndex(isSupervisor)[index] = value;
        }
    }

    @Override
    public Memory getWriteMemoryBlockAt(boolean isSupervisor, int addr) {
        return getWriteIndexValue(isSupervisor, addr >>> AddressSpace.INDEX_SHIFT);
    }

    private Memory getReadIndexValue(boolean isSupervisor, int index)
    {
        try {
            return readIndex[index];
        } catch (NullPointerException e) {
            return createReadIndex(isSupervisor)[index];
        }
    }

    @Override
    public void setWriteMemoryBlockAt(boolean isSupervisor, int addr, Memory value) {
        int index = addr >>> AddressSpace.INDEX_SHIFT;
        try {
            writeIndex[index] = value;
        } catch (NullPointerException e) {
            createWriteIndex(isSupervisor)[index] = value;
        }
    }

    private Memory getWriteIndexValue(boolean isSupervisor, int index)
    {
        try {
            return writeIndex[index];
        } catch (NullPointerException e) {
            return createWriteIndex(isSupervisor)[index];
        }
    }

    protected void setPageSize(int addr, byte type)
    {
        pageSize[addr >>> AddressSpace.INDEX_SHIFT] = type;
    }

    protected void replaceBlocks(Memory oldBlock, Memory newBlock)
    {
        try {
            for (int i = 0; i < AddressSpace.INDEX_SIZE; i++)
                if (readUserIndex[i] == oldBlock)
                    readUserIndex[i] = newBlock;
        } catch (NullPointerException e) {}

        try {
            for (int i = 0; i < AddressSpace.INDEX_SIZE; i++)
                if (writeUserIndex[i] == oldBlock)
                    writeUserIndex[i] = newBlock;
        } catch (NullPointerException e) {}

        try {
            for (int i = 0; i < AddressSpace.INDEX_SIZE; i++)
                if (readSupervisorIndex[i] == oldBlock)
                    readSupervisorIndex[i] = newBlock;
        } catch (NullPointerException e) {}

        try {
            for (int i = 0; i < AddressSpace.INDEX_SIZE; i++)
                if (writeSupervisorIndex[i] == oldBlock)
                    writeSupervisorIndex[i] = newBlock;
        } catch (NullPointerException e) {}
    }
}
