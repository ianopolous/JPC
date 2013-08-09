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

public class frndint extends Executable
{

    public frndint(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(0);
        if (!Double.isInfinite(freg0)) // preserve infinities
        {
            switch(cpu.fpu.getRoundingControl())
            {
                case FpuState.FPU_ROUNDING_CONTROL_EVEN:
                    cpu.fpu.setST(0, Math.rint(freg0));
                    break;
                case FpuState.FPU_ROUNDING_CONTROL_DOWN:
                    cpu.fpu.setST(0, Math.floor(freg0));
                    break;
                case FpuState.FPU_ROUNDING_CONTROL_UP:
                    cpu.fpu.setST(0, Math.ceil(freg0));
                    break;
                case FpuState.FPU_ROUNDING_CONTROL_TRUNCATE:
                    cpu.fpu.setST(0, Math.signum(freg0) * Math.floor(Math.abs(freg0)));
                    break;
                default:
                    throw new IllegalStateException("Invalid rounding control value");
            }
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