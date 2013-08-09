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

package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class popfd extends Executable
{

    public popfd(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        if (cpu.getCPL() == 0)
			cpu.setEFlags(((cpu.getEFlags() & 0x20000) | (cpu.pop32() & ~(0x20000 | 0x180000))));
		    else {
			if (cpu.getCPL() > cpu.eflagsIOPrivilegeLevel)
			    cpu.setEFlags(((cpu.getEFlags() & 0x23200) | (cpu.pop32() & ~(0x23200 | 0x180000))));
			else
			    cpu.setEFlags(((cpu.getEFlags() & 0x23000) | (cpu.pop32() & ~(0x23000 | 0x180000))));
		    }
        return Branch.None;
    }

    public boolean isBranch()
    {
        return false;
    }

    public String toString()
    {
        return this.getClass().getName();
    }
}