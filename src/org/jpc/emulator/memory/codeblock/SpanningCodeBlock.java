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

package org.jpc.emulator.memory.codeblock;

import org.jpc.emulator.processor.Processor;

/**
 * Abstract <code>CodeBlock</code> instance that never caches a decode result.
 * This is necessary for blocks that span memory boundaries.
 * @author Chris Dennis
 */
public abstract class SpanningCodeBlock implements CodeBlock
{
    private CodeBlock lastBlock;

    public int getX86Length()
    {
        return 0;
    }

    public int getX86Count()
    {
	try {
	    return lastBlock.getX86Count();
	} catch (NullPointerException e) {
	    return 0;
	}
    }
    
    public int execute(Processor cpu)
    {
	lastBlock = decode(cpu);
	return lastBlock.execute(cpu);
    }

    /**
     * Forces a new decode on the current memory state.
     * @param cpu processor state on which we are about to execute
     * @return fresh <code>CodeBlock</code> instance
     */
    protected abstract CodeBlock decode(Processor cpu);

    /**
     * Indicates whether this block can handle a change in the memory contents
     * over the given range.  Spanning blocks always return <code>true</code>
     * here as they never cache decode results.
     * @param startAddress offset of first byte in range (inclusive)
     * @param endAddress offset of last byte in range (exclusive)
     * @return whether code block has been invalidated
     */
    public boolean handleMemoryRegionChange(int startAddress, int endAddress)
    {
        return true;
    }

    public String getDisplayString()
    {
        if (lastBlock != null)
            return lastBlock.getDisplayString();
        else
            return "Undecoded Spanning Block";
    }
}
