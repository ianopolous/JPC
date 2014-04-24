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

public class FastTLB extends TLB
{
    private static final byte FOUR_M = (byte) 0x01;
    private static final byte FOUR_K = (byte) 0x00;

    private static final int TLB_SIZE = 1024;
    private static final int TLB_MASK = (TLB_SIZE-1) << 12;
    private static int TLBIndexOf(int addr) {
        return (addr & TLB_MASK) >>> 12;
    }

    private final Set<Integer> nonGlobalPages = new HashSet<Integer>();
    private TLB_Entry[] cache = new TLB_Entry[TLB_SIZE];
    private boolean split_large = false;
    private boolean globalPagesEnabled;
    private byte[] pageSize;

    public FastTLB()
    {
        pageSize = new byte[AddressSpace.INDEX_SIZE];
        for (int i=0; i < AddressSpace.INDEX_SIZE; i++)
            pageSize[i] = FOUR_K;
    }

    @Override
    public void saveState(DataOutput output) throws IOException {
        output.writeInt(pageSize.length);
        output.write(pageSize);
        output.writeInt(nonGlobalPages.size());
        for (Integer value : nonGlobalPages)
            output.writeInt(value.intValue());
    }

    @Override
    public void loadState(DataInput input) throws IOException {
        int len = input.readInt();
        pageSize = new byte[len];
        input.readFully(pageSize,0,len);
        nonGlobalPages.clear();
        int count = input.readInt();
        for (int i=0; i < count; i++)
            nonGlobalPages.add(Integer.valueOf(input.readInt()));
    }

    @Override
    public void setSupervisor(boolean isSupervisor) {

    }

    @Override
    public void setWriteProtectPages(boolean value) {

    }

    @Override
    public void flush() {
        cache = new TLB_Entry[TLB_SIZE];
    }

    @Override
    public void flushNonGlobal() {
        if (globalPagesEnabled) {
            for (Integer value : nonGlobalPages) {
                int page = value.intValue();
                int index = TLBIndexOf(page << AddressSpace.INDEX_SHIFT);
                if ((cache[index] == null) || (!cache[index].samePage(page)))
                    continue;
                cache[index] = null;
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

    @Override
    public void addNonGlobalPage(int addr) {
        nonGlobalPages.add(addr >>> AddressSpace.INDEX_SHIFT);
    }

    @Override
    public boolean globalPagesEnabled() {
        return globalPagesEnabled;
    }

    @Override
    public Memory getReadMemoryBlockAt(boolean isSupervisor, int addr) {
        TLB_Entry entry = cache[TLBIndexOf(addr)];
        if ((entry == null) || !entry.samePage(addr) || !entry.isRead(isSupervisor))
            return null;
        return  entry.m;
    }

    @Override
    public void setReadMemoryBlockAt(boolean isSupervisor, int addr, Memory m) {
        int index = TLBIndexOf(addr);
        if ((cache[index] == null) || !cache[index].samePage(addr))
            cache[index] = new TLB_Entry(m, addr & AddressSpace.INDEX_MASK, TLB_Entry.getAccess(isSupervisor, true, false, true));
        else
            cache[index].accessBits |= TLB_Entry.getAccess(isSupervisor, true, false, true);
    }

    @Override
    public Memory getWriteMemoryBlockAt(boolean isSupervisor, int addr) {
        TLB_Entry entry = cache[TLBIndexOf(addr)];
        if ((entry == null) || !entry.samePage(addr) || !entry.isWrite(isSupervisor))
            return null;
        return  entry.m;
    }

    @Override
    public void setWriteMemoryBlockAt(boolean isSupervisor, int addr, Memory m) {
        int index = TLBIndexOf(addr);
        if ((cache[index] == null) || !cache[index].samePage(addr))
            cache[index] = new TLB_Entry(m, addr & AddressSpace.INDEX_MASK, TLB_Entry.getAccess(isSupervisor, false, true, false));
        else
            cache[index].accessBits |= TLB_Entry.getAccess(isSupervisor, false, true, false);
    }

    @Override
    protected void setPageSize(int addr, byte type) {
        pageSize[addr >>> AddressSpace.INDEX_SHIFT] = type;
    }

    @Override
    protected void replaceBlocks(Memory oldBlock, Memory newBlock) {
        System.out.println("Replace blocks used!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }

    @Override
    public void invalidateTLBEntry(int addr) {
        int index = addr >>> AddressSpace.INDEX_SHIFT;
        if (pageSize[index] == FOUR_K) {
            nonGlobalPages.remove(Integer.valueOf(index));
            int page = TLBIndexOf(addr);
            if ((cache[page] == null) || (!cache[page].samePage(addr)))
                return;
            cache[page] = null;
        } else {
            index &= 0xFFC00;
            for (int i = 0; i < 1024; i++, index++) {
                nonGlobalPages.remove(Integer.valueOf(index));
                int page = TLBIndexOf(index << AddressSpace.INDEX_SHIFT);
                if ((cache[page] == null) || (!cache[page].samePage(addr)))
                    continue;
                cache[page] = null;
            }
        }
    }

    private static class TLB_Entry
    {
        private static int GLOBAL_PAGE   = 0x80000000;
        private static int SysReadOK     = 0x01;
        private static int UserReadOK    = 0x02;
        private static int SysWriteOK    = 0x04;
        private static int UserWriteOK   = 0x08;
        private static int SysExecuteOK  = 0x10;
        private static int UserExecuteOK = 0x20;

        private static final boolean[] allowed = new boolean[32];
        static {
            for (int i=0; i < 32; i++)
                allowed[i] = (0xff0bbb0b & (1 << i)) != 0;
        }

        private static int getAccess(boolean isSupervisor, boolean readable, boolean writable, boolean executable)
        {
            if (isSupervisor)
                return (readable ? SysReadOK : 0) | (writable ? SysWriteOK : 0) | (executable ? SysExecuteOK : 0);
            else
                return (readable ? SysReadOK | UserReadOK : 0) | (writable ? SysWriteOK | UserWriteOK : 0) | (executable ? SysExecuteOK | UserExecuteOK : 0);
        }

        int linearAddress;
        Memory m;
        int accessBits;
        int linearMask;

        private TLB_Entry(Memory m, int linearAddress, int accessBits)
        {
            this.linearAddress = linearAddress;
            this.m = m;
            this.accessBits = accessBits;
            linearMask = ~0xfff;
        }

        public boolean isRead(boolean isSupervisor)
        {
            if (isSupervisor)
                return (accessBits & SysReadOK) != 0;
            return (accessBits & UserReadOK) != 0;
        }

        public boolean isWrite(boolean isSupervisor)
        {
            if (isSupervisor)
                return (accessBits & SysWriteOK) != 0;
            return (accessBits & UserWriteOK) != 0;
        }

        public boolean samePage(int addr)
        {
            return (linearAddress & linearMask) == (addr & linearMask);
        }
    }
}
