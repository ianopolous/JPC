package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class setl_Eb extends Executable
{
    final int op1Index;

    public setl_Eb(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        op1.set8((byte)((cpu.sf() != cpu.of()) ? 1 : 0));
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