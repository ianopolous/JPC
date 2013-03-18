package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.processor.Processor;

public class Prefices
{
    public static int encodePrefix(int b)
    {
        if (b == 0x66) // Op size
            return 1;
        if (b == 0x67) // addr size
            return 1 << 1;
        if (b == 0x26) // ES
            return Processor.ES_INDEX << 2;
        if (b == 0x2E) // CS
            return Processor.CS_INDEX << 2;
        if (b == 0x36) // SS
            return Processor.SS_INDEX << 2;
        if (b == 0x3E) // DS
            return Processor.DS_INDEX << 2;
        if (b == 0x64) // FS
            return Processor.FS_INDEX << 2;
        if (b == 0x65) // GS
            return Processor.GS_INDEX << 2;
        if (b == 0xF0) // Lock
            return 1 << 5;
        if (b == 0xF2) // REPNE
            return 1 << 6;
        if (b == 0xF3) // REP
            return 1 << 7;
        return 0;
    }

    public static boolean isLock(int prefices)
    {
        return (prefices & (1 << 5)) != 0;
    }

    public static boolean isRep(int prefices)
    {
        return (prefices & (1 << 7)) != 0;
    }

    public static boolean isRepne(int prefices)
    {
        return (prefices & (1 << 6)) != 0;
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
