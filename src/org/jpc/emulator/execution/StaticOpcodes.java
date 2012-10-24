package org.jpc.emulator.execution;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import static org.jpc.emulator.processor.Processor.*;

public class StaticOpcodes
{
    /*    public static void rep_cmpsb(Processor cpu)
    {
        int count = cpu.r_ecx.get32();
        int addrOne = cpu.r_esi.get32();
        int addrTwo = cpu.r_edi.get32();
        int dataOne =0, dataTwo =0;

        if (count != 0)
            try {
                if (cpu.df) {
                    while (count != 0) {
                        dataOne = memory.getByte(addrOne);
                        dataTwo = memory.getByte(addrTwo);
                        count--;
                        addrOne -= 1;
                        addrTwo -= 1;
                        if (dataOne != dataTwo)
                            break;
                    }
                } else {
                    while (count != 0) {
                        dataOne = memory.getByte(addrOne);
                        dataTwo = memory.getByte(addrTwo);
                        count--;
                        addrOne += 1;
                        addrTwo += 1;
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
                cpu.flagIns = UCodes.SUB8;
            }
    }

    public static void rep_cmpsw(Processor cpu)
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
    }

    public static void rep_movsb(Processor cpu)
    {
        int count = cpu.r_ecx.get32();
        int inAddr = cpu.r_edi.get32();
        int outAddr = cpu.r_esi.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    memory.setByte(inAddr, memory.getByte(outAddr));
                    count--;
                    outAddr -= 1;
                    inAddr -= 1;
                }
            } else {
                while (count != 0) {
                    memory.setByte(inAddr, memory.getByte(outAddr));
                    count--;
                    outAddr += 1;
                    inAddr += 1;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(inAddr);
            cpu.r_esi.set32(outAddr);
        }
    }

    public static void rep_movsd(Processor cpu)
    {
        int count = cpu.r_ecx.get32();
        int inAddr = cpu.r_edi.get32();
        int outAddr = cpu.r_esi.get32();

        try {
            if (cpu.df) {
                while (count != 0) {
                    memory.setDoubleWord(inAddr, memory.getDoubleWord(outAddr));
                    count--;
                    outAddr -= 4;
                    inAddr -= 4;
                }
            } else {
                while (count != 0) {
                    memory.setDoubleWord(inAddr, memory.getDoubleWord(outAddr));
                    count--;
                    outAddr += 4;
                    inAddr += 4;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(inAddr);
            cpu.r_esi.set32(outAddr);
        }
    }

    public static void repne_scasb(Processor cpu)
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
    }

    public static void rep_stosb(Processor cpu)
    {
        int count = cpu.r_ecx.get32();
        int tAddr = cpu.r_edi.get32();
        int data = cpu.r_al.get8();

        try {
            if (cpu.df) {
                while (count != 0) {
                    memory.setByte(tAddr, (byte) data);
                    count--;
                    tAddr -= 1;
                }
            } else {
                while (count != 0) {
                    memory.setByte(tAddr, (byte) data);
                    count--;
                    tAddr += 1;
                }
            }
        }
        finally {
            cpu.r_ecx.set32(count);
            cpu.r_edi.set32(tAddr);
        }
        }*/

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
}
