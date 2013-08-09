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

public class Modrm
{
    public static int getSegmentIndex(int prefices)
    {
        return (prefices >> 2) & 7;
    }

    public static int Ib(PeekableInputStream in)
    {
        return in.read8();
    }

    public static int Iw(PeekableInputStream in)
    {
        return in.read16();
    }

    public static int Id(PeekableInputStream in)
    {
        return in.read32();
    }

    public static int Jb(PeekableInputStream in)
    {
        return in.read8();
    }

    public static int Jw(PeekableInputStream in)
    {
        return in.read16();
    }

    public static int Jd(PeekableInputStream in)
    {
        return in.read32();
    }

    public static int jmpOffset(int prefices, PeekableInputStream in)
    {
        if ((prefices & 1) != 0)
            return in.read32();
        return in.readU16();
    }

    public static int jmpCs(PeekableInputStream in)
    {
        return in.readU16();
    }

    // al, cl, dl, bl, ah, ch, dh, bh, ax, cx, dx, bx, sp, bp, si, di, eax, ecx, edx, ebx, esp, ebp, esi, edi
    private static int[] regIndices = new int[]{3, 11, 15, 7, 2, 10, 14, 6, 1, 9, 13, 5, 21, 23, 17, 19, 0, 8, 12, 4, 20, 22, 16, 18};

    public static int mod(int modrm)
    {
        return (modrm >> 6) & 3;
    }

    public static int reg(int modrm)
    {
        return (modrm >> 3) & 7;
    }

    public static int rm(int modrm)
    {
        return modrm & 7;
    }

    public static boolean isMem(int modrm)
    {
        return (modrm & 0xC0) != 0xC0;
    }

    public static int R(int modrm) {
        return regIndices[(modrm &7) + 16];
    }

    public static int Eb(int modrm) {
        return regIndices[modrm &7];
    }

    public static int Gb(int modrm) {
        return regIndices[(modrm >> 3) & 7];
    }

    public static int Ew(int modrm) {
        return regIndices[8 + (modrm & 7)];
    }

    public static int Gw(int modrm) {
        return regIndices[8 + ((modrm >> 3) & 7)];
    }

    public static int Ed(int modrm) {
        return regIndices[16 + (modrm & 7)];
    }

    public static int Gd(int modrm) {
        return regIndices[16 + ((modrm >> 3) & 7)];
    }

    public static Pointer Ob(int prefices, PeekableInputStream input)
    {
        if (Prefices.isAddr16(prefices))
            return new Pointer(-1, -1, 0, input.readU16(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
        else
            return new Pointer(-1, -1, 0, input.read32(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    public static Pointer Ow(int prefices, PeekableInputStream input)
    {
        if (Prefices.isAddr16(prefices))
            return new Pointer(-1, -1, 0, input.readU16(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
        else
            return new Pointer(-1, -1, 0, input.read32(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    public static Pointer Od(int prefices, PeekableInputStream input)
    {
        if (Prefices.isAddr16(prefices))
            return new Pointer(-1, -1, 0, input.readU16(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
        else
            return new Pointer(-1, -1, 0, input.read32(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    public static Pointer getPointer(int prefices, int modrm, PeekableInputStream input)
    {
        if ((prefices & 2) != 0)
            return getPointer32(prefices, modrm, input);
        return getPointer16(prefices, modrm, input);
    }

    static Pointer getPointer16(int prefices, int modrm, PeekableInputStream input)
    {
        if (modrm < 0x40) {
            switch (modrm & 7) {
                case 0x00: return Ptr16_00(input, prefices);
                case 0x01: return Ptr16_01(input, prefices);
                case 0x02: return Ptr16_02(input, prefices);
                case 0x03: return Ptr16_03(input, prefices);
                case 0x04: return Ptr16_04(input, prefices);
                case 0x05: return Ptr16_05(input, prefices);
                case 0x06: return Ptr16_06(input, prefices);
                case 0x07: return Ptr16_07(input, prefices);
            }
        } else if (modrm<0x80) {
            switch (modrm & 7) {
                case 0x00: return Ptr16_40(input, prefices);
                case 0x01: return Ptr16_41(input, prefices);
                case 0x02: return Ptr16_42(input, prefices);
                case 0x03: return Ptr16_43(input, prefices);
                case 0x04: return Ptr16_44(input, prefices);
                case 0x05: return Ptr16_45(input, prefices);
                case 0x06: return Ptr16_46(input, prefices);
                case 0x07: return Ptr16_47(input, prefices);
            }
        } else {
            switch (modrm & 7) {
                case 0x00: return Ptr16_80(input, prefices);
                case 0x01: return Ptr16_81(input, prefices);
                case 0x02: return Ptr16_82(input, prefices);
                case 0x03: return Ptr16_83(input, prefices);
                case 0x04: return Ptr16_84(input, prefices);
                case 0x05: return Ptr16_85(input, prefices);
                case 0x06: return Ptr16_86(input, prefices);
                case 0x07: return Ptr16_87(input, prefices);
            }
        }
        return null;
    }

    private static Pointer Ptr16_00(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BX_INDEX, Processor.SI_INDEX, 1, 0, Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_01(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BX_INDEX, Processor.DI_INDEX, 1, 0, Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_02(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BP_INDEX, Processor.SI_INDEX, 1, 0, Prefices.getSegment(prefices, Processor.SS_INDEX), false);
    }

    private static Pointer Ptr16_03(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BP_INDEX, Processor.DI_INDEX, 1, 0, Prefices.getSegment(prefices, Processor.SS_INDEX), false);
    }

    private static Pointer Ptr16_04(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.SI_INDEX, -1, 0, 0, Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_05(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.DI_INDEX, -1, 0, 0, Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_06(PeekableInputStream input, int prefices)
    {
        return new Pointer(-1, -1, 0, input.readU16(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_07(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BX_INDEX, -1, 0, 0, Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_40(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BX_INDEX, Processor.SI_INDEX, 1, input.read8(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_41(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BX_INDEX, Processor.DI_INDEX, 1, input.read8(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_42(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BP_INDEX, Processor.SI_INDEX, 1, input.read8(), Prefices.getSegment(prefices, Processor.SS_INDEX), false);
    }

    private static Pointer Ptr16_43(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BP_INDEX, Processor.DI_INDEX, 1, input.read8(), Prefices.getSegment(prefices, Processor.SS_INDEX), false);
    }

    private static Pointer Ptr16_44(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.SI_INDEX, -1, 0, input.read8(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_45(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.DI_INDEX, -1, 0, input.read8(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_46(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BP_INDEX, -1, 0, input.read8(), Prefices.getSegment(prefices, Processor.SS_INDEX), false);
    }

    private static Pointer Ptr16_47(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BX_INDEX, -1, 0, input.read8(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_80(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BX_INDEX, Processor.SI_INDEX, 1, input.readU16(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_81(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BX_INDEX, Processor.DI_INDEX, 1, input.readU16(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_82(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BP_INDEX, Processor.SI_INDEX, 1, input.readU16(), Prefices.getSegment(prefices, Processor.SS_INDEX), false);
    }

    private static Pointer Ptr16_83(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BP_INDEX, Processor.DI_INDEX, 1, input.readU16(), Prefices.getSegment(prefices, Processor.SS_INDEX), false);
    }

    private static Pointer Ptr16_84(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.SI_INDEX, -1, 0, input.readU16(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_85(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.DI_INDEX, -1, 0, input.readU16(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    private static Pointer Ptr16_86(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BP_INDEX, -1, 0, input.readU16(), Prefices.getSegment(prefices, Processor.SS_INDEX), false);
    }

    private static Pointer Ptr16_87(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.BX_INDEX, -1, 0, input.readU16(), Prefices.getSegment(prefices, Processor.DS_INDEX), false);
    }

    static Pointer getPointer32(int prefices, int modrm, PeekableInputStream input)
    {
        if (modrm < 0x40) {
            switch (modrm & 7) {
                case 0x00: return Ptr32_00(input, prefices);
                case 0x01: return Ptr32_01(input, prefices);
                case 0x02: return Ptr32_02(input, prefices);
                case 0x03: return Ptr32_03(input, prefices);
                case 0x04: return Ptr32_04(input, prefices);
                case 0x05: return Ptr32_05(input, prefices);
                case 0x06: return Ptr32_06(input, prefices);
                case 0x07: return Ptr32_07(input, prefices);
            }
        } else if (modrm<0x80) {
            switch (modrm & 7) {
                case 0x00: return Ptr32_40(input, prefices);
                case 0x01: return Ptr32_41(input, prefices);
                case 0x02: return Ptr32_42(input, prefices);
                case 0x03: return Ptr32_43(input, prefices);
                case 0x04: return Ptr32_44(input, prefices);
                case 0x05: return Ptr32_45(input, prefices);
                case 0x06: return Ptr32_46(input, prefices);
                case 0x07: return Ptr32_47(input, prefices);
            }
        } else {
            switch (modrm & 7) {
                case 0x00: return Ptr32_80(input, prefices);
                case 0x01: return Ptr32_81(input, prefices);
                case 0x02: return Ptr32_82(input, prefices);
                case 0x03: return Ptr32_83(input, prefices);
                case 0x04: return Ptr32_84(input, prefices);
                case 0x05: return Ptr32_85(input, prefices);
                case 0x06: return Ptr32_86(input, prefices);
                case 0x07: return Ptr32_87(input, prefices);
            }
        }
        return null;
    }

    private static Pointer Ptr32_00(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EAX_INDEX, -1, 0, 0, Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_01(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.ECX_INDEX, -1, 0, 0, Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_02(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EDX_INDEX, -1, 0, 0, Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_03(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EBX_INDEX, -1, 0, 0, Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_04(PeekableInputStream input, int prefices)
    {
        return Sib.Ptr32_04(input, prefices);
    }

    private static Pointer Ptr32_05(PeekableInputStream input, int prefices)
    {
        return new Pointer(-1, -1, 0, input.read32(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_06(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.ESI_INDEX, -1, 0, 0, Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_07(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EDI_INDEX, -1, 0, 0, Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_40(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EAX_INDEX, -1, 0, input.read8(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_41(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.ECX_INDEX, -1, 0, input.read8(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_42(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EDX_INDEX, -1, 0, input.read8(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_43(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EBX_INDEX, -1, 0, input.read8(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_44(PeekableInputStream input, int prefices)
    {
        return Sib.Ptr32_44(input, prefices);
    }

    private static Pointer Ptr32_45(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EBP_INDEX, -1, 0, input.read8(), Prefices.getSegment(prefices, Processor.SS_INDEX), true);
    }

    private static Pointer Ptr32_46(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.ESI_INDEX, -1, 0, input.read8(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_47(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EDI_INDEX, -1, 0, input.read8(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_80(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EAX_INDEX, -1, 0, input.read32(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_81(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.ECX_INDEX, -1, 0, input.read32(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_82(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EDX_INDEX, -1, 0, input.read32(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_83(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EBX_INDEX, -1, 0, input.read32(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_84(PeekableInputStream input, int prefices)
    {
        return Sib.Ptr32_84(input, prefices);
    }

    private static Pointer Ptr32_85(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EBP_INDEX, -1, 0, input.read32(), Prefices.getSegment(prefices, Processor.SS_INDEX), true);
    }

    private static Pointer Ptr32_86(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.ESI_INDEX, -1, 0, input.read32(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }

    private static Pointer Ptr32_87(PeekableInputStream input, int prefices)
    {
        return new Pointer(Processor.EDI_INDEX, -1, 0, input.read32(), Prefices.getSegment(prefices, Processor.DS_INDEX), true);
    }
}
