package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class cmpxchg_Ed_Gd_mem extends Executable
{
    final Pointer op1;
    final int op2Index;

    public cmpxchg_Ed_Gd_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
        op2Index = Modrm.Gd(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op2 = cpu.regs[op2Index];
        cpu.flagOp1 = cpu.r_eax.get32();
        cpu.flagOp2 = op1.get32(cpu);
        cpu.flagResult = (cpu.flagOp1 - cpu.flagOp2);
        cpu.flagIns = UCodes.SUB32;
        cpu.flagStatus = OSZAPC;
        if (cpu.flagOp1 == cpu.flagOp2)
        {
            cpu.zf(true);
            op1.set32(cpu, op2.get32());
        }
        else
        {
            cpu.zf(false);
            cpu.r_eax.set32(op1.get32(cpu));
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