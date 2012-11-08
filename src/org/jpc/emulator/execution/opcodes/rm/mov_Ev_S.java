package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_Ev_S extends Executable
{
    String seg;
    final int op1Index;
    final int size;

    public mov_Ev_S(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        seg = parent.operand[1].toString();
        size = parent.operand[0].size;
        op1Index = Processor.getRegIndex(parent.operand[0].toString());
    }

    public Branch execute(Processor cpu)
    {
        Reg op1 = cpu.regs[op1Index];

        if (seg.equals("ds"))
        {
            op1.set16(cpu.ds());
        }
        else if (seg.equals("es"))
        {
            op1.set16(cpu.es());
        }
        else if (seg.equals("ss"))
        {
            op1.set16(cpu.ss());
        }
        else if (seg.equals("cs"))
        {
            op1.set16(cpu.cs());
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