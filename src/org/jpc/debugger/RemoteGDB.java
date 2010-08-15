/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.4

    A project from the Physics Dept, The University of Oxford

    Copyright (C) 2007-2010 The University of Oxford

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

    Conceived and Developed by:
    Rhys Newman, Ian Preston, Chris Dennis

    End of licence header
*/

package org.jpc.debugger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpc.emulator.processor.Processor;
import org.jpc.emulator.processor.SegmentFactory;

/**
 *
 * @author Ian Preston
 */
public class RemoteGDB {

    private long[] lastRegisters = null;
    private String[] nexti = new String[2];
    public static long[] regs = null;
    private Processor cpu = null;
    private int breakpointnumber = 1;

    public void startAndConnect() {
        if (JPC.p == null) {
            try {
                Runtime.getRuntime().exec("qemu -cpu pentium2 -cdrom /home/ian/jpc/driveimages/cdrom/ubuntu-8.04.1-desktop-i386.iso -m 256 -boot d -S -s -L /home/ian/jpc/current/src/resources/bios/ -std-vga");
                Thread.currentThread().sleep(1000);
                JPC.p = Runtime.getRuntime().exec("gdb");
                JPC.input = new BufferedReader(new InputStreamReader(JPC.p.getInputStream()));
                JPC.output = new BufferedWriter(new OutputStreamWriter(JPC.p.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException f) {
            }

            try {
                JPC.output.write("target remote localhost:1234\n");
                JPC.output.write("display/i $pc\n");
                JPC.output.write("set disassembly-flavor intel\n");
                JPC.output.flush();
                for (int i = 0; i < 11; i++) {
                    System.out.println(JPC.input.readLine());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            cpu = (Processor) JPC.getObject(Processor.class);
        }
    }

    public void continueRemote() {
        try {
            JPC.output.write("c\n");
            JPC.output.flush();
//            for (int i = 0; i < 5; i++) {
//                System.out.println(JPC.input.readLine());
//            }
            while (((char) JPC.input.read()) != "?".charAt(0)) {
            }
            while (((char) JPC.input.read()) != ":".charAt(0)) {
            }
            while (((char) JPC.input.read()) != ":".charAt(0)) {
            }

        } catch (IOException ex) {
            Logger.getLogger(RemoteGDB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setRemoteTbreak(String address) {
        try {
            JPC.output.write("tbreak * " + address + " \n");
            breakpointnumber++;
            JPC.output.flush();
        } catch (IOException ex) {
            Logger.getLogger(RemoteGDB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setRemoteBreak(String address) {
        try {
            JPC.output.write("break * " + address + "\n");
            breakpointnumber++;
            JPC.output.flush();
//            for (int i = 0; i < 1; i++) {
//                System.out.println(JPC.input.readLine());
//            }
        } catch (IOException ex) {
            Logger.getLogger(RemoteGDB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void printLastRegisters() {
        System.out.println("****************");
        if (lastRegisters != null) {
            System.out.println("Previous registers:");
            for (int k = 0; k < lastRegisters.length; k++) {
                System.out.println(Long.toHexString(lastRegisters[k]));
            }
            System.out.println("****************");
        }
        System.out.println("Previous instruction:");
        System.out.println(nexti[1]);
        System.out.println("****************");
    }

    public int executeRemoteInstructions(int count) {
        String[] tempinst = new String[2];
        int nextEIP = -1;
        for (int j = 0; j < count; j++) {
            try {
                JPC.output.write("nexti\n");
                JPC.output.flush();
                String nextEipString = JPC.input.readLine();
                int start = nextEipString.indexOf("0x");
                nextEIP = (int) Long.parseLong(nextEipString.substring(start + 2, start + 10), 16);
                tempinst[0] = JPC.input.readLine();
                tempinst[1] = JPC.input.readLine();
                if ((nexti != null) && (nexti[1] != null)) {
                    System.out.println(nexti[1]);
                }
            } catch (IOException ex) {
                Logger.getLogger(RemoteGDB.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //handle repeated string operations
        while ((lastRegisters != null) && (nextEIP == regs[8])) {
            try {
                JPC.output.write("nexti\n");
                JPC.output.flush();
                String nextEip = JPC.input.readLine();
                int start = nextEip.indexOf("0x");
                nextEIP = Integer.parseInt(nextEip.substring(start + 2, start + 10), 16);
                tempinst[0] = JPC.input.readLine();
                tempinst[1] = JPC.input.readLine();
            } catch (IOException ex) {
                Logger.getLogger(RemoteGDB.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (!compareRegisters()) {
            printLastRegisters();
            throw new IllegalStateException("invalid register value ");
        }
        if ((lastRegisters != null) && (regs[21] != lastRegisters[21])) {
            throw new IllegalStateException("gdtr changed ");
        }
        nexti = tempinst;
        return (int) regs[8];
    }

    public void skipBreaks(int breaks) {
        for (int i = 0; i < breaks; i++) {
            continueRemote();
        }
    }

    public boolean getInterruptEnableFlag() {
        long[] curregs = getRemoteRegisters();
        return ((curregs[9] & 0x200) == 0x200);
    }

    public boolean isProtectedMode() {
        long[] curregs = getRemoteRegisters();
        return ((curregs[16] & 0x1) == 1);
    }

    public int getEIP() {
        return (int) regs[8];
    }

    public void deleteBreaks() {
        try {
            JPC.output.write("delete breakpoints\n");
            JPC.output.write("y\n");
            JPC.output.flush();
//            JPC.input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private Executor proxy = Executors.newSingleThreadExecutor();

    private class ExecuteInstruction implements Callable<Integer> {

        public Integer call() {
            int nextAddress = -1;
            try {
                JPC.output.write("nexti\n");
                JPC.output.flush();
                String nextEipString = JPC.input.readLine();
                System.out.println("A: " + nextEipString);
                int start = nextEipString.indexOf("0x");
                if (start == -1) {
                    return -1;
                }
                nextAddress = (int) Long.parseLong(nextEipString.substring(start + 2, start + 10), 16);
                System.out.println(JPC.input.readLine());
                System.out.println(JPC.input.readLine());
            } catch (IOException ex) {
                Logger.getLogger(RemoteGDB.class.getName()).log(Level.SEVERE, null, ex);
            }
            return nextAddress;
        }
    }

    private int executeInstruction() {
        FutureTask<Integer> result = new FutureTask<Integer>(new ExecuteInstruction());
        proxy.execute(result);
        long start = System.currentTimeMillis();
        while (!result.isDone()) {
            if (System.currentTimeMillis() - start > 5000) {
                System.out.println("Cancelling readlines");
                result.cancel(true);
                proxy = Executors.newSingleThreadExecutor();
                return -1;
            }
        }
        try {
            return result.get();
        } catch (InterruptedException ex) {
            Logger.getLogger(RemoteGDB.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        } catch (ExecutionException ex) {
            Logger.getLogger(RemoteGDB.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    public void printAddresses() {
        File out = new File("QemuAddresses.bin");
        DataOutputStream outs = null;
        try {
            outs = new DataOutputStream(new FileOutputStream(out));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(RemoteGDB.class.getName()).log(Level.SEVERE, null, ex);
        }

        //execute a certain number of instructions
        int addr;
        try {
            for (int i = 0; i < 8000000; i++) {
                addr = executeInstruction();
                if (addr != -1) {
                    outs.writeInt(addr);
                }
            }
            outs.flush();
            outs.close();
        } catch (IOException ex) {
            Logger.getLogger(RemoteGDB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int smartBreakSearch(int blocks) {
        Set<Integer> addresses = new HashSet<Integer>();

        //execute a certain number of instructions
        for (int i = 0; i < blocks; i++) {
            addresses.add(executeInstruction());
        }
        System.out.println("Done " + blocks + " blocks.");
        int nextAddress;
        do {
            //then keep going until you find an address that wasn't reached in the previous loop
            do {
                nextAddress = executeInstruction();
            } while (addresses.contains(nextAddress));
            System.out.println("Found new address: " + nextAddress);
        } while (isProtectedMode() && !getInterruptEnableFlag());
        return nextAddress;
    }

    public int getFirstAddressAfter(int instrs) {
        try {
            RandomAccessFile rnd = new RandomAccessFile(new File("QemuAddressCounts.bin"), "r");
            while (true) {
                rnd.seek((instrs - 1) * 8);
                int address = rnd.readInt();
                int count = rnd.readInt();
                if (count == 1) {
                    return address;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(RemoteGDB.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public int getFirstNewAddressAfter(int instrs, int run) {
        HashSet<Integer> counts = new HashSet<Integer>();
        try {
            File f = new File("QemuAddresses.bin");
            RandomAccessFile in = new RandomAccessFile(f, "r");
            in.seek(instrs);
            for (int i = 0; i < run; i++) {
                int address = in.readInt();
                if (!counts.contains(address)) {
                    counts.add(address);
                }
            }
            while (true) {
                int address = in.readInt();
                if (!counts.contains(address)) {
                    return address;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(RemoteGDB.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    private static long parseRegister(String reg) {
        try {
            int i = 0;
            while (i < reg.length()) {
                if (((char) JPC.input.read()) == reg.charAt(i)) {
                    i++;
                } else {
                    if (i > 0) {
                        i = 0;
                    }
                }
            }
            while (((char) JPC.input.read()) != "0".charAt(0)) {
            }
            while (((char) JPC.input.read()) != "x".charAt(0)) {
            }
            //read number
            StringBuffer buf = new StringBuffer();
            char next;
            while (Character.isLetterOrDigit(next = (char) JPC.input.read())) {
                buf.append(next);
            }
            return Long.parseLong(buf.toString(), 16);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static long[] parseRegisters() {
        long[] tmpregs = new long[27];

        try {
            tmpregs[0] = parseRegister("eax");
            tmpregs[1] = parseRegister("ecx");
            tmpregs[2] = parseRegister("edx");
            tmpregs[3] = parseRegister("ebx");
            tmpregs[4] = parseRegister("esp");
            tmpregs[5] = parseRegister("ebp");
            tmpregs[6] = parseRegister("esi");
            tmpregs[7] = parseRegister("edi");
            tmpregs[8] = parseRegister("eip");
            tmpregs[9] = parseRegister("eflags");
            tmpregs[10] = parseRegister("cs");
            tmpregs[11] = parseRegister("ss");
            tmpregs[12] = parseRegister("ds");
            tmpregs[13] = parseRegister("es");
            tmpregs[14] = parseRegister("fs");
            tmpregs[15] = parseRegister("gs");

            //control regs
            tmpregs[16] = parseRegister("raw");
            tmpregs[17] = parseRegister("raw");
            tmpregs[18] = parseRegister("raw");
            tmpregs[19] = parseRegister("raw");
            tmpregs[20] = parseRegister("raw");

            //xdtr's
            tmpregs[21] = parseRegister("fctrl");
            tmpregs[22] = parseRegister("fstat");
            tmpregs[23] = parseRegister("ftag");
            tmpregs[24] = parseRegister("fiseg");
            tmpregs[25] = parseRegister("fioff");
            tmpregs[26] = parseRegister("foseg");

            return tmpregs;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    private long[] getRemoteRegisters() {
        try {
            JPC.output.write("info all-registers\n");
            JPC.output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return parseRegisters();
    }

    public void adoptRegisters() {
        long[] newr = getRemoteRegisters();
        setRegisters(newr);
    }

    private void updateRegHistory(long[] newregs) {
        lastRegisters = regs;
        regs = newregs;
    }

    public String getDifferences() {
        StringBuffer buf = new StringBuffer();
        buf.append(getDifferentRegister(cpu.eax, regs[0], "eax"));
        buf.append(getDifferentRegister(cpu.ecx, regs[1], "ecx"));
        buf.append(getDifferentRegister(cpu.edx, regs[2], "edx"));
        buf.append(getDifferentRegister(cpu.ebx, regs[3], "ebx"));
        buf.append(getDifferentRegister(cpu.esp, regs[4], "esp"));
        buf.append(getDifferentRegister(cpu.ebp, regs[5], "ebp"));
        buf.append(getDifferentRegister(cpu.esi, regs[6], "esi"));
        buf.append(getDifferentRegister(cpu.edi, regs[7], "edi"));
        buf.append(getDifferentRegister(cpu.eip + cpu.cs.getBase(), regs[8], "eip"));
        buf.append(getDifferentRegister(cpu.cs.getSelector(), regs[10], "cs"));
        buf.append(getDifferentRegister(cpu.ss.getSelector(), regs[11], "ss"));
        buf.append(getDifferentRegister(cpu.ds.getSelector(), regs[12], "ds"));
        buf.append(getDifferentRegister(cpu.es.getSelector(), regs[13], "es"));
        buf.append(getDifferentRegister(cpu.fs.getSelector(), regs[14], "fs"));
        buf.append(getDifferentRegister(cpu.gs.getSelector(), regs[15], "gs"));
        buf.append(getDifferentRegister(cpu.getCR0(), regs[16], "cr0"));
        buf.append(getDifferentRegister(cpu.getCR2(), regs[18], "cr2"));
        buf.append(getDifferentRegister(cpu.getCR3(), regs[19], "cr3"));
        return buf.toString();
    }

    public String getDifferentRegister(long jpc, long remote, String name) {
        if ((jpc & 0x00000000FFFFFFFFL) != remote) {
            return (name + " jpc: " + Long.toHexString(jpc) + ", remote: " + Long.toHexString(remote));
        }
        return "";
    }

    public boolean compareRegisters() {
        updateRegHistory(getRemoteRegisters());
        boolean same = true;
        boolean eaxonly = false;
        same &= compareRegister(cpu.eax, regs[0], "eax");
        eaxonly = same;
        same = true;
        same &= compareRegister(cpu.ecx, regs[1], "ecx");
        same &= compareRegister(cpu.edx, regs[2], "edx");
        same &= compareRegister(cpu.ebx, regs[3], "ebx");
        same &= compareRegister(cpu.esp, regs[4], "esp");
        same &= compareRegister(cpu.ebp, regs[5], "ebp");
        same &= compareRegister(cpu.esi, regs[6], "esi");
        same &= compareRegister(cpu.edi, regs[7], "edi");
        same &= compareRegister(cpu.eip + cpu.cs.getBase(), regs[8], "eip");
        //same &= compareRegister(cpu.getEFlags(), regs[9], "eflags");
        same &= compareRegister(cpu.cs.getSelector(), regs[10], "cs");
        same &= compareRegister(cpu.ss.getSelector(), regs[11], "ss");
        same &= compareRegister(cpu.ds.getSelector(), regs[12], "ds");
        same &= compareRegister(cpu.es.getSelector(), regs[13], "es");
        same &= compareRegister(cpu.fs.getSelector(), regs[14], "fs");
        same &= compareRegister(cpu.gs.getSelector(), regs[15], "gs");
        same &= compareRegister(cpu.getCR0(), regs[16], "cr0");
        same &= compareRegister(cpu.getCR2(), regs[18], "cr2");
        same &= compareRegister(cpu.getCR3(), regs[19], "cr3");
        //same &= compareRegister(cpu.getCR4(), regs[20], "cr4");
        if ((cpu.gdtr != SegmentFactory.NULL_SEGMENT) || (regs[21] != 0)) {
            same &= compareRegister(cpu.gdtr.getBase(), regs[21], "gdt-base");
            same &= compareRegister(cpu.gdtr.getLimit(), regs[22], "gdt-limit");
        }
        if ((cpu.idtr != SegmentFactory.NULL_SEGMENT) || (regs[23] != 0)) {
            same &= compareRegister(cpu.idtr.getBase(), regs[23], "idt-base");
            same &= compareRegister(cpu.idtr.getLimit(), regs[24], "idt-limit");
        }
        if ((cpu.ldtr != SegmentFactory.NULL_SEGMENT) || (regs[25] != 0)) {
            if (cpu.ldtr == SegmentFactory.NULL_SEGMENT) {
                System.out.println("ldt-base: " + regs[25]);
                System.out.println("ldt-limit: " + regs[26]);
                same = false;
            } else {
                same &= compareRegister(cpu.ldtr.getBase(), regs[25], "ldt-base");
                same &= compareRegister(cpu.ldtr.getLimit(), regs[26], "ldt-limit");
            }
        }
        if (!same) {
            compareRegister(cpu.getEFlags(), regs[9], "eflags");
            System.out.println("*****************");
        //printLastRegisters();
        //setRegisters();
        } else {
            System.out.println("Identical Registers...");
        }
        if (!same) {
            return same;
        } else {
            if (!eaxonly) {
                if (0x46 == (int) regs[0]) {
                    setRegisters(regs);
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    public void setRegisters() {
        setRegisters(getRemoteRegisters());
    }

    public void setRegisters(long[] regs) {
        cpu.eax = (int) regs[0];
        cpu.ecx = (int) regs[1];
        cpu.edx = (int) regs[2];
        cpu.ebx = (int) regs[3];
        cpu.esp = (int) regs[4];
        cpu.ebp = (int) regs[5];
        cpu.esi = (int) regs[6];
        cpu.edi = (int) regs[7];
        cpu.eip = (int) regs[8];
    }

    private boolean compareRegister(long jpc, long remote, String name) {
        if ((jpc & 0x00000000FFFFFFFFL) != remote) {
            System.out.println(name + " jpc: " + Long.toHexString(jpc) + ", remote: " + Long.toHexString(remote));
            return false;
        }
        return true;
    }
}
