package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class add_Ev_Ib_mem extends Executable
{
    final Address op1;
    final int imm;
    final int size;

    public add_Ev_Ib_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        size = parent.operand[0].size;
        op1 = new Address(parent.operand[0]);
        imm = (byte)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        if (size == 16)
        {
        cpu.flagOp1 = op1.get16(cpu);
        cpu.flagOp2 = imm;
        cpu.flagResult = (short)(cpu.flagOp1 + cpu.flagOp2);
        op1.set16(cpu, (short)cpu.flagResult);
        cpu.flagIns = UCodes.ADD16;
        cpu.flagStatus = OSZAPC;
        }
        else if (size == 32)
        {
        cpu.flagOp1 = op1.get32(cpu);
        cpu.flagOp2 = imm;
        cpu.flagResult = (cpu.flagOp1 + cpu.flagOp2);
        op1.set32(cpu, cpu.flagResult);
        cpu.flagIns = UCodes.ADD32;
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