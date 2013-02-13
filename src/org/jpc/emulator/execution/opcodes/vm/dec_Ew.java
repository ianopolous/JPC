package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class dec_Ew extends Executable
{
    final int op1Index;

    public dec_Ew(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        cpu.cf = cpu.cf();
        cpu.flagOp1 = op1.get16();
        cpu.flagOp2 = 1;
        cpu.flagResult = (short)(cpu.flagOp1 - 1);
        op1.set16((short)cpu.flagResult);
        cpu.flagIns = UCodes.SUB16;
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