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
import java.util.*;
import java.util.logging.*;

import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.processor.*;

/**
 * Class that implements the paging system used by an x86 MMU when in protected
 * mode.
 * @author Rhys Newman
 * @author Chris Dennis
 */
public final class LinearAddressSpace extends AddressSpace implements HardwareComponent
{
    private static final Logger LOGGING = Logger.getLogger(LinearAddressSpace.class.getName());
    
    private static final PageFaultWrapper PF_NOT_PRESENT_RU = new PageFaultWrapper(4);
    private static final PageFaultWrapper PF_NOT_PRESENT_RS = new PageFaultWrapper(0);
    private static final PageFaultWrapper PF_NOT_PRESENT_WU = new PageFaultWrapper(6);
    private static final PageFaultWrapper PF_NOT_PRESENT_WS = new PageFaultWrapper(2);

    private static final PageFaultWrapper PF_PROTECTION_VIOLATION_RU = new PageFaultWrapper(5);
    private static final PageFaultWrapper PF_PROTECTION_VIOLATION_WU = new PageFaultWrapper(7);
    private static final PageFaultWrapper PF_PROTECTION_VIOLATION_WS = new PageFaultWrapper(3);

    private static final byte FOUR_M = (byte) 0x01;
    private static final byte FOUR_K = (byte) 0x00;

    private boolean isSupervisor, globalPagesEnabled, pagingDisabled, pageCacheEnabled, writeProtectUserPages, pageSizeExtensions;
    private int baseAddress, lastAddress;
    private PhysicalAddressSpace target;

    private byte[] pageSize;
    private final Set<Integer> nonGlobalPages;
    private Memory[] readUserIndex, readSupervisorIndex, writeUserIndex, writeSupervisorIndex, readIndex, writeIndex;

    /**
     * Constructs a <code>LinearAddressSpace</code> with paging initially disabled
     * and a <code>PhysicalAddressSpace<code> that is defined during component
     * configuration.
     */
    public LinearAddressSpace()
    {
        baseAddress = 0;
        lastAddress = 0;
        pagingDisabled = true;
        globalPagesEnabled = false;
        writeProtectUserPages = false;
        pageSizeExtensions = false;

        nonGlobalPages = new HashSet<Integer>();
        
        pageSize = new byte[INDEX_SIZE];
        for (int i=0; i < INDEX_SIZE; i++)
            pageSize[i] = FOUR_K;
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeBoolean(isSupervisor);
        output.writeBoolean(globalPagesEnabled);
        output.writeBoolean(pagingDisabled);
        output.writeBoolean(pageCacheEnabled);
        output.writeBoolean(writeProtectUserPages);
        output.writeBoolean(pageSizeExtensions);
        output.writeInt(baseAddress);
        output.writeInt(lastAddress);
        output.writeInt(pageSize.length);
        output.write(pageSize);
        output.writeInt(nonGlobalPages.size());
        for (Integer value : nonGlobalPages) 
            output.writeInt(value.intValue());
    }

    public void loadState(DataInput input) throws IOException
    {
        reset();
        isSupervisor  = input.readBoolean();
        globalPagesEnabled  = input.readBoolean();
        pagingDisabled  = input.readBoolean();
        pageCacheEnabled  = input.readBoolean();
        writeProtectUserPages  = input.readBoolean();
        pageSizeExtensions  = input.readBoolean();
        baseAddress = input.readInt();
        lastAddress = input.readInt();
        int len = input.readInt();
        pageSize = new byte[len];
        input.readFully(pageSize,0,len);
        nonGlobalPages.clear();
        int count = input.readInt();
        for (int i=0; i < count; i++)
            nonGlobalPages.add(Integer.valueOf(input.readInt()));
        setSupervisor(isSupervisor);
    }

    private Memory[] createReadIndex()
    {
	if (isSupervisor)
	    return (readIndex = readSupervisorIndex = new Memory[INDEX_SIZE]);
	else
	    return (readIndex = readUserIndex = new Memory[INDEX_SIZE]);
    }

    private Memory[] createWriteIndex()
    {
	if (isSupervisor)
	    return (writeIndex = writeSupervisorIndex = new Memory[INDEX_SIZE]);
	else
	    return (writeIndex = writeUserIndex = new Memory[INDEX_SIZE]);
    }
    
    private void setReadIndexValue(int index, Memory value)
    {
	try {
	    readIndex[index] = value;
	} catch (NullPointerException e) {
	    createReadIndex()[index] = value;
	}
    }

    private Memory getReadIndexValue(int index)
    {
	try {
	    return readIndex[index];
	} catch (NullPointerException e) {
	    return createReadIndex()[index];
	}
    }

    private void setWriteIndexValue(int index, Memory value)
    {
	try {
	    writeIndex[index] = value;
	} catch (NullPointerException e) {
	    createWriteIndex()[index] = value;
	}
    }

    private Memory getWriteIndexValue(int index)
    {
	try {
	    return writeIndex[index];
	} catch (NullPointerException e) {
	    return createWriteIndex()[index];
	}
    }

    /**
     * Returns the linear address translated by this instance.  This is used 
     * by the processor during the handling of a page fault.
     * @return the last translated address.
     */
    public int getLastWalkedAddress()
    {
        return lastAddress;
    }

    /**
     * Returns <code>true</code> if the address space if in supervisor-mode which
     * is when the processor is at a CPL of zero.
     * @return <code>true</code> if in supervisor-mode.
     */
    public boolean isSupervisor()
    {
        return isSupervisor;
    }

    /**
     * Set the address space to either supervisor or user mode.
     * <p>
     * This is used when the processor transition into or out of a CPL of zero,
     * or when accessing system segments that always perform accesses using
     * supervisor mode.
     * @param value <code>true</code> for supervisor, <code>false</code> for user mode.
     */
    public void setSupervisor(boolean value)
    {
        isSupervisor = value;
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

    /**
     * Returns the state of the paging system.
     * @return <code>true</code> is paging is enabled.
     */
    public boolean isPagingEnabled()
    {
        return !pagingDisabled;
    }

    /**
     * Enables or disables paging.
     * @param value <code>true</code> to enable paging.
     */
    public void setPagingEnabled(boolean value)
    {
        if (value && !target.getGateA20State())
            LOGGING.log(Level.WARNING, "Paging enabled with A20 masked");

        pagingDisabled = !value;
        flush();
    }

    /**
     * Returns <code>true</code> if the address-space is caching page translations.
     * <p>
     * This enables or disables the emulated equivalent of the TLBs (Translation
     * Look-aside Buffers).
     * @param value <code>true</code> to enable translation caching.
     */
    public void setPageCacheEnabled(boolean value)
    {
        pageCacheEnabled = value;
    }

    /**
     * Enables the use of large (4MB) pages.
     * @param value <code>true</code> to enable 4MB pages.
     */
    public void setPageSizeExtensionsEnabled(boolean value)
    {
        pageSizeExtensions = value;
        flush();
    }

    /**
     * Not as yet implemented
     * @param value
     */
    public void setPageWriteThroughEnabled(boolean value)
    {
        //System.err.println("ERR: Write Through Caching enabled for TLBs");        
    }

    /**
     * Enables the use of global pages (which are harder to flush).
     * <p>
     * Global page cache entries are not flushed on a task switch.  Therefore
     * they are commonly used for system pages (e.g. Linux kernel pages).
     * @param value <code>true</code> to enable to use of global pages.
     */
    public void setGlobalPagesEnabled(boolean value)
    {
        if (globalPagesEnabled == value)
            return;

        globalPagesEnabled = value;
        flush();
    }

    /**
     * Enables the write-protection of user pages for supervisor code.
     * <p>
     * When set to <code>false</code> supervisor code can write to write
     * protected user pages, which is not allowed if this option is enabled.
     * @param value <code>true</code> to prevent writing to RO user pages.
     */
    public void setWriteProtectUserPages(boolean value)
    {
	if (value) {
            if (writeSupervisorIndex != null)
                for (int i = 0; i < INDEX_SIZE; i++)
                    nullIndex(writeSupervisorIndex, i);
	}
	    
        writeProtectUserPages = value;
    }

    /**
     * Clears the entire translation cache.
     * <p>
     * This includes both non-global and global pages.
     */
    public void flush()
    {
        for (int i = 0; i < INDEX_SIZE; i++)
            pageSize[i] = FOUR_K;

        nonGlobalPages.clear();

	readUserIndex = null;
	writeUserIndex = null;
	readSupervisorIndex = null;
	writeSupervisorIndex = null;
    }

    private void partialFlush()
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

    private static void nullIndex(Memory[] array, int index)
    {        
	try {
	    array[index] = null;
	} catch (NullPointerException e) {}
    }

    /**
     * Changes the base address of the translation tables and flushes the
     * translation cache.
     * <p>
     * This is executed in response to a task switch, as the new task will have
     * its own set of page translation entries.  The flush performed here is
     * only partial and will leave any global entries intact if global pages are
     * enabled.
     * @param address new base address of the paging system.
     */
    public void setPageDirectoryBaseAddress(int address)
    {
        baseAddress = address & 0xFFFFF000;
        partialFlush();
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
        int index = offset >>> INDEX_SHIFT;
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

    private Memory validateTLBEntryRead(int offset)
    {
        int idx = offset >>> INDEX_SHIFT;
        if (pagingDisabled)
        {
	    setReadIndexValue(idx, target.getReadMemoryBlockAt(offset));
            return readIndex[idx];
        }

        lastAddress = offset;

        int directoryAddress = baseAddress | (0xFFC & (offset >>> 20)); // This should be (offset >>> 22) << 2.
        int directoryRawBits = target.getDoubleWord(directoryAddress); 

        boolean directoryPresent = (0x1 & directoryRawBits) != 0;
        if (!directoryPresent) 
        {
            if (isSupervisor)
                return PF_NOT_PRESENT_RS;
            else
                return PF_NOT_PRESENT_RU;
        }

	boolean directoryGlobal = globalPagesEnabled && ((0x100 & directoryRawBits) != 0);
//        boolean directoryReadWrite = (0x2 & directoryRawBits) != 0;
        boolean directoryUser = (0x4 & directoryRawBits) != 0;
        boolean directoryIs4MegPage = ((0x80 & directoryRawBits) != 0) && pageSizeExtensions;

        if (directoryIs4MegPage) {
            if (!directoryUser && !isSupervisor)
                return PF_PROTECTION_VIOLATION_RU;

            if ((directoryRawBits & 0x20) == 0)
            {
                directoryRawBits |= 0x20;
                target.setDoubleWord(directoryAddress, directoryRawBits);
            }

            int fourMegPageStartAddress = 0xFFC00000 & directoryRawBits;

            if (!pageCacheEnabled)
                return target.getReadMemoryBlockAt(fourMegPageStartAddress | (offset & 0x3FFFFF));

	    int tableIndex = (0xFFC00000 & offset) >>> 12; 
            for (int i=0; i<1024; i++)
            {
                Memory m = target.getReadMemoryBlockAt(fourMegPageStartAddress);
                fourMegPageStartAddress += BLOCK_SIZE;
                pageSize[tableIndex] = FOUR_M;
		setReadIndexValue(tableIndex++, m);
		if (directoryGlobal)
                    continue;

                nonGlobalPages.add(Integer.valueOf(i));
            }

            return readIndex[idx];
        }
        else 
        {
	    int directoryBaseAddress = directoryRawBits & 0xFFFFF000;
//	    boolean directoryPageLevelWriteThrough = (0x8 & directoryRawBits) != 0; 
//	    boolean directoryPageCacheDisable = (0x10 & directoryRawBits) != 0; 
//	    boolean directoryDirty = (0x40 & directoryRawBits) != 0;

            int tableAddress = directoryBaseAddress | ((offset >>> 10) & 0xFFC);
            int tableRawBits = target.getDoubleWord(tableAddress); 
        
            boolean tablePresent = (0x1 & tableRawBits) != 0;
            if (!tablePresent)
            {
                if (isSupervisor)
                    return PF_NOT_PRESENT_RS;
                else
                    return PF_NOT_PRESENT_RU;
            }

	    boolean tableGlobal = globalPagesEnabled && ((0x100 & tableRawBits) != 0);
//            boolean tableReadWrite = (0x2 & tableRawBits) != 0;
            boolean tableUser = (0x4 & tableRawBits) != 0;
            
            boolean pageIsUser = tableUser && directoryUser;
//            boolean pageIsReadWrite = tableReadWrite || directoryReadWrite;
//            if (pageIsUser)
//                pageIsReadWrite = tableReadWrite && directoryReadWrite;
            
            if (!pageIsUser && !isSupervisor)
                return PF_PROTECTION_VIOLATION_RU;

            if ((tableRawBits & 0x20) == 0) {
                tableRawBits |= 0x20;
                target.setDoubleWord(tableAddress, tableRawBits);
            }

            int fourKStartAddress = tableRawBits & 0xFFFFF000;
            if (!pageCacheEnabled)
                return target.getReadMemoryBlockAt(fourKStartAddress);

            pageSize[idx] = FOUR_K;
            if (!tableGlobal)
                nonGlobalPages.add(Integer.valueOf(idx));
            
	    setReadIndexValue(idx, target.getReadMemoryBlockAt(fourKStartAddress));
            return readIndex[idx];
	}
    }

    private Memory validateTLBEntryWrite(int offset)
    {
        int idx = offset >>> INDEX_SHIFT;
        if (pagingDisabled)
        {
	    setWriteIndexValue(idx, target.getWriteMemoryBlockAt(offset));
            return writeIndex[idx];
        }

        lastAddress = offset;

        int directoryAddress = baseAddress | (0xFFC & (offset >>> 20)); // This should be (offset >>> 22) << 2.
        int directoryRawBits = target.getDoubleWord(directoryAddress); 

        boolean directoryPresent = (0x1 & directoryRawBits) != 0;
        if (!directoryPresent) 
        {
            if (isSupervisor)
                return PF_NOT_PRESENT_WS;
            else
                return PF_NOT_PRESENT_WU;
        }

	boolean directoryGlobal = globalPagesEnabled && ((0x100 & directoryRawBits) != 0);
        boolean directoryReadWrite = (0x2 & directoryRawBits) != 0;
        boolean directoryUser = (0x4 & directoryRawBits) != 0;
        boolean directoryIs4MegPage = ((0x80 & directoryRawBits) != 0) && pageSizeExtensions;

        if (directoryIs4MegPage)
        {
            if (directoryUser)
            {
                if (!directoryReadWrite) // if readWrite then all access is OK
                {
                    if (isSupervisor)
                    {
                        if (writeProtectUserPages)
                            return PF_PROTECTION_VIOLATION_WS;
                    }
                    else
                        return PF_PROTECTION_VIOLATION_WU;
                }
            }
            else // A supervisor page
            {
                if (directoryReadWrite) 
                {
                    if (!isSupervisor)
                        return PF_PROTECTION_VIOLATION_WU;
                }
                else
                {
                    if (isSupervisor)
                        return PF_PROTECTION_VIOLATION_WS;
                    else
                        return PF_PROTECTION_VIOLATION_WU;
                }
            }

            if ((directoryRawBits & 0x60) != 0x60)
            {
                directoryRawBits |= 0x60;
                target.setDoubleWord(directoryAddress, directoryRawBits);
            }

            int fourMegPageStartAddress = 0xFFC00000 & directoryRawBits;

            if (!pageCacheEnabled)
                return target.getWriteMemoryBlockAt(fourMegPageStartAddress | (offset & 0x3FFFFF));

	    int tableIndex = (0xFFC00000 & offset) >>> 12; 
            for (int i=0; i<1024; i++)
            {
                Memory m = target.getWriteMemoryBlockAt(fourMegPageStartAddress);
                fourMegPageStartAddress += BLOCK_SIZE;
                pageSize[tableIndex] = FOUR_M;
		setWriteIndexValue(tableIndex++, m);
 
                if (directoryGlobal)
                    continue;

                nonGlobalPages.add(Integer.valueOf(i));
            }
            
            return writeIndex[idx];
        }
        else 
        {
	    int directoryBaseAddress = directoryRawBits & 0xFFFFF000;
//	    boolean directoryPageLevelWriteThrough = (0x8 & directoryRawBits) != 0; 
//	    boolean directoryPageCacheDisable = (0x10 & directoryRawBits) != 0; 
//	    boolean directoryDirty = (0x40 & directoryRawBits) != 0;

            int tableAddress = directoryBaseAddress | ((offset >>> 10) & 0xFFC);
            int tableRawBits = target.getDoubleWord(tableAddress); 
        
            boolean tablePresent = (0x1 & tableRawBits) != 0;
            if (!tablePresent)
            {
                if (isSupervisor)
                    return PF_NOT_PRESENT_WS;
                else
                    return PF_NOT_PRESENT_WU;
            }

	    boolean tableGlobal = globalPagesEnabled && ((0x100 & tableRawBits) != 0);
            boolean tableReadWrite = (0x2 & tableRawBits) != 0;
            boolean tableUser = (0x4 & tableRawBits) != 0;
            
            boolean pageIsUser = tableUser && directoryUser;
            boolean pageIsReadWrite = tableReadWrite || directoryReadWrite;
            if (pageIsUser)
                pageIsReadWrite = tableReadWrite && directoryReadWrite;
            
            if (pageIsUser)
            {
                if (!pageIsReadWrite) // if readWrite then all access is OK
                {
                    if (isSupervisor)
                    {
                        if (writeProtectUserPages)
                            return PF_PROTECTION_VIOLATION_WS;
                    }
                    else
                        return PF_PROTECTION_VIOLATION_WU;
                }
            }
            else // A supervisor page
            {
                if (pageIsReadWrite) 
                {
                    if (!isSupervisor)
                        return PF_PROTECTION_VIOLATION_WU;
                }
                else
                {
                    if (isSupervisor)
                        return PF_PROTECTION_VIOLATION_WS;
                    else
                        return PF_PROTECTION_VIOLATION_WU;
                }
            }

            if ((tableRawBits & 0x60) != 0x60)
            {
                tableRawBits |= 0x60;
                target.setDoubleWord(tableAddress, tableRawBits);
            }

            int fourKStartAddress = tableRawBits & 0xFFFFF000;
            if (!pageCacheEnabled)
                return target.getWriteMemoryBlockAt(fourKStartAddress);

            pageSize[idx] = FOUR_K;

            if (!tableGlobal)
                nonGlobalPages.add(Integer.valueOf(idx));
            
	    setWriteIndexValue(idx, target.getWriteMemoryBlockAt(fourKStartAddress));
            return writeIndex[idx];
	}
    }

    protected Memory getReadMemoryBlockAt(int offset)
    {
	return getReadIndexValue(offset >>> INDEX_SHIFT);
    }

    protected Memory getWriteMemoryBlockAt(int offset)
    {
	return getWriteIndexValue(offset >>> INDEX_SHIFT);
    }

    /**
     * Calls replace block on the underlying <code>PhysicalAddressSpace</code>
     * object.
     * @param oldBlock block to be replaced.
     * @param newBlock new block to be added.
     */
    protected void replaceBlocks(Memory oldBlock, Memory newBlock)
    {
	try {
	    for (int i = 0; i < INDEX_SIZE; i++)
		if (readUserIndex[i] == oldBlock)
		    readUserIndex[i] = newBlock;
	} catch (NullPointerException e) {}

	try {
	    for (int i = 0; i < INDEX_SIZE; i++)
		if (writeUserIndex[i] == oldBlock)
		    writeUserIndex[i] = newBlock;
	} catch (NullPointerException e) {}

	try {
	    for (int i = 0; i < INDEX_SIZE; i++)
		if (readSupervisorIndex[i] == oldBlock)
		    readSupervisorIndex[i] = newBlock;
	} catch (NullPointerException e) {}

	try {
	    for (int i = 0; i < INDEX_SIZE; i++)
		if (writeSupervisorIndex[i] == oldBlock)
		    writeSupervisorIndex[i] = newBlock;
	} catch (NullPointerException e) {}
    }

    public byte getByte(int offset)
    {        
        try 
        {
            return super.getByte(offset);
        } 
        catch (NullPointerException e) {}
        catch (ProcessorException p) {}

        return validateTLBEntryRead(offset).getByte(offset & BLOCK_MASK);
    }

    public short getWord(int offset)
    { 
        try 
        {
            return super.getWord(offset);
        } 
        catch (NullPointerException e) {}
        catch (ProcessorException p) {}

        Memory m = validateTLBEntryRead(offset);
        try
        {
            return m.getWord(offset & BLOCK_MASK);
        } 
        catch (ArrayIndexOutOfBoundsException e) 
        {
            return getWordInBytes(offset);
        }
    }

    public int getDoubleWord(int offset)
    {
        try 
        {
            return super.getDoubleWord(offset);
        } 
        catch (NullPointerException e) {}
        catch (ProcessorException p) {}

        Memory m = validateTLBEntryRead(offset);
        try
        {
            return m.getDoubleWord(offset & BLOCK_MASK);
        } 
        catch (ArrayIndexOutOfBoundsException e) 
        {
            return getDoubleWordInBytes(offset);
        }
    }

    public void setByte(int offset, byte data)
    {        
        try 
        {
            super.setByte(offset, data);
            return;
        } 
        catch (NullPointerException e) {}
        catch (ProcessorException p) {}

        validateTLBEntryWrite(offset).setByte(offset & BLOCK_MASK, data);
    }

    public void setWord(int offset, short data)
    { 
        try 
        {
            super.setWord(offset, data);
            return;
        } 
        catch (NullPointerException e) {}
        catch (ProcessorException p) {}

        Memory m = validateTLBEntryWrite(offset);
        try
        {
            m.setWord(offset & BLOCK_MASK, data);
        } 
        catch (ArrayIndexOutOfBoundsException e) 
        {
            setWordInBytes(offset, data);
        }
    }

    public void setDoubleWord(int offset, int data)
    {
        try 
        {
            super.setDoubleWord(offset, data);
            return;
        } 
        catch (NullPointerException e) {}
        catch (ProcessorException p) {}

        Memory m = validateTLBEntryWrite(offset);
        try
        {
            m.setDoubleWord(offset & BLOCK_MASK, data);
        } 
        catch (ArrayIndexOutOfBoundsException e) 
        {
            setDoubleWordInBytes(offset, data);
        }
    }

    /**
     * Clears the underlying <code>PhysicalAddressSpace of this object.</code>
     */
    public void clear()
    {
        target.clear();
    }

    public int executeReal(Processor cpu, int offset)
    {
        throw new IllegalStateException("Cannot execute a Real Mode block in linear memory");
    }

    public int executeProtected(Processor cpu, int offset)
    {
	Memory memory = getReadMemoryBlockAt(offset);

	try {
	    return memory.executeProtected(cpu, offset & AddressSpace.BLOCK_MASK);
	} catch (NullPointerException n) {
	    memory = validateTLBEntryRead(offset); //memory object was null (needs mapping)
	} catch (ProcessorException p) {
	    memory = validateTLBEntryRead(offset); //memory object caused a page fault (double check)
	}

	try {
	    return memory.executeProtected(cpu, offset & AddressSpace.BLOCK_MASK);
	} catch (ProcessorException p) {
	    cpu.handleProtectedModeException(p);
	    return 1;
	} catch (IllegalStateException e) {
            System.out.println("Current eip = " + Integer.toHexString(cpu.eip));
            throw e;
        }
    }

    public int executeVirtual8086(Processor cpu, int offset)
    {
	Memory memory = getReadMemoryBlockAt(offset);

	try {
	    return memory.executeVirtual8086(cpu, offset & AddressSpace.BLOCK_MASK);
	} catch (NullPointerException n) {
	    memory = validateTLBEntryRead(offset); //memory object was null (needs mapping)
	} catch (ProcessorException p) {
	    memory = validateTLBEntryRead(offset); //memory object caused a page fault (double check)
	}

	try {
	    return memory.executeVirtual8086(cpu, offset & AddressSpace.BLOCK_MASK);
	} catch (ProcessorException p) {
	    cpu.handleProtectedModeException(p);
	    return 1;
	}
    }

    public static final class PageFaultWrapper implements Memory
    {
        private final ProcessorException pageFault;

        private PageFaultWrapper(int errorCode)
        {
            pageFault = new ProcessorException(ProcessorException.Type.PAGE_FAULT, errorCode, true);
        }

        public ProcessorException getException() 
        {
            return pageFault;
        }
        
        private void fill()
        {
            //pageFault.fillInStackTrace();
        }

        public boolean isAllocated()
        {
            return false;
        }
        
        public void clear() {}

        public void clear(int start, int length) {}

        public void copyContentsIntoArray(int address, byte[] buffer, int off, int len)
        {
            fill();
            throw pageFault;
        }

        public void copyArrayIntoContents(int address, byte[] buffer, int off, int len)
        {
            fill();
            throw pageFault;
        }

        public long getSize()
        {
            return 0;
        }

        public byte getByte(int offset)
        {
            fill();
            throw pageFault;
        }

        public short getWord(int offset)
        {
            fill();
            throw pageFault;
        }

        public int getDoubleWord(int offset)
        {
            fill();
            throw pageFault;
        }

        public long getQuadWord(int offset)
        {
            fill();
            throw pageFault;
        }

        public long getLowerDoubleQuadWord(int offset)
        {
            fill();
            throw pageFault;
        }

        public long getUpperDoubleQuadWord(int offset)
        {
            fill();
            throw pageFault;
        }

        public void setByte(int offset, byte data)
        {
            fill();
            throw pageFault;
        }

        public void setWord(int offset, short data)
        {
            fill();
            throw pageFault;
        }

        public void setDoubleWord(int offset, int data)
        {
            fill();
            throw pageFault;
        }

        public void setQuadWord(int offset, long data)
        {
            fill();
            throw pageFault;
        }

        public void setLowerDoubleQuadWord(int offset, long data)
        {
            fill();
            throw pageFault;
        }

        public void setUpperDoubleQuadWord(int offset, long data)
        {
            fill();
            throw pageFault;
        }

        public int executeReal(Processor cpu, int offset)
        {
            throw new IllegalStateException("Cannot execute a Real Mode block in linear memory");
        }

	public int executeProtected(Processor cpu, int offset)
	{
	    fill();
	    throw pageFault;
	}

        public int executeVirtual8086(Processor cpu, int offset)
	{
	    fill();
	    throw pageFault;
	}
        
        public String toString()
        {
            return "PF " + pageFault;
        }

        public void loadInitialContents(int address, byte[] buf, int off, int len) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public void reset()
    {
	flush();

        baseAddress = 0;
        lastAddress = 0;
        pagingDisabled = true;
        globalPagesEnabled = false;
        writeProtectUserPages = false;
        pageSizeExtensions = false;

	readUserIndex = null;
	writeUserIndex = null;
	readSupervisorIndex = null;
	writeSupervisorIndex = null;
    }

    public boolean updated()
    {
        return target.updated();
    }

    public void updateComponent(HardwareComponent component)
    { }
    
    public boolean initialised()
    {
        return (target != null);
    }

    public void acceptComponent(HardwareComponent component)
    {
	if (component instanceof PhysicalAddressSpace)
	    target = (PhysicalAddressSpace) component;
    }

    public String toString()
    {
        return "Linear Address Space";
    }

    public void loadInitialContents(int address, byte[] buf, int off, int len) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

