package org.jpc.emulator.processor;

import java.util.*;

public final class State
{
    public static void print(Processor cpu)
    {
        StringBuilder builder = new StringBuilder(8192);
        Formatter formatter=new Formatter(builder);
        registersImpl(cpu, formatter);
        segmentsImpl(cpu, formatter);
        //extrasImpl(cpu, formatter);
        System.out.flush();
        System.err.println(builder);
    }

    private static void extrasImpl(Processor cpu, Formatter formatter) {
        formatter.format("[%8s] \n"
                ,"ticks"
        );
        extrasOnly(cpu, formatter);
    }

    private static void registersImpl(Processor cpu, Formatter formatter) {
        formatter.format("[%8s] [%8s] [%8s] [%8s] [%8s] [%8s] [%8s] [%8s] [%8s] [%8s]\n"
                ,"eax"
                ,"ebx"
                ,"ecx"
                ,"edx"
                ,"esi"
                ,"edi"
                ,"ebp"
                ,"esp"
                ,"eip"
                ,"oszapc"
        );
        registersOnly(cpu, formatter);
    }

    private static void segmentsImpl(Processor cpu, Formatter formatter) {
        formatter.format("[%4s] [%4s] [%4s] [%4s] [%4s] [%4s]\n"
                ,"cs"
                ,"ds"
                ,"es"
                ,"fs"
                ,"gs"
                ,"ss"
        );
        segmentsOnly(cpu, formatter);
    }

    private static void extrasOnly(Processor cpu, Formatter formatter) {

        formatter.format("[%8X] \n"
                ,0
        );
    }

    private static void registersOnly(Processor cpu, Formatter formatter) {
        int flags=cpu.getEFlags();

        formatter.format("[%8X] [%8X] [%8X] [%8X] [%8X] [%8X] [%8X] [%8X] [%8X] [%08X]\n"
                         ,cpu.r_eax.get32()
                ,cpu.r_ebx.get32()
                ,cpu.r_ecx.get32()
                ,cpu.r_edx.get32()
                ,cpu.r_esi.get32()
                ,cpu.r_edi.get32()
                ,cpu.r_ebp.get32()
                ,cpu.r_esp.get32()
                ,cpu.eip
                ,flags
        );
    }
    
    private static void segmentsOnly(Processor cpu, Formatter formatter) {
        int flags=cpu.getEFlags();

        formatter.format("[%4X] [%4X] [%4X] [%4X] [%4X] [%4X]\n"
                         ,cpu.cs.getSelector()&0xFFFF
                ,cpu.ds.getSelector()&0xFFFF
                ,cpu.es.getSelector()&0xFFFF
                ,cpu.fs.getSelector()&0xFFFF
                ,cpu.gs.getSelector()&0xFFFF
                ,cpu.ss.getSelector()&0xFFFF
        );
    }
}