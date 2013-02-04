package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class dec_Ew_mem extends Executable
{
    final Pointer op1;

    public dec_Ew_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        cpu.cf = cpu.cf();
        cpu.flagOp1 = op1.get16(cpu);
        cpu.flagOp2 = 1;
        cpu.flagResult = (short)(cpu.flagOp1 - 1);
        op1.set16(cpu, (short)cpu.flagResult);
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