package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class imul_Ed extends Executable
{
    final int op1Index;

    public imul_Ed(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
            cpu.flagStatus = OSZAPC;
            cpu.flagOp1 = op1.get32();
            cpu.flagOp2 = cpu.r_eax.get32();
            long res64 = (((long) cpu.flagOp1)*cpu.flagOp2);
            cpu.flagResult = (int) res64;
            cpu.r_eax.set32(cpu.flagResult);
            cpu.r_edx.set32((int)(res64 >> 32));
            cpu.flagIns = UCodes.IMUL32;
            if (res64 < 0)
                cpu.sf(true);
            else
                cpu.sf(false);
            if (res64 == cpu.flagResult)
            {
                cpu.of(false);
                cpu.cf(false);
            } else
            {
                cpu.of(true);
                cpu.cf(true);
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