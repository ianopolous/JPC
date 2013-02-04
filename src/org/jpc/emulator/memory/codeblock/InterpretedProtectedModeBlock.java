package org.jpc.emulator.memory.codeblock;

import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.execution.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.execution.Executable.*;

public class InterpretedProtectedModeBlock implements ProtectedModeCodeBlock
{
    private final BasicBlock b;

    public InterpretedProtectedModeBlock(BasicBlock b)
    {
        this.b = b;
    }

    public int getX86Length() {
        return b.getX86Length();
    }

    public int getX86Count() {
        return b.getX86Count();
    }

    public Branch execute(Processor cpu)
    {
        Executable current = b.start;
        Executable.Branch ret;

        b.preBlock(cpu);
        try
        {
            while ((ret = current.execute(cpu)) == Executable.Branch.None)
            {
                b.postInstruction(cpu, current);
                current = current.next;
            }
            b.postInstruction(cpu, current);
            return ret;
        } catch (ProcessorException e)
        {
            if (current.next != null) // branches have already updated eip
                cpu.eip += current.delta;
            else
                cpu.eip -= current.x86Length; // so eip points at the branch that barfed
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

    public String getDisplayString() {
        return "Interpreted Protected Mode Block:\n"+b.getDisplayString();
    }

    public Instruction getInstructions() {
        return b.getInstructions();
    }

    public boolean handleMemoryRegionChange(int startAddress, int endAddress) {
        return b.handleMemoryRegionChange(startAddress, endAddress);
    }
}
