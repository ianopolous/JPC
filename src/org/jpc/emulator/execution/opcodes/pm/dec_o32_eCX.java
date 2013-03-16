package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class dec_o32_eCX extends Executable
{

    public dec_o32_eCX(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }


    public dec_o32_eCX(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
    }

    public Branch execute(Processor cpu)
    {
        cpu.cf = Processor.getCarryFlag(cpu.flagStatus, cpu.cf, cpu.flagOp1, cpu.flagOp2, cpu.flagResult, cpu.flagIns);
        cpu.flagOp1 = cpu.r_ecx.get32();
        cpu.flagOp2 = 1;
        cpu.flagResult = (cpu.flagOp1 - 1);
        cpu.r_ecx.set32(cpu.flagResult);
        cpu.flagIns = UCodes.SUB32;
        cpu.flagStatus = NCF;
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