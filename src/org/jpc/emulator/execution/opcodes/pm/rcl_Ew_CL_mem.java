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

public class rcl_Ew_CL_mem extends Executable
{
    final Pointer op1;

    public rcl_Ew_CL_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
            int shift = cpu.r_cl.get8() & 0x1f;
            shift %= 16+1;
            long val = 0xFFFF&op1.get16(cpu);
            val |= cpu.cf() ? 1L << 16 : 0;
            val = (val << shift) | (val >>> (16+1-shift));
            op1.set16(cpu, (short)(int)val);
            boolean bit31 = (val & (1L << (16-1))) != 0;
            boolean bit32 = (val & (1L << (16))) != 0;
            cpu.cf(bit32);
            if (shift == 1)
                cpu.of(bit31 ^ bit32);
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