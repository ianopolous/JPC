package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class retf extends Executable
{
    final int blockLength;
    final int instructionLength;

    public retf(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        instructionLength = (int)input.getAddress()-eip;
        blockLength = (int)input.getAddress()-blockStart;
    }

    public Branch execute(Processor cpu)
    {
        //System.out.printf("Reading far return address from %08x\n", cpu.r_esp.get32());
        cpu.eip = 0xFFFF&cpu.pop16();
        //System.out.printf("Far return to eip=%08x\n", cpu.eip);
        cpu.cs(0xffff & cpu.pop16());
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