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

import java.io.*;
import java.util.logging.*;

import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.execution.codeblock.*;
import org.jpc.emulator.processor.*;

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

    private boolean isSupervisor, pagingDisabled, pageCacheEnabled, writeProtectPages, pageSizeExtensions;
    private int baseAddress, lastAddress;
    private PhysicalAddressSpace target;
    private final FastTLB tlb;

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
        writeProtectPages = false;
        pageSizeExtensions = false;
//        tlb = new SlowTLB();
        tlb = new FastTLB();
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeBoolean(isSupervisor);
        output.writeBoolean(tlb.globalPagesEnabled());
        output.writeBoolean(pagingDisabled);
        output.writeBoolean(pageCacheEnabled);
        output.writeBoolean(writeProtectPages);
        output.writeBoolean(pageSizeExtensions);
        output.writeInt(baseAddress);
        output.writeInt(lastAddress);
        tlb.saveState(output);
    }

    public void loadState(DataInput input) throws IOException
    {
        reset();
        isSupervisor  = input.readBoolean();
        tlb.setGlobalPages(input.readBoolean());
        pagingDisabled  = input.readBoolean();
        pageCacheEnabled  = input.readBoolean();
        writeProtectPages = input.readBoolean();
        pageSizeExtensions  = input.readBoolean();
        baseAddress = input.readInt();
        lastAddress = input.readInt();
        tlb.loadState(input);
        setSupervisor(isSupervisor);
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
        tlb.setSupervisor(value);
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
        tlb.flush();
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
        tlb.flush();
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
        if (tlb.globalPagesEnabled() == value)
            return;

        tlb.setGlobalPages(value);
        tlb.flush();
    }

    /**
     * Enables the write-protection of user pages for supervisor code.
     * <p>
     * When set to <code>false</code> supervisor code can write to write
     * protected user pages, which is not allowed if this option is enabled.
     * @param value <code>true</code> to prevent writing to RO user pages.
     */
    public void setWriteProtectPages(boolean value)
    {
        tlb.setWriteProtectPages(value);
        writeProtectPages = value;
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
        tlb.flushNonGlobal();
    }

    public void flush()
    {
        tlb.flush();
    }

    public void invalidateTLBEntry(int offset)
    {
        tlb.invalidateTLBEntry(offset);
    }

    private Memory validateTLBEntryRead(int offset)
    {
        if (pagingDisabled)
        {
            tlb.setReadMemoryBlockAt(isSupervisor, offset, target.getReadMemoryBlockAt(offset));
            return tlb.getReadMemoryBlockAt(isSupervisor, offset);
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

        boolean directoryGlobal = tlb.globalPagesEnabled() && ((0x100 & directoryRawBits) != 0);
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

            int mapAddress = 0xFFC00000 & offset;
            for (int i=0; i<1024; i++,fourMegPageStartAddress += BLOCK_SIZE, mapAddress += BLOCK_SIZE)
            {
                Memory m = target.getReadMemoryBlockAt(fourMegPageStartAddress);
                tlb.setPageSize(mapAddress, FOUR_M);
                tlb.setReadMemoryBlockAt(isSupervisor, mapAddress, m);
                if (directoryGlobal)
                    continue;

                tlb.addNonGlobalPage(mapAddress);
            }

            return tlb.getReadMemoryBlockAt(isSupervisor, offset);
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

            boolean tableGlobal = tlb.globalPagesEnabled() && ((0x100 & tableRawBits) != 0);
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

            if (!tableGlobal)
                tlb.addNonGlobalPage(offset);

            tlb.setReadMemoryBlockAt(isSupervisor, offset, target.getReadMemoryBlockAt(fourKStartAddress));
            tlb.setPageSize(offset, FOUR_K);
            return tlb.getReadMemoryBlockAt(isSupervisor, offset);
	}
    }

    private Memory validateTLBEntryWrite(int offset)
    {
        if (pagingDisabled)
        {
            tlb.setWriteMemoryBlockAt(isSupervisor, offset, target.getWriteMemoryBlockAt(offset));
            return tlb.getWriteMemoryBlockAt(isSupervisor, offset);
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

        boolean directoryGlobal = tlb.globalPagesEnabled() && ((0x100 & directoryRawBits) != 0);
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
                        if (writeProtectPages)
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

            int mapAddress = 0xFFC00000 & offset;
            for (int i=0; i<1024; i++, fourMegPageStartAddress += BLOCK_SIZE, mapAddress += BLOCK_SIZE)
            {
                Memory m = target.getWriteMemoryBlockAt(fourMegPageStartAddress);
                tlb.setPageSize(mapAddress, FOUR_M);
                tlb.setWriteMemoryBlockAt(isSupervisor, mapAddress, m);

                if (directoryGlobal)
                    continue;

                tlb.addNonGlobalPage(mapAddress);
            }
            
            return tlb.getWriteMemoryBlockAt(isSupervisor, offset);
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

	    boolean tableGlobal = tlb.globalPagesEnabled() && ((0x100 & tableRawBits) != 0);
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
                        if (writeProtectPages)
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
                    {
                        if (writeProtectPages)
                            return PF_PROTECTION_VIOLATION_WS;
                    }
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


            if (!tableGlobal)
                tlb.addNonGlobalPage(offset);

            tlb.setWriteMemoryBlockAt(isSupervisor, offset, target.getWriteMemoryBlockAt(fourKStartAddress));
            tlb.setPageSize(offset, FOUR_K);
            return tlb.getWriteMemoryBlockAt(isSupervisor, offset);
        }
    }

    protected Memory getReadMemoryBlockAt(int offset)
    {
        return tlb.getReadMemoryBlockAt(isSupervisor, offset);
    }

    protected Memory getWriteMemoryBlockAt(int offset)
    {
        return tlb.getWriteMemoryBlockAt(isSupervisor, offset);
    }

    /**
     * Calls replace block on the underlying <code>PhysicalAddressSpace</code>
     * object.
     * @param oldBlock block to be replaced.
     * @param newBlock new block to be added.
     */
    protected void replaceBlocks(Memory oldBlock, Memory newBlock)
    {
        tlb.replaceBlocks(oldBlock, newBlock);
    }

    public byte getByte(int offset)
    {        
        try 
        {
            return getReadMemoryBlockAt(offset).getByte(offset & BLOCK_MASK);
        } 
        catch (NullPointerException e) {}
        catch (ProcessorException p) {}

        return validateTLBEntryRead(offset).getByte(offset & BLOCK_MASK);
    }

    public short getWord(int offset)
    { 
        try 
        {
            try
            {
                return getReadMemoryBlockAt(offset).getWord(offset & BLOCK_MASK);
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                return super.getWord(offset);
            }
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
            try
            {
                return getReadMemoryBlockAt(offset).getDoubleWord(offset & BLOCK_MASK);
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                return super.getDoubleWord(offset);
            }
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
            getWriteMemoryBlockAt(offset).setByte(offset & BLOCK_MASK, data);
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
            try
            {
                getWriteMemoryBlockAt(offset).setWord(offset & BLOCK_MASK, data);
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                super.setWord(offset, data);
            }
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
            try
            {
                getWriteMemoryBlockAt(offset).setDoubleWord(offset & BLOCK_MASK, data);
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                super.setDoubleWord(offset, data);
            }
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
        } catch (SpanningDecodeException e)
        {
            SpanningCodeBlock block = e.getBlock();
            int length = block.decode(cpu).getX86Length();
            // add block to subsequent page to allow invalidation upon a write
            try {
                getReadMemoryBlockAt(offset+0x1000).addSpanningBlock(block, length-(0x1000-(offset & AddressSpace.BLOCK_MASK)));
            } catch (NullPointerException n) { // had to map subsequent page
                Memory page = validateTLBEntryRead(offset+0x1000);
                page.addSpanningBlock(block, length-(0x1000-(offset & AddressSpace.BLOCK_MASK)));
            }
        }

        try {
            return memory.executeProtected(cpu, offset & AddressSpace.BLOCK_MASK);
        } catch (ProcessorException p) {
            cpu.handleProtectedModeException(p);
            return 1;
        } catch (SpanningDecodeException e)
        {
            SpanningCodeBlock block = e.getBlock();
            int length = block.decode(cpu).getX86Length();
            // add block to subsequent page to allow invalidation upon a write
            try {
                getReadMemoryBlockAt(offset+0x1000).addSpanningBlock(block, length-(0x1000-(offset & AddressSpace.BLOCK_MASK)));
            } catch (NullPointerException n) { // had to map subsequent page
                Memory page = validateTLBEntryRead(offset+0x1000);
                page.addSpanningBlock(block, length-(0x1000-(offset & AddressSpace.BLOCK_MASK)));
            }
            return memory.executeProtected(cpu, offset & AddressSpace.BLOCK_MASK);
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
        } catch (SpanningDecodeException e)
        {
            SpanningCodeBlock block = e.getBlock();
            int length = block.decode(cpu).getX86Length();
            // add block to subsequent page to allow invalidation upon a write
            try {
                getReadMemoryBlockAt(offset+0x1000).addSpanningBlock(block, length-(0x1000-(offset & AddressSpace.BLOCK_MASK)));
            } catch (NullPointerException n) { // had to map subsequent page
                Memory page = validateTLBEntryRead(offset+0x1000);
                page.addSpanningBlock(block, length-(0x1000-(offset & AddressSpace.BLOCK_MASK)));
            }
        }

        try {
            return memory.executeVirtual8086(cpu, offset & AddressSpace.BLOCK_MASK);
        } catch (ProcessorException p) {
            cpu.handleProtectedModeException(p);
            return 1;
        } catch (SpanningDecodeException e)
        {
            SpanningCodeBlock block = e.getBlock();
            int length = block.decode(cpu).getX86Length();
            // add block to subsequent page to allow invalidation upon a write
            try {
                getReadMemoryBlockAt(offset+0x1000).addSpanningBlock(block, length-(0x1000-(offset & AddressSpace.BLOCK_MASK)));
            } catch (NullPointerException n) { // had to map subsequent page
                Memory page = validateTLBEntryRead(offset+0x1000);
                page.addSpanningBlock(block, length-(0x1000-(offset & AddressSpace.BLOCK_MASK)));
            }
            return memory.executeVirtual8086(cpu, offset & AddressSpace.BLOCK_MASK);
        }
    }

    public static final class PageFaultWrapper implements Memory
    {
        private final ProcessorException pageFault;

        private PageFaultWrapper(int errorCode)
        {
            pageFault = new ProcessorException(ProcessorException.Type.PAGE_FAULT, errorCode, true);
        }

        public void lock(int addr) {}

        public void unlock(int addr) {}

        public void addSpanningBlock(SpanningCodeBlock span, int lengthRemaining) {}

        public ProcessorException getException() 
        {
            return pageFault;
        }
        
        private void fill()
        {
            pageFault.fillInStackTrace();
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
        tlb.flush();

        baseAddress = 0;
        lastAddress = 0;
        pagingDisabled = true;
        tlb.setGlobalPages(false);
        writeProtectPages = false;
        pageSizeExtensions = false;
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
        return "Linear Pointer Space";
    }

    public void loadInitialContents(int address, byte[] buf, int off, int len) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

