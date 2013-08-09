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

public class fucomip_ST0_ST6 extends Executable
{

    public fucomip_ST0_ST6(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(0);
        double freg1 = cpu.fpu.ST(6);
        if (freg0 > freg1)
        {
            cpu.zf = cpu.pf = cpu.cf = false;
            cpu.flagStatus &= ~(ZF | PF | CF);
        } else if (freg0 < freg1)
        {
            cpu.zf = cpu.pf = false;
            cpu.cf = true;
            cpu.flagStatus &= ~(ZF | PF | CF);
        } else if (freg0 == freg1)
        {
            cpu.cf = cpu.pf = false;
            cpu.zf = true;
            cpu.sf = false;
            cpu.flagStatus &= ~(ZF | PF | CF | SF);
        } else
        {
            cpu.zf = cpu.pf = cpu.cf = true;
            cpu.flagStatus &= ~(ZF | PF | CF);
        }
        cpu.af = false;
        cpu.sf = false;
        cpu.flagStatus &= ~(AF | SF);
        cpu.fpu.pop();
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