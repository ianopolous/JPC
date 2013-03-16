package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.processor.Processor;

public class Sib
{
    public static Pointer Ptr32_04(PeekableInputStream input)
    {
        int sib = input.readU8();
        int seg = Processor.DS_INDEX;
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
                seg = Processor.SS_INDEX;
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
        return new Pointer(base, index, scale, offset, seg, true);
    }

    public static Pointer Ptr32_44(PeekableInputStream input)
    {
        int sib = input.readU8();
        int seg = Processor.DS_INDEX;
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
                seg = Processor.SS_INDEX;
                base = Processor.ESP_INDEX;break;
            case 5:	/* #1 Base */
                seg = Processor.SS_INDEX;
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
        return new Pointer(base, index, scale, offset, seg, true);
    }

    public static Pointer Ptr32_84(PeekableInputStream input)
    {
        int sib = input.readU8();
        int seg = Processor.DS_INDEX;
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
                seg = Processor.SS_INDEX;
                base = Processor.ESP_INDEX;break;
            case 5:	/* #1 Base */
                seg = Processor.SS_INDEX;
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
        return new Pointer(base, index, scale, offset, seg, true);
    }
}
