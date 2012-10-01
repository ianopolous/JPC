package org.jpc.emulator.execution.opcodes;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_Gv_Ev_mem extends Executable
{
    final int op1Index;
    final Address op2;
    final int size;

    public mov_Gv_Ev_mem(Instruction parent)
    {
        super(parent);
        size = parent.operand[1].size;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
        op2 = new Address(parent.operand[1]);
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];

        if (size == 16)
        {
            op1.set16(memory.getWord(op2.get(cpu)));
        }
        else if (size == 32)
        {
            op1.set32(memory.getDoubleWord(op2.get(cpu)));
        }
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