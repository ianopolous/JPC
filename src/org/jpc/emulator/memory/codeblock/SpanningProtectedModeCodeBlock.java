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
import org.jpc.emulator.memory.AddressSpace;

/**
 * 
 * @author Chris Dennis
 */
class SpanningProtectedModeCodeBlock extends SpanningCodeBlock implements ProtectedModeCodeBlock
{
    private ByteSourceWrappedMemory byteSource = new ByteSourceWrappedMemory();

    private CodeBlockFactory[] factories;
    private int length;

    public SpanningProtectedModeCodeBlock(CodeBlockFactory[] factories)
    {
	this.factories = factories;
    }

    public int getX86Length()
    {
        return length;
    }
    
    protected CodeBlock decode(Processor cpu)
    {
	ProtectedModeCodeBlock block = null;
	AddressSpace memory = cpu.linearMemory;
	int address = cpu.getInstructionPointer();
	boolean opSize = cpu.cs.getDefaultSizeFlag();
	for (int i = 0; (i < factories.length) && (block == null); i++) {
	    try {
		byteSource.set(memory, address);
		block = factories[i].getProtectedModeCodeBlock(byteSource, opSize);
	    } catch (IllegalStateException e) {}
	}
	length = block.getX86Length();
        byteSource.set(null, 0);
	return block;
    }
    
    public String toString()
    {
	return "Spanning Protected Mode CodeBlock";
    }
}
