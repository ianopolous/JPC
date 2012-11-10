package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class mov_Ob_AL extends Executable
{
    final Address op1;

    public mov_Ob_AL(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Address(parent.operand[0]);
    }

    public Branch execute(Processor cpu)
    {
        op1.set8(cpu, (byte)cpu.r_al.get8());
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