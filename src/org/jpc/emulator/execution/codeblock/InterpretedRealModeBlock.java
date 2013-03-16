package org.jpc.emulator.execution.codeblock;

import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.execution.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.execution.Executable.*;

public class InterpretedRealModeBlock implements RealModeCodeBlock
{
    private final BasicBlock b;

    public InterpretedRealModeBlock(BasicBlock b)
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
            cpu.eip += current.delta;
            if (current.next == null) // branches have already updated eip
                cpu.eip -= getX86Length(); // so eip points at the branch that barfed
            if (!e.pointsToSelf())
            {
                if (current.next == null)
                    cpu.eip += getX86Length() - current.delta;
                else
                    cpu.eip += current.next.delta - current.delta;
            }

            cpu.handleRealModeException(e);
            return Branch.Exception;
        }
    }

    public String getDisplayString() {
        return "Interpreted Real Mode Block:\n"+b.getDisplayString();
    }

    public Instruction getInstructions() {
        return b.getInstructions();
    }

    public boolean handleMemoryRegionChange(int startAddress, int endAddress) {
        return b.handleMemoryRegionChange(startAddress, endAddress);
    }


}
