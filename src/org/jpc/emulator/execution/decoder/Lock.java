package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.processor.*;
import org.jpc.emulator.execution.*;
import org.jpc.emulator.memory.*;

public class Lock extends Executable
{
    Executable exec;

    public Lock(int blockStart, Executable parent, Instruction in)
    {
        super(blockStart, in);
        this.exec = parent;
    }

    public Branch execute(Processor cpu)
    {
        cpu.lock(0);
        Branch b = exec.execute(cpu);
        cpu.unlock(0);
        return b;
    }

    public void configure(Instruction i)
    {}
}
