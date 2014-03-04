package org.jpc.emulator.execution.opcodes.rm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class InvalidOpcode extends Executable
{
    final int blockLength;
    final int instructionLength;
    String error;

    public InvalidOpcode(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        instructionLength = (int)input.getAddress()-eip;
        blockLength = (int)input.getAddress()-blockStart;
        input.seek(-instructionLength);
        Instruction in = Disassembler.disassemble(input, Prefices.isAddr16(prefices) ? 32 : 16);
        error = in.toString() + ", x86 byte = " + Disassembler.getRawBytes(input, 0);
    }

    public Branch execute(Processor cpu)
    {
        if (true) throw ProcessorException.UNDEFINED;
        return Branch.Jmp_Unknown;
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