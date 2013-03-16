package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fbld_Mt_mem extends Executable
{
    final Pointer op1;

    public fbld_Mt_mem(int blockStart, int eip, int prefices, PeekableInputStream input)
    {
        super(blockStart, eip);
        int modrm = input.readU8();
        op1 = Modrm.getPointer(prefices, modrm, input);
    }

    public Branch execute(Processor cpu)
    {
        byte[] data = op1.getF80(cpu);
        long n = 0;
        long decade = 1;
        for (int i = 0; i < 9; i++) 
        {
            byte b = data[i];
            n += (b & 0xf) * decade; 
            decade *= 10;
            n += ((b >> 4) & 0xf) * decade; 
            decade *= 10;
        }
        byte sign = data[9];
        double m = (double)n;
        if (sign < 0)
            m *= -1.0;
       cpu.fpu.push(m);
        return Branch.None;
    }

    public boolean isBranch()
    {
        return false;
    }

    public String toString()
    {
        return this.getClass().getName();
    }
}