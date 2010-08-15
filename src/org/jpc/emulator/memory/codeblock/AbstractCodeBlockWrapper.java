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
 * Abstract implementation of <code>CodeBlock</code> that wraps another CodeBlock
 * implementation.
 * @author Chris Dennis
 */
public abstract class AbstractCodeBlockWrapper implements CodeBlock
{
    private volatile CodeBlock actualBlock;
    
    /**
     * Constructs a wrapper around <code>target</code>.
     * @param target <code>CodeBlock</code> instance being wrapped.
     */
    public AbstractCodeBlockWrapper(CodeBlock target)
    {
        actualBlock = target;
    }

    public final int getX86Length()
    {
	return actualBlock.getX86Length();
    }
    
    public final int getX86Count()
    {
	return actualBlock.getX86Count();
    }
    
    public int execute(Processor cpu)
    {
	return actualBlock.execute(cpu);
    }
    
    public String getDisplayString()
    {
        return "WRAP " + actualBlock.getDisplayString();
    }
    
    public boolean handleMemoryRegionChange(int startAddress, int endAddress)
    {
        return actualBlock.handleMemoryRegionChange(startAddress, endAddress);
    }

    /**
     * Sets the target block of this wrapper.
     * @param block new target block instance
     */
    public final void setTargetBlock(CodeBlock block)
    {
	actualBlock = block;
    }
    
    /**
     * Gets a reference to the target block of this wrapper.
     * @return target block of this wrapper
     */
    public final CodeBlock getTargetBlock()
    {
	return actualBlock;
    }
}
