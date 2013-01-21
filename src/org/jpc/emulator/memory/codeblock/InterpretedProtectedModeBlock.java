package org.jpc.emulator.memory.codeblock;

import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.execution.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.execution.Executable.*;

public class InterpretedProtectedModeBlock extends BasicBlock implements ProtectedModeCodeBlock
{
    public InterpretedProtectedModeBlock(Executable start, int x86Length, int x86Count)
    {
        super(start, x86Length, x86Count);
    }

    public InterpretedProtectedModeBlock(BasicBlock b)
    {
        this(b.start, b.x86Length, b.x86Count);
    }

    public Branch execute(Processor cpu)
    {
        Executable current = start;
        Executable.Branch ret;

        preBlock(cpu);
        try
        {
            while ((ret = current.execute(cpu)) == Executable.Branch.None)
            {
                postInstruction(cpu, current);
                current = current.next;
            }
            postInstruction(cpu, current);
            return ret;
        } catch (ProcessorException e)
        {
            cpu.eip += current.delta;
            if (!e.pointsToSelf())
                cpu.eip += current.x86Length;

            if (e.getType() != ProcessorException.Type.PAGE_FAULT)
            {
                /*System.out.println("cs selector = " + Integer.toHexString(cpu.cs.getSelector())
                        + ", cs base = " + Integer.toHexString(cpu.cs.getBase()) + ", EIP = "
                        + Integer.toHexString(cpu.eip));*/
            }
            cpu.handleProtectedModeException(e);
            return Branch.Exception;
        }
    }
}
