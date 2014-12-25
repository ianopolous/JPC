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

package org.jpc.emulator.execution.codeblock;

import org.jpc.emulator.execution.Executable;
import org.jpc.emulator.execution.decoder.Instruction;
import org.jpc.emulator.processor.Processor;

/**
 * Abstract <code>CodeBlock</code> instance that spans a page boundary and thus needs to be careful
 * about caching the decode result.
 * @author Ian Preston
 */
public abstract class SpanningCodeBlock implements CodeBlock
{
    private CodeBlock lastBlock = null;

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
    
    public Executable.Branch execute(Processor cpu)
    {
        if (lastBlock == null)
            lastBlock = decode(cpu);
        return lastBlock.execute(cpu);
    }

    public void invalidate()
    {
        lastBlock = null;
    }

    /**
     * Forces a new decode on the current memory state.
     * @param cpu processor state on which we are about to execute
     * @return fresh <code>CodeBlock</code> instance
     */
    public abstract CodeBlock decode(Processor cpu);

    public boolean handleMemoryRegionChange(int startAddress, int endAddress)
    {
        return false;
    }

    public String getDisplayString()
    {
        if (lastBlock != null)
            return lastBlock.getDisplayString();
        else
            return "Undecoded Spanning Block";
    }

    public Instruction getInstructions()
    {
        if (lastBlock != null)
            return lastBlock.getInstructions();
        return null;
    }
}
