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

import org.jpc.emulator.processor.Processor;
import org.jpc.emulator.memory.AddressSpace;

class SpanningProtectedModeCodeBlock extends SpanningCodeBlock implements ProtectedModeCodeBlock
{
    private PeekableMemoryStream byteSourceStream = new PeekableMemoryStream();

    private CodeBlockFactory[] factories;
    private int length;
    private int decodes = 0;

    public SpanningProtectedModeCodeBlock(CodeBlockFactory[] factories)
    {
	    this.factories = factories;
    }

    public int getX86Length()
    {
        return length;
    }
    
    public CodeBlock decode(Processor cpu)
    {
        decodes++;
        ProtectedModeCodeBlock block = null;
        AddressSpace memory = cpu.linearMemory;
        int address = cpu.getInstructionPointer();
        boolean opSize = cpu.cs.getDefaultSizeFlag();
        for (int i = 0; (i < factories.length) && (block == null); i++) {
            try {
                byteSourceStream.set(memory, address);
                block = factories[i].getProtectedModeCodeBlock(byteSourceStream, opSize);
            } catch (IllegalStateException e) {e.printStackTrace();}
        }
        length = block.getX86Length();
        if (decodes % 1000 == 0)
            System.out.printf("PM Spanning block at %08x of length %d decoded %d times.\n", cpu.getInstructionPointer(), length, decodes);
        byteSourceStream.set(null, 0);
        return block;
    }
    
    public String toString()
    {
        return "Spanning Protected Mode CodeBlock";
    }
}
