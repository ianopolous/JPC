package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class xchg_o32_rSPr12_rAX extends Executable
{

    public xchg_o32_rSPr12_rAX(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }


    public xchg_o32_rSPr12_rAX(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
            int tmp = cpu.r_eax.get32();
        cpu.r_eax.set32(cpu.r_esp.get32());
        cpu.r_esp.set32(tmp);
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