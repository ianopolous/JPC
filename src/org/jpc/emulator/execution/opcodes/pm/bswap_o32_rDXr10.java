package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class bswap_o32_rDXr10 extends Executable
{

    public bswap_o32_rDXr10(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }


    public bswap_o32_rDXr10(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        cpu.r_edx.set32(Integer.reverseBytes(cpu.r_edx.get32()));
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