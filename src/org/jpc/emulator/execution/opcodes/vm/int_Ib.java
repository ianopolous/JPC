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

package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class int_Ib extends Executable
{
    final int immb;
    final int blockLength;
    final int instructionLength;

    public int_Ib(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        immb = Modrm.Ib(input);
        instructionLength = (int)input.getAddress()-eip;
        blockLength = eip-blockStart+instructionLength;
    }

    public Branch execute(Processor cpu)
    {
        cpu.eip += blockLength;
        if ((cpu.getCR4() & Processor.CR4_VIRTUAL8086_MODE_EXTENSIONS) != 0)
	    throw new IllegalStateException();
	if (cpu.eflagsIOPrivilegeLevel < 3)
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);
        }
	cpu.handleSoftVirtual8086ModeInterrupt(0xFF&immb, instructionLength);
        return Branch.Jmp_Unknown;
    }

    public boolean isBranch()
    {
        return true;
    }

    public String toString()
    {
        return this.getClass().getName();
    }
}