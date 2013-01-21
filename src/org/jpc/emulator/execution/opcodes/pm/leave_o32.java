package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class leave_o32 extends Executable
{

    public leave_o32(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        if (cpu.ss.getDefaultSizeFlag())
        {
            cpu.r_esp.set32(cpu.r_ebp.get32());
            cpu.r_ebp.set32(cpu.pop32());
        } else
        {
            cpu.r_sp.set16(cpu.r_bp.get16());
            cpu.r_ebp.set32(cpu.pop32());
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