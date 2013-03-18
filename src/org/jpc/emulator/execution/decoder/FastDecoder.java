package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.processor.Processor;

public class FastDecoder
{
    static OpcodeDecoder[] pmOps = new OpcodeDecoder[0x800];
    static OpcodeDecoder[] rmOps = new OpcodeDecoder[0x800];
    static OpcodeDecoder[] vmOps = new OpcodeDecoder[0x800];

    public static Executable decodePMOpcode(int blockStart, PeekableInputStream input, boolean is32Bit)
    {
        int opStart = (int) input.getAddress();
        int prefices = 0;
        int b = input.readU8();
        boolean addrSize = is32Bit;
        while (Prefices.isPrefix(b))
        {
            if (b == 0x66)
                is32Bit = !is32Bit;
            else if (b == 0x67)
                addrSize = !addrSize;
            else
                prefices |= Prefices.encodePrefix(b);
            b = input.readU8();
        }
        int opcode = 0;
        if (is32Bit)
        {
            opcode += 0x200;
            prefices |= Prefices.encodePrefix(0x66);
        }
        if (addrSize)
        {
            opcode += 0x400;
            prefices |=Prefices.encodePrefix(0x67);
        }
        if (b == 0x0F)
        {
            opcode += 0x100;
            b = input.readU8();
        }
        opcode += b;
        return pmOps[opcode].decodeOpcode(blockStart, opStart, prefices, input);
    }
}
