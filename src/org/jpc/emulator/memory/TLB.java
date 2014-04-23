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

public abstract class TLB
{
    public abstract void saveState(DataOutput output) throws IOException;
    public abstract void loadState(DataInput input) throws IOException;

    public abstract void setSupervisor(boolean isSupervisor);
    public abstract void setWriteProtectPages(boolean value);

    public abstract void flush();
    public abstract void flushNonGlobal();

    public abstract void setGlobalPages(boolean enabled);
    public abstract void addNonGlobalPage(int addr);
    public abstract boolean globalPagesEnabled();

    public abstract Memory getReadMemoryBlockAt(boolean isSupervisor, int addr);
    public abstract void setReadMemoryBlockAt(boolean isSupervisor, int addr, Memory m);

    public abstract Memory getWriteMemoryBlockAt(boolean isSupervisor, int addr);
    public abstract void setWriteMemoryBlockAt(boolean isSupervisor, int addr, Memory m);

    protected abstract void setPageSize(int addr, byte type);
    protected abstract void replaceBlocks(Memory oldBlock, Memory newBlock);

    /**
     * Invalidate any entries for this address in the translation cache.
     * <p>
     * This will cause the next request for an address within the same page to
     * have to walk the translation tables in memory.
     * @param addr address within the page to be invalidated.
     */
    public abstract void invalidateTLBEntry(int addr);
}
