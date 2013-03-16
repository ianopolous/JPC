package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.execution.*;

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
        while (isPrefix(b))
        {
            if (b == 0x66)
                is32Bit = !is32Bit;
            else if (b == 0x67)
                addrSize = !addrSize;
            else
                prefices |= encodePrefix(b);
            b = input.readU8();
        }
        int opcode = 0;
        if (is32Bit)
        {
            opcode += 0x200;
            prefices |= encodePrefix(0x66);
        }
        if (addrSize)
        {
            opcode += 0x400;
            prefices |= encodePrefix(0x67);
        }
        if (b == 0x0F)
        {
            opcode += 0x100;
            b = input.readU8();
        }
        opcode += b;
        return pmOps[opcode].decodeOpcode(blockStart, opStart, prefices, input);
    }

    public static int encodePrefix(int b)
    {
        if (b == 0x66) // Op size
            return 1;
        if (b == 0x67) // addr size
            return 1 << 1;
        if (b == 0x26) // ES
            return 1 << 2;
        if (b == 0x2E) // CS
            return 2 << 2;
        if (b == 0x36) // SS
            return 3 << 2;
        if (b == 0x3E) // DS
            return 4 << 2;
        if (b == 0x64) // FS
            return 5 << 2;
        if (b == 0x65) // GS
            return 6 << 2;
        if (b == 0xF0) // Lock
            return 1 << 5;
        if (b == 0xF2) // REPNE
            return 1 << 6;
        if (b == 0xF3) // REP
            return 1 << 7;
        return 0;
    }

    public static boolean isPrefix(int b)
    {
        if (b == 0x66) // Op size
            return true;
        if (b == 0x67) // addr size
            return true;
        if (b == 0x26) // ES
            return true;
        if (b == 0x2E) // CS
            return true;
        if (b == 0x36) // SS
            return true;
        if (b == 0x3E) // DS
            return true;
        if (b == 0x64) // FS
            return true;
        if (b == 0x65) // GS
            return true;
        if (b == 0xF0) // Lock
            return true;
        if (b == 0xF2) // REPNE
            return true;
        if (b == 0xF3) // REP
            return true;
        return false;
    }
}
