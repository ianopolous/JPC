package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class and_Gd_Ed_mem extends Executable
{
    final int op1Index;
    final Pointer op2;

    public and_Gd_Ed_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        op2 = new Pointer(parent.operand[1], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        cpu.of = cpu.af = cpu.cf = false;
        cpu.flagResult = (op1.get32() & op2.get32(cpu));
        op1.set32(cpu.flagResult);
        cpu.flagStatus = SZP;
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