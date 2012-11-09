package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class ret extends Executable
{

    public ret(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        System.out.printf("Reading return address from %08x\n", cpu.r_esp.get32());
        cpu.eip = 0xFFFF&cpu.pop16();
        System.out.printf("Return to %08x\n", cpu.eip);
        return Branch.Ret;
    }

    public boolean isBranch()
    {
        return true;
    }

    public String toString()
    {
        return this.getClass().getName();
    }
}