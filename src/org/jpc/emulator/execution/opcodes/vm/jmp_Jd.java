package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class jmp_Jd extends Executable
{
    final int jmp;
    final int blockLength;
    final int instructionLength;

    public jmp_Jd(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
        instructionLength = parent.x86Length;
        jmp = (int)parent.operand[0].lval;
    }


    public jmp_Jd(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        jmp = Modrm.Jd(input);
        instructionLength = (int)input.getAddress()-eip;
        blockLength = (int)input.getAddress()-blockStart;
    }

    public Branch execute(Processor cpu)
    {
        cpu.eip += jmp+blockLength;
        cpu.eip &= 0xFFFF;
        return Branch.T1;
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