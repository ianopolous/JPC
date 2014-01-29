/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

    Copyright (C) 2012-2013 Ian Preston

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

    Details (including contact information) can be found at:

    jpc.sourceforge.net
    or the developer website
    sourceforge.net/projects/jpc/

    End of licence header
*/

package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.PC;
import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.codeblock.CodeBlock;
import org.jpc.emulator.processor.*;
import org.jpc.j2se.Option;

import static org.jpc.emulator.execution.Executable.*;

public class BasicBlock implements CodeBlock
{
    public static final boolean LOG_BLOCKENTRY = Option.log_blockentry.value();
    public static final boolean LOG_STATE = Option.log_state.value();
    public static final boolean SINGLE_STEP_TIME = Option.singlesteptime.value();
    public static final int MIN_ADDR_WATCH = Option.min_addr_watch.intValue(0);
    public static final int MAX_ADDR_WATCH = Option.max_addr_watch.intValue(0xffffffff);
    public static int lastExitEip;

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

    public void preBlock(Processor cpu)
    {
        if (LOG_BLOCKENTRY)
            System.out.printf("***** %08x:%08x\n", cpu.cs.getBase(), cpu.eip);
        if (PC.HISTORY)
            PC.logBlock(cpu.getInstructionPointer(), this);
    }

    public void postBlock(Processor cpu)
    {
        if (PC.HISTORY)
            lastExitEip = cpu.getInstructionPointer();
    }

    private boolean watchedAddress(int addr)
    {
        if (addr < MIN_ADDR_WATCH)
            return false;
        if ((addr & 0xFFFFFFFFL) > (MAX_ADDR_WATCH & 0xFFFFFFFFL))
            return false;
        return true;
    }

    public void postInstruction(Processor cpu, Executable last)
    {
        if ((LOG_STATE) && watchedAddress(cpu.getInstructionPointer()))
        {
            System.out.println("\t"+last);
            State.print(cpu);
        }
        if ((SINGLE_STEP_TIME) && !last.toString().contains("eip"))
            cpu.vmClock.update(1);
        cpu.rf(false);
    }

    public Branch execute(Processor cpu)
    {
        Executable current = start;
        Executable.Branch ret;

        preBlock(cpu);
        while ((ret = current.execute(cpu)) == Executable.Branch.None)
        {
            postInstruction(cpu, current);
            current = current.next;
        }
        postInstruction(cpu, current);
        postBlock(cpu);
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

    public boolean handleMemoryRegionChange(int startAddress, int endAddress)
    {
        return false;
    }

    public String getDisplayString()
    {
        return toString();
    }

    public Instruction getInstructions()
    {
        return null;
    }
}