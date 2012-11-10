package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class lea_Gv_M extends Executable
{
    final int op1Index;
    final Address op2;
    final int size;

    public lea_Gv_M(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.operand[0].size;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        op2 = new Address(parent.operand[1]);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];
        if (size == 16)
        {
        op1.set16(op2.get(cpu));
        }
        else if (size == 32)
        {
        op1.set32(op2.get(cpu));
        }        else throw new IllegalStateException("Unknown size "+size);
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