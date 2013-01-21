package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class xchg_o16_rBXr11_rAX extends Executable
{

    public xchg_o16_rBXr11_rAX(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
            int tmp = cpu.r_eax.get16();
        cpu.r_eax.set16(cpu.r_ebx.get16());
        cpu.r_ebx.set16((short)tmp);
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