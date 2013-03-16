package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fucomip_ST0_ST2 extends Executable
{

    public fucomip_ST0_ST2(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(0);
        double freg1 = cpu.fpu.ST(2);
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