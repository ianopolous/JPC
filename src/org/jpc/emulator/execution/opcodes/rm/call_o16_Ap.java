package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class call_o16_Ap extends Executable
{
    final int cs, targetEip;
    final int blockLength;
    final int instructionLength;

    public call_o16_Ap(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        blockLength = parent.x86Length+(int)parent.eip-blockStart;
        instructionLength = parent.x86Length;
        targetEip = parent.operand[0].ptr.off;
        cs = parent.operand[0].ptr.seg;
    }


    public call_o16_Ap(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        targetEip = Modrm.jmpOffset(prefices, input);
        cs = Modrm.jmpCs(input);
        instructionLength = (int)input.getAddress()-eip;
        blockLength = (int)input.getAddress()-blockStart;
    }

    public Branch execute(Processor cpu)
    {
        cpu.eip += blockLength;
        cpu.callFar(cs, (short)targetEip);
        return Branch.Call_Unknown;
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