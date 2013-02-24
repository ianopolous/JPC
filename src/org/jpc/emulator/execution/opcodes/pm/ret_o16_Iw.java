package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class ret_o16_Iw extends Executable
{
    final int immw;
    final int blockLength;
    final int instructionLength;

    public ret_o16_Iw(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
        instructionLength = parent.x86Length;
        immw = (short)parent.operand[0].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.eip += blockLength;
        cpu.eip = 0xFFFF & cpu.pop16();
        if (cpu.ss.getDefaultSizeFlag())
            cpu.r_esp.set32(cpu.r_esp.get32()+immw);
        else
            cpu.r_sp.set16(cpu.r_sp.get16()+immw);
        return Branch.Ret;
    }

    public boolean isBranch()
    {
        return true;
    }

    public String toString()
    {
        return this.getClass().getName();
    }
}