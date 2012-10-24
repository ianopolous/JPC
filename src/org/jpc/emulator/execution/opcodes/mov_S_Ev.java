package org.jpc.emulator.execution.opcodes;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_S_Ev extends Executable
{
    String seg;
    final int op2Index;
    final int size;

    public mov_S_Ev(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        seg = parent.operand[0].toString();
        size = parent.operand[1].size;
        op2Index = Processor.getRegIndex(parent.operand[1].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op2 = cpu.regs[op2Index];

        if (seg.equals("ds"))
        {
            cpu.ds((short)op2.get16());
        }
        else if (seg.equals("es"))
        {
            cpu.es((short)op2.get16());
        }
        else if (seg.equals("ss"))
        {
            cpu.ss((short)op2.get16());
        }
        else throw new IllegalStateException(seg.toString()+" - Unknown segment load");
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