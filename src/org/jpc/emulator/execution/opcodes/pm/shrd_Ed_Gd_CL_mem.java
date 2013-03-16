package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class shrd_Ed_Gd_CL_mem extends Executable
{
    final Pointer op1;
    final int op2Index;

    public shrd_Ed_Gd_CL_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
        op2Index = Modrm.Gd(modrm);
    }

    public Branch execute(Processor cpu)
    {
        Reg op2 = cpu.regs[op2Index];
        if(cpu.r_cl.get8() != 0)
        {
            int shift = cpu.r_cl.get8() & 0x1f;
            cpu.flagOp1 = op1.get32(cpu);
            cpu.flagOp2 = shift;
            long rot = ((0xffffffffL &op2.get32()) << 32) | (0xffffffffL & op1.get32(cpu));
            cpu.flagResult = ((int)(rot >> shift));
            op1.set32(cpu, cpu.flagResult);
            cpu.flagIns = UCodes.SHRD32;
            cpu.flagStatus = OSZAPC;
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