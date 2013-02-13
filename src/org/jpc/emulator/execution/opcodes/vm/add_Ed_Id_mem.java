package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class add_Ed_Id_mem extends Executable
{
    final Pointer op1;
    final int immd;

    public add_Ed_Id_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
        immd = (int)parent.operand[1].lval;
    }

    public Branch execute(Processor cpu)
    {
        cpu.flagOp1 = op1.get32(cpu);
        cpu.flagOp2 = immd;
        cpu.flagResult = (cpu.flagOp1 + cpu.flagOp2);
        op1.set32(cpu, cpu.flagResult);
        cpu.flagIns = UCodes.ADD32;
        cpu.flagStatus = OSZAPC;
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