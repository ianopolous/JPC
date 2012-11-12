package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.memory.codeblock.CodeBlock;
import org.jpc.emulator.processor.*;

import static org.jpc.emulator.execution.Executable.*;

public class BasicBlock implements CodeBlock
{
    public static final boolean log_blocks = true;
    public Executable start;
    public BasicBlock link1, link2;
    public final int x86Length, x86Count;
    
    public BasicBlock(Executable start, int x86Length)
    {
        this.start = start;
        this.x86Length = x86Length;
        int count=1;
        while (start.next!= null)
        {
            count++;
            start = start.next;
        }
        x86Count = count;
    }

    public Branch execute(Processor cpu)
    {
        Executable current = start;
        Executable.Branch ret;

        if (log_blocks)
            System.out.printf("*****Entering %08x\n", cpu.cs.getBase()+cpu.eip);
        while ((ret = current.execute(cpu)) == Executable.Branch.None)
        {
            if (log_blocks)
                System.out.println("\t"+current);
            current = current.next;
            if (log_blocks)
                State.print(cpu);
        }
        //System.out.println("\t"+current);
        if (log_blocks)
            State.print(cpu);
        return ret;
    }

    public int getX86Length()
    {
        return x86Length;
    }

    public int getX86Count()
    {
        return x86Count;
    }

    /**
     * Returns true if this block has been rendered invalid.
     * <p>
     * If modification of memory within the given range causes this block to
     * become invalid then returns <code>true</code>.
     * @param startAddress inclusive start address of memory region
     * @param endAddress exclusive end address of memory region
     * @return <code>true</code> if this block is invalid
     */
    public boolean handleMemoryRegionChange(int startAddress, int endAddress)
    {
        return false;
    }

    public String getDisplayString()
    {
        return toString();
    }
}