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

import org.jpc.emulator.processor.Processor;

public class Prefices
{
    public static int encodePrefix(int prefices, int b)
    {
        if (b == 0x66) // Op size
            return prefices ^ 1;
        if (b == 0x67) // addr size
            return prefices ^ (1 << 1);
        if (b == 0x26) // ES
            return (Processor.ES_INDEX << 2) | (prefices & ~0x1C);
        if (b == 0x2E) // CS
            return (Processor.CS_INDEX << 2) | (prefices & ~0x1C);
        if (b == 0x36) // SS
            return (Processor.SS_INDEX << 2) | (prefices & ~0x1C);
        if (b == 0x3E) // DS
            return (Processor.DS_INDEX << 2) | (prefices & ~0x1C);
        if (b == 0x64) // FS
            return (Processor.FS_INDEX << 2) | (prefices & ~0x1C);
        if (b == 0x65) // GS
            return (Processor.GS_INDEX << 2) | (prefices & ~0x1C);
        if (b == 0xF0) // Lock
            return (1 << 5) | prefices;
        if (b == 0xF2) // REPNE
            return (1 << 6) | prefices;
        if (b == 0xF3) // REP
            return (1 << 7) | prefices;
        return 0;
    }

    public static boolean isAddr16(int prefices)
    {
        return (prefices & 2) == 0;
    }

    public static int getSegment(int prefices, int def)
    {
        int seg = ((prefices >> 2) & 7);
        if (seg == 7) // no override
            return def;
        return seg;
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
