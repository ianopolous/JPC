package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class neg_Ev_mem extends Executable
{
    final Pointer op1;
    final int size;

    public neg_Ev_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.opr_mode;
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        if (size == 16)
        {
            cpu.flagOp1 = op1.get16(cpu);
            cpu.flagResult = -(short)(cpu.flagOp1);
            op1.set16(cpu, (short)cpu.flagResult);
            cpu.flagIns = UCodes.NEG16;
            cpu.flagStatus = OSZAPC;
        }
        else if (size == 32)
        {
            cpu.flagOp1 = op1.get32(cpu);
            cpu.flagResult = -(cpu.flagOp1);
            op1.set32(cpu, cpu.flagResult);
            cpu.flagIns = UCodes.NEG32;
            cpu.flagStatus = OSZAPC;
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