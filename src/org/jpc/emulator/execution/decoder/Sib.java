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

public class Sib
{
    public static Pointer Ptr32_04(PeekableInputStream input, int prefices)
    {
        int sib = input.readU8();
        int seg = Prefices.getSegment(prefices, Processor.DS_INDEX);
        int base = -1;
        int index = -1;
        int offset = 0;
        switch (sib&7) {
            case 0:	/* EAX Base */
                base = Processor.EAX_INDEX;break;
            case 1:	/* ECX Base */
                base = Processor.ECX_INDEX;break;
            case 2:	/* EDX Base */
                base = Processor.EDX_INDEX;break;
            case 3:	/* EBX Base */
                base = Processor.EBX_INDEX;break;
            case 4:	/* ESP Base */
                seg = Prefices.getSegment(prefices, Processor.SS_INDEX);
                base = Processor.ESP_INDEX;break;
            case 5:	/* #1 Base */
                offset = input.read32();
                break;
            case 6:	/* ESI Base */
                base = Processor.ESI_INDEX;break;
            case 7:	/* EDI Base */
                base = Processor.EDI_INDEX;break;
            }
            int indexReg =(sib >> 3) & 7;
            switch (indexReg) {
                case 0:
                    index = Processor.EAX_INDEX;
                    break;
                case 1:
                    index = Processor.ECX_INDEX;
                    break;
                case 2:
                    index = Processor.EDX_INDEX;
                    break;
                case 3:
                    index = Processor.EBX_INDEX;
                    break;
                case 4:

                    break;
                case 5:
                    index = Processor.EBP_INDEX;
                    break;
                case 6:
                    index = Processor.ESI_INDEX;
                    break;
                case 7:
                    index = Processor.EDI_INDEX;
                    break;
            }
            sib = sib >> 6;
        int scale = 1 << sib;
        if (index == -1)
            scale = 0;
        return new Pointer(base, index, scale, offset, seg, true);
    }

    public static Pointer Ptr32_44(PeekableInputStream input, int prefices)
    {
        int sib = input.readU8();
        int seg = Prefices.getSegment(prefices, Processor.DS_INDEX);
        int base = -1;
        int index = -1;
        int offset = input.read8();
        switch (sib&7) {
            case 0:	/* EAX Base */
                base = Processor.EAX_INDEX;break;
            case 1:	/* ECX Base */
                base = Processor.ECX_INDEX;break;
            case 2:	/* EDX Base */
                base = Processor.EDX_INDEX;break;
            case 3:	/* EBX Base */
                base = Processor.EBX_INDEX;break;
            case 4:	/* ESP Base */
                seg = Prefices.getSegment(prefices, Processor.SS_INDEX);
                base = Processor.ESP_INDEX;break;
            case 5:	/* #1 Base */
                seg = Prefices.getSegment(prefices, Processor.SS_INDEX);
                base = Processor.EBP_INDEX;break;
            case 6:	/* ESI Base */
                base = Processor.ESI_INDEX;break;
            case 7:	/* EDI Base */
                base = Processor.EDI_INDEX;break;
            }
            int indexReg =(sib >> 3) & 7;
            switch (indexReg) {
                case 0:
                    index = Processor.EAX_INDEX;
                    break;
                case 1:
                    index = Processor.ECX_INDEX;
                    break;
                case 2:
                    index = Processor.EDX_INDEX;
                    break;
                case 3:
                    index = Processor.EBX_INDEX;
                    break;
                case 4:

                    break;
                case 5:
                    index = Processor.EBP_INDEX;
                    break;
                case 6:
                    index = Processor.ESI_INDEX;
                    break;
                case 7:
                    index = Processor.EDI_INDEX;
                    break;
            }
            sib = sib >> 6;
        int scale = 1 << sib;
        if (index == -1)
            scale = 0;
        return new Pointer(base, index, scale, offset, seg, true);
    }

    public static Pointer Ptr32_84(PeekableInputStream input, int prefices)
    {
        int sib = input.readU8();
        int seg = Prefices.getSegment(prefices, Processor.DS_INDEX);
        int base = -1;
        int index = -1;
        int offset = input.read32();
        switch (sib&7) {
            case 0:	/* EAX Base */
                base = Processor.EAX_INDEX;break;
            case 1:	/* ECX Base */
                base = Processor.ECX_INDEX;break;
            case 2:	/* EDX Base */
                base = Processor.EDX_INDEX;break;
            case 3:	/* EBX Base */
                base = Processor.EBX_INDEX;break;
            case 4:	/* ESP Base */
                seg = Prefices.getSegment(prefices, Processor.SS_INDEX);
                base = Processor.ESP_INDEX;break;
            case 5:	/* #1 Base */
                seg = Prefices.getSegment(prefices, Processor.SS_INDEX);
                base = Processor.EBP_INDEX;break;
            case 6:	/* ESI Base */
                base = Processor.ESI_INDEX;break;
            case 7:	/* EDI Base */
                base = Processor.EDI_INDEX;break;
            }
            int indexReg =(sib >> 3) & 7;
            switch (indexReg) {
                case 0:
                    index = Processor.EAX_INDEX;
                    break;
                case 1:
                    index = Processor.ECX_INDEX;
                    break;
                case 2:
                    index = Processor.EDX_INDEX;
                    break;
                case 3:
                    index = Processor.EBX_INDEX;
                    break;
                case 4:

                    break;
                case 5:
                    index = Processor.EBP_INDEX;
                    break;
                case 6:
                    index = Processor.ESI_INDEX;
                    break;
                case 7:
                    index = Processor.EDI_INDEX;
                    break;
            }
            sib = sib >> 6;
        int scale = 1 << sib;
        if (index == -1)
            scale = 0;
        return new Pointer(base, index, scale, offset, seg, true);
    }
}
