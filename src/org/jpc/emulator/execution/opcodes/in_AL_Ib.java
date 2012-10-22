package org.jpc.emulator.execution.opcodes;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class in_AL_Ib extends Executable
{
    final int size;
    final int op1Index;
    final int imm;

    public in_AL_Ib(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.operand[0].size;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        imm = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        Reg op2 = cpu.regs[op1Index];
        op2.set8(cpu.ioports.ioPortReadByte(imm));
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