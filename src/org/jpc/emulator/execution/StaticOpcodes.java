package org.jpc.emulator.execution;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;
import static org.jpc.emulator.execution.Executable.*;

public class StaticOpcodes
{
    public static void aad(Processor cpu, int base)
    {
        int tl = (cpu.r_eax.get8() & 0xff);
        int th = (cpu.r_eax.getHigh() & 0xff);
	int ax1 = th * base;
	int ax2 = ax1 + tl;
        cpu.r_ax.set16(ax2 & 0xff);
        //flags
        cpu.of = cpu.af = cpu.cf = false;
        cpu.flagResult = cpu.r_al.get8();
        cpu.flagStatus = SZP;
    }

    public static void aam(Processor cpu, int base)
    {
        if (base == 0) 
            throw ProcessorException.DIVIDE_ERROR;
        int tl = 0xff & cpu.r_al.get8();
        int ah = 0xff & (tl / base);
        int al = 0xff & (tl % base);
        cpu.r_eax.set16(al | (ah << 8));

        //flags
        cpu.of = cpu.af = cpu.cf = false;
        cpu.flagResult = cpu.r_al.get8();
        cpu.flagStatus = SZP;
    }

    public static void lodsb(Processor cpu)
    {
        int addr = cpu.r_esi.get32();
        cpu.r_al.set8(cpu.ds.getByte(addr));
        if (cpu.df)
            addr -= 1;
        else
            addr += 1;
        cpu.r_esi.set32(addr);
    }

    public static void lodsb_a16(Processor cpu)
    {
        int addr = 0xFFFF & cpu.r_esi.get16();
        cpu.r_al.set8(cpu.ds.getByte(addr));
        if (cpu.df)
            addr -= 1;
        else
            addr += 1;
        cpu.r_esi.set16(addr);
    }

    public static void lodsw_a16(Processor cpu)
    {
        int addr = 0xFFFF & cpu.r_esi.get16();
        cpu.r_ax.set16(cpu.ds.getWord(addr));
        if (cpu.df)
            addr -= 2;
        else
            addr += 2;
        cpu.r_esi.set16(addr);
    }

    public static void rep_cmpsb_a16(Processor cpu)
    {
        int count = 0xFFFF & cpu.r_ecx.get16();
        int addrOne = 0xFFFF & cpu.r_esi.get16();
        int addrTwo = 0xFFFF & cpu.r_edi.get16();
        int dataOne =0, dataTwo =0;

        if (count != 0)
            try {
                if (cpu.df) {
                    while (count != 0) {
                        dataOne = cpu.ds.getByte(addrOne);
                        dataTwo = cpu.es.getByte(addrTwo);
                        count--;
                        addrOne -= 1;
                        addrTwo -= 1;
                        if (dataOne != dataTwo)
                            break;
                    }
                } else {
                    while (count != 0) {
                        dataOne = cpu.ds.getByte(addrOne);
                        dataTwo = cpu.es.getByte(addrTwo);
                        count--;
                        addrOne += 1;
                        addrTwo += 1;
                        if (dataOne != dataTwo)
                            break;
                    }
                }
            }
            finally {
                cpu.r_ecx.set16(count);
                cpu.r_edi.set16(addrTwo);
                cpu.r_esi.set16(addrOne);
                cpu.flagOp1 = dataOne;
                cpu.flagOp2 = dataTwo;
                cpu.flagResult = dataOne-dataTwo;
                cpu.flagStatus = OSZAPC;
                cpu.flagIns = UCodes.SUB8;
            }
    }

    /*public static void rep_cmpsw(Processor cpu)
    {
        int count = cpu.r_ecx.get32();
        int addrOne = cpu.r_esi.get32();
        int addrTwo = cpu.r_edi.get32();
        int dataOne =0, dataTwo =0;
        if (count != 0)
            try {
                if (cpu.df) {
                    while (count != 0) {
                        dataOne = memory.getWord(addrOne);
                        dataTwo = memory.getWord(addrTwo);
                        count--;
                        addrOne -= 2;
                        addrTwo -= 2;
                        if (dataOne != dataTwo)
                            break;
                    }
                } else {
                    while (count != 0) {
                        dataOne = memory.getWord(addrOne);
                        dataTwo = memory.getWord(addrTwo);
                        count--;
                        addrOne += 2;
                        addrTwo += 2;
                        if (dataOne != dataTwo)
                            break;
                    }
                }
            }
            finally {
                cpu.r_ecx.set32(count);
                cpu.r_edi.set32(addrTwo);
                cpu.r_esi.set32(addrOne);
                cpu.flagOp1 = dataOne;
                cpu.flagOp2 = dataTwo;
                cpu.flagResult = dataOne-dataTwo;
                cpu.flagStatus = OSZAPC;
                cpu.flagIns = UCodes.SUB16;
            }
            }*/

    public static void rep_movsb_a16(Processor cpu)
    {
        int count = cpu.r_ecx.get16() & 0xffff;
        int inAddr = cpu.r_edi.get16() & 0xffff;
        int outAddr = cpu.r_esi.get16() & 0xffff;

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setByte(inAddr & 0xffff, cpu.ds.getByte(outAddr & 0xffff));
                    count--;
                    outAddr -= 1;
                    inAddr -= 1;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setByte(inAddr & 0xffff, cpu.ds.getByte(outAddr & 0xffff));
                    count--;
                    outAddr += 1;
                    inAddr += 1;
                }
            }
        }
        finally {
            cpu.r_ecx.set16(count & 0xffff);
            cpu.r_edi.set16(inAddr & 0xffff);
            cpu.r_esi.set16(outAddr & 0xffff);
        }
    }

    public static void movsw_a16(Processor cpu)
    {
        int inAddr = cpu.r_edi.get16() & 0xffff;
        int outAddr = cpu.r_esi.get16() & 0xffff;

        try {
            if (cpu.df) {
                    //check hardware interrupts
                    cpu.es.setWord(inAddr & 0xffff, cpu.ds.getWord(outAddr & 0xffff));
                    outAddr -= 2;
                    inAddr -= 2;
            } else {
                    //check hardware interrupts
                    cpu.es.setWord(inAddr & 0xffff, cpu.ds.getWord(outAddr & 0xffff));
                    outAddr += 2;
                    inAddr += 2;
            }
        }
        finally {
            cpu.r_edi.set16(inAddr & 0xffff);
            cpu.r_esi.set16(outAddr & 0xffff);
        }
    }

    public static void rep_movsw_a16(Processor cpu)
    {
        int count = cpu.r_ecx.get16() & 0xffff;
        int inAddr = cpu.r_edi.get16() & 0xffff;
        int outAddr = cpu.r_esi.get16() & 0xffff;

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setWord(inAddr & 0xffff, cpu.ds.getWord(outAddr & 0xffff));
                    count--;
                    outAddr -= 2;
                    inAddr -= 2;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setWord(inAddr & 0xffff, cpu.ds.getWord(outAddr & 0xffff));
                    count--;
                    outAddr += 2;
                    inAddr += 2;
                }
            }
        }
        finally {
            cpu.r_ecx.set16(count & 0xffff);
            cpu.r_edi.set16(inAddr & 0xffff);
            cpu.r_esi.set16(outAddr & 0xffff);
        }
    }

    public static void rep_movsd_a32(Processor cpu)
    {
        int count = cpu.r_ecx.get32();
        int targetAddr = cpu.r_edi.get32();
        int srcAddr = cpu.r_esi.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setDoubleWord(targetAddr, cpu.ds.getDoubleWord(srcAddr));
                    count--;
                    srcAddr -= 4;
                    targetAddr -= 4;
                }
            } else {
                while (count != 0) {
                    cpu.es.setDoubleWord(targetAddr, cpu.ds.getDoubleWord(srcAddr));
                    count--;
                    srcAddr += 4;
                    targetAddr += 4;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(targetAddr);
            cpu.r_esi.set32(srcAddr);
        }
    }

    /*public static void repne_scasb(Processor cpu)
    {
        int count = cpu.r_ecx.get32();
        int tAddr = cpu.r_edi.get32();
        int data = cpu.r_al.get8();
        int input = 0;

        try {
            if (cpu.df) {
                while (count != 0) {
                    input = 0xff & memory.getByte(tAddr);
                    count--;
                    tAddr -= 1;
                    if (data == input) break;
                }
            } else {
                while (count != 0) {
                    input = 0xff & memory.getByte(tAddr);
                    count--;
                    tAddr += 1;
                    if (data == input) break;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(tAddr);
            cpu.flagOp1 = data;
            cpu.flagOp2 = input;
            cpu.flagResult = data-input;
            cpu.flagIns = UCodes.SUB8;
        }
    }*/

    public static void rep_insw_a16(Processor cpu)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_edi.get16() & 0xffff;

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setWord(addr & 0xffff, (short)cpu.ioports.ioPortRead16(port));
                    count--;
                    addr -= 2;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setWord(addr & 0xffff, (short)cpu.ioports.ioPortRead16(port));
                    count--;
                    addr += 2;
                }
            }
        }
        finally {
            cpu.r_ecx.set16(count);
            cpu.r_edi.set16(addr);
        }
    }

    public static void rep_insd_a16(Processor cpu)
    {
        int port = cpu.r_dx.get16() & 0xffff;
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_edi.get16() & 0xffff;

        try {
            if (cpu.df) {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setDoubleWord(addr & 0xffff, (short)cpu.ioports.ioPortRead32(port));
                    count--;
                    addr -= 4;
                }
            } else {
                while (count != 0) {
                    //check hardware interrupts
                    cpu.es.setDoubleWord(addr & 0xffff, (short)cpu.ioports.ioPortRead32(port));
                    count--;
                    addr += 4;
                }
            }
        }
        finally {
            cpu.r_ecx.set16(count);
            cpu.r_edi.set16(addr);
        }
    }

    public static void rep_stosb_a32(Processor cpu)
    {
        int count = cpu.r_ecx.get32();
        int tAddr = cpu.r_edi.get32();
        int data = cpu.r_al.get8();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setByte(tAddr, (byte) data);
                    count--;
                    tAddr -= 1;
                }
            } else {
                while (count != 0) {
                    cpu.es.setByte(tAddr, (byte) data);
                    count--;
                    tAddr += 1;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(tAddr);
        }
    }

    public static void rep_stosb_a16(Processor cpu)
    {
        int count = cpu.r_cx.get16();
        int tAddr = cpu.r_di.get16();
        byte data = (byte)cpu.r_al.get8();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setByte(tAddr, data);
                    count--;
                    tAddr -= 1;
                }
            } else {
                while (count != 0) {
                    cpu.es.setByte(tAddr, data);
                    count--;
                    tAddr += 1;
                }
            }
        }
        finally {
            cpu.r_cx.set16(count);
            cpu.r_di.set16(tAddr);
        }
    }

    public static void stosw_a16(Processor cpu)
    {
        int tAddr = 0xFFFF & cpu.r_di.get16();
        short data = (short)cpu.r_ax.get16();

        try {
            if (cpu.df) {
                cpu.es.setWord(tAddr, data);
                tAddr -= 2;
            } else {
                cpu.es.setWord(tAddr, data);
                tAddr += 2;
            }
        }
        finally {
            cpu.r_di.set16(tAddr);
        }
    }

    public static void rep_stosw_a16(Processor cpu)
    {
        int count = cpu.r_cx.get16();
        int tAddr = cpu.r_di.get16();
        short data = (short)cpu.r_ax.get16();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setWord(tAddr, data);
                    count--;
                    tAddr -= 2;
                }
            } else {
                while (count != 0) {
                    cpu.es.setWord(tAddr, data);
                    count--;
                    tAddr += 2;
                }
            }
        }
        finally {
            cpu.r_cx.set16(count);
            cpu.r_di.set16(tAddr);
        }
    }

    public static void rep_stosd_a16(Processor cpu)
    {
        int count = cpu.r_cx.get16();
        int tAddr = cpu.r_di.get16();
        int data = cpu.r_eax.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setDoubleWord(tAddr, data);
                    count--;
                    tAddr -= 4;
                }
            } else {
                while (count != 0) {
                    cpu.es.setDoubleWord(tAddr, data);
                    count--;
                    tAddr += 4;
                }
            }
        }
        finally {
            cpu.r_cx.set16(count);
            cpu.r_di.set16(tAddr);
        }
    }

    public static void rep_stosd_a32(Processor cpu)
    {
        int count = cpu.r_ecx.get32();
        int tAddr = cpu.r_edi.get32();
        int data = cpu.r_eax.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    cpu.es.setDoubleWord(tAddr, data);
                    count--;
                    tAddr -= 4;
                }
            } else {
                while (count != 0) {
                    cpu.es.setDoubleWord(tAddr, data);
                    count--;
                    tAddr += 4;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(tAddr);
        }
    }

    public static final void repne_scasb_a16(Processor cpu)
    {
        int data = 0xff & cpu.r_al.get8();
        int count = cpu.r_ecx.get16() & 0xffff;
        int addr = cpu.r_edi.get16() & 0xffff;
        boolean used = count != 0;
        int input = 0;

        try {
            if (cpu.df) {
                while (count != 0) {
                    input = 0xff & cpu.es.getByte(addr);
                    count--;
                    addr -= 1;
                    if (data == input) break;
                }
            } else {
                while (count != 0) {
                    input = 0xff & cpu.es.getByte(addr);
                    count--;
                    addr += 1;
                    if (data == input) break;
                }
            }
        } finally {
            cpu.r_ecx.set16(count & 0xffff);
            cpu.r_edi.set16(addr & 0xffff);
            cpu.flagOp1 = data;
            cpu.flagOp2 = input;
            cpu.flagResult = data-input;
            cpu.flagIns = UCodes.SUB8;
        }
    }
}
