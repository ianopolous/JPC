package org.jpc.emulator.execution.opcodes.vm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class fbstp_Mt_mem extends Executable
{
    final Pointer op1;

    public fbstp_Mt_mem(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
        op1 = new Pointer(parent.operand[0], parent.adr_mode);
    }

    public Branch execute(Processor cpu)
    {
        byte[] data = new byte[10];
        long n = (long)Math.abs(cpu.fpu.ST(0));
        long decade = 1;
        for (int i = 0; i < 9; i++) 
        {
            int val = (int) ((n % (decade * 10)) / decade);
            byte b = (byte) val;
            decade *= 10;
            val = (int) ((n % (decade * 10)) / decade);
            b |= (val << 4);
            data[i] = b;
       }
       data[9] =  (cpu.fpu.ST(0) < 0) ? (byte)0x80 : (byte)0x00;
       op1.setF80(cpu,  data);
       cpu.fpu.pop();
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