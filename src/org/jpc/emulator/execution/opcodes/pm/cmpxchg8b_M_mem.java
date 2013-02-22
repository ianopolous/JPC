package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class cmpxchg8b_M_mem extends Executable
{
    final Address op1;

    public cmpxchg8b_M_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Address(parent.operand[0], parent.adr_mode);
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