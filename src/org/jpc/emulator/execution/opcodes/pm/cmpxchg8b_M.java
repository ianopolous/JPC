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

public class cmpxchg8b_M extends Executable
{
    final Pointer op1;

    public cmpxchg8b_M(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
        long val1 = cpu.r_edx.get32()& 0xffffffffL;
        val1 = val1 << 32;
        val1 |= (0xffffffffL & cpu.r_eax.get32());
        long val2 = cpu.linearMemory.getQuadWord(op1.get(cpu));
        if (val1 == val2)
        {
            cpu.zf(true);
            long res = cpu.r_ecx.get32()& 0xffffffffL;
            res = res << 32;
            res |= (0xffffffffL & cpu.r_ebx.get32());
            cpu.linearMemory.setQuadWord(op1.get(cpu), res);
        }
        else
        {
            cpu.zf(false);
            cpu.r_eax.set32((int)val2);
            cpu.r_edx.set32((int)(val2 >> 32));
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