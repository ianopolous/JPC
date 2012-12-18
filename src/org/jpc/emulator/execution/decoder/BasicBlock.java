package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.memory.codeblock.CodeBlock;
import org.jpc.emulator.processor.*;
import org.jpc.j2se.Option;

import static org.jpc.emulator.execution.Executable.*;

public class BasicBlock implements CodeBlock
{
    public static final boolean LOG_BLOCKENTRY = Option.log_blockentry.value();
    public static final boolean LOG_STATE = Option.log_state.value();

    public Executable start;
    public BasicBlock link1, link2;
    public final int x86Length, x86Count;
    
    public BasicBlock(Executable start, int x86Length, int x86Count)
    {
        this.start = start;
        this.x86Length = x86Length;
        this.x86Count = x86Count;
        if (x86Count == 0)
            throw new IllegalStateException("Block with zero x86Count!");
    }

    public Branch execute(Processor cpu)
    {
        Executable current = start;
        Executable.Branch ret;

        if (LOG_BLOCKENTRY)
            System.out.printf("*****Entering basic block %08x\n", cpu.cs.getBase()+cpu.eip);
        while ((ret = current.execute(cpu)) == Executable.Branch.None)
        {
            if (LOG_STATE)
                System.out.println("\t"+current);
            current = current.next;
            if (LOG_STATE)
                State.print(cpu);
        }
        if (LOG_STATE)
            System.out.println("\t"+current);
        if (LOG_STATE)
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