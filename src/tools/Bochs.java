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

package tools;

import java.io.*;
import java.util.*;

public class Bochs extends EmulatorControl
{
    public static final String EXE = "/home/ian/jpc/bochs/bochs-2.6.1/bochs";
    public static final boolean PRINT = false;
    private static int lineCount = 0;
    public static String[] names2 = new String[]
        {
            "eax", "ecx", "edx", "ebx", "esp", "ebp", "esi", "edi","eip", "flags",
            /*10*/"es", "cs", "ss", "ds", "fs", "gs", "ticks",
            /*17*/"es-lim", "cs-lim", "ss-lim", "ds-lim", "fs-lim", "gs-lim", "cs-prop",
            /*24*/"gdtrbase", "gdtr-lim", "idtrbase", "idtr-lim", "ldtrbase", "ldtr-lim",
            /*30*/"es-base", "cs-base", "ss-base", "ds-base", "fs-base", "gs-base",
            /*36*/"cr0",
            /*37*/"ST0H", "ST0L","ST1H", "ST1L","ST2H", "ST2L","ST3H", "ST3L",
            /*45*/"ST4H", "ST4L","ST5H", "ST5L","ST6H", "ST6L","ST7H", "ST7L",
            //"expiry"
        };
    final Process p;
    final BufferedReader in;
    final BufferedWriter out;

    public Bochs() throws IOException
    {
        this("prince.cfg");
    }

    public Bochs(String config) throws IOException
    {
        ProcessBuilder pb =  new ProcessBuilder(EXE, "-q", "-f", config);
        Map<String, String> env = pb.environment();
        // force path to /home/ian/java/jni/bochs
        pb.directory(new File("/home/ian/jpc/bochs"));
        pb.redirectErrorStream(true);
        p = pb.start();
        in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
        // read startup
        String end = readLine();
        while (!end.contains("e05b") && !end.contains("2000")) // my hacked BIOS jumps to 0000:2000
            end = readLine();
        readLine(); // last line
    }

    public String executeInstruction() throws IOException
    {
        writeCommand("s");
        String pream = readLine();
        String next = readLine(); // normally cs:eip, disam of next instruction, raw bytes (very useful)
        String end = readLine();
        if (next.contains("Next at"))
            next = end + next;
        while (!end.contains("Mouse capture off"))
        {
            next += end;
            end = readLine();
        }
        return next + pream;
    }

    public int[] getState() throws IOException
    {
        writeCommand("r");
        int[] regs = new int[names2.length];
        for (int i=0; i < 10; i++)
            regs[i] = parseReg(readLine(), 8);
        readLine();
        // segment registers
        writeCommand("sreg");
        for (int i=0; i < 6; i++)
        {
            String line1 = readLine();
            regs[10+i] = parseReg(line1, 4); // selector
            if (i == 1) // cs, extract size
            {
                regs[23] = (parseReg(line1.substring(line1.indexOf("dh")), 8) & 0x00400000) >> 22;
            }

            if (!line1.contains("valid=0"))
            {
                String line2 = readLine();
                if (!line2.contains("limit"))
                    throw new IllegalStateException(line2 + ",i="+i+" doesn't contain limit, prev line="+line1);
                regs[17+i] = parseReg(line2.substring(line2.indexOf("limit")), 8); // limit
                regs[30+i] = parseReg(line2, 8); // base
            }
            else
            {
                regs[17+i] = 0xffff; // limit (technically meaningless here)
                regs[30+i] = 0x0; // base (technically meaningless here)
            }
        }
        String lline = readLine(); // ldtr
        int lhigh = parseReg(lline.substring(lline.indexOf("dh")), 8);
        int llow = parseReg(lline.substring(lline.indexOf("dl")), 8);
        regs[28] = getBase(lhigh, llow); // base
        regs[29] = getLimit(lhigh, llow);
        readLine(); // tr
        String gline = readLine(); // gdtr
        regs[24] = parseReg(gline, 8); // base
        regs[25] = parseReg(gline.substring(gline.indexOf("limit")), 4);
        String iline = readLine(); // idtr
        regs[26] = parseReg(iline, 8); // base
        regs[27] = parseReg(iline.substring(iline.indexOf("limit")), 4);
        readLine();
        // control regs
        writeCommand("creg");
        regs[36] = parseReg(readLine(), 8);
        String last = readLine(); // CR2, CR3, CR3, CR3, CR4, EFER
        while (!last.contains("Mouse capture off"))
            last = readLine();
        // ticks
        writeCommand("ptime");
        String time = readLine();
        regs[16] = Integer.parseInt(time.substring(time.indexOf("ptime:")+7));
        String end = readLine();
        while (!end.contains("Mouse capture off"))
            end = readLine();
        // get FPU stack
        writeCommand("fpu");
        String fline;
        fline = readLine();
        int status=0;
        if (fline.contains("status")) {
            int start = fline.indexOf("0x") + 2;
            status = Integer.parseInt(fline.substring(start, fline.indexOf(":", start)), 16);
        }
        while (!(fline = readLine()).contains("fds"));
        for (int i=0; i < 8; i++)
        {
            int findex = (i - ((status >> 11) & 7)) & 7;
            fline = readLine();
            long val = parseFPUReg(fline);
            regs[37+2*findex] = (int)(val >> 32);
            regs[37+2*findex+1] = (int)val;
        }
        readLine(); // Mouse capture off
        //print(regs);
        return regs;
    }

    public void setPhysicalMemory(int addr, byte[] data) throws IOException
    {
        for (int i=0; i < data.length; i++)
        {
            writeCommand(String.format("setpmem 0x%x 1 0x%x", addr+i, data[i]));
            String line = readLine();
            while (!line.contains("Mouse capture"))
                line = readLine();
        }
    }

    public byte[] getCMOS() throws IOException
    {
        writeCommand("info device \"cmos\"");
        String line;
        while (!(line=readLine()).contains("Index register"));
        readLine();
        byte[] res = new byte[128];
        for (int i=0; i < 8; i++)
        {
            line = readLine();
            for (int j=0; j < 16; j++)
            {
                res[i*16+j] = (byte) Integer.parseInt(line.substring(6+j*3, 8+j*3), 16);
            }
        }
        String end = readLine();
        while (!end.contains("Mouse capture off"))
            end = readLine();
        return res;
    }

    public int[] getPit() throws IOException
    {
        int[] state = new int[3*4];
        getPit(state, 0);
        getPit(state, 1);
        getPit(state, 2);
        return state;
    }

    @Override
    public int getPITIntTargetEIP() throws IOException {
        writeCommand("info ivt 8");
        String line =readLine();
        try {
            while (!line.contains("INT# 08"))
            {
                if (line.contains("protected"))
                {
                    //need to lookup idt

                    return 0;
                }
                line = readLine();
            }
        } finally {
            String end = readLine();
            while (!end.contains("Mouse capture off"))
                end = readLine();
        }
        int start = line.indexOf(":", line.indexOf("INT"))+1;
        int last = line.indexOf(" ", start);

        return Integer.parseInt(line.substring(start, last), 16);
    }

    private void getPit(int[] state, int channel) throws IOException
    {
        writeCommand("info device 'pit' 'counter="+channel+"'");
        String line;
        while (!(line=readLine()).contains("counter"));
        line = readLine();
        int count = Integer.parseInt(line.substring(7));
        readLine();
        int gate = Integer.parseInt(readLine().substring(14));
        int out = Integer.parseInt(readLine().substring(13));
        int nextChangeTime = Integer.parseInt(readLine().substring(18));
        String end = readLine();
        while (!end.contains("Mouse capture off"))
            end = readLine();
        state[4*channel] = count;
        state[4*channel+1] = gate;
        state[4*channel+2] = out;
        state[4*channel+3] = nextChangeTime;
    }

    public void keysDown(String keys)
    {
        throw new IllegalStateException("Unimplemented keysDown");
    }

    public void keysUp(String keys)
    {
        throw new IllegalStateException("Unimplemented keysUp");
    }

    public void sendMouse(Integer dx, Integer dy, Integer dz, Integer buttons)
    {
        throw new IllegalStateException("Unimplemented sendMouse");
    }

    public String disam(byte[] code, Integer ops, Boolean mode)
    {
        throw new IllegalStateException("Unimplemented sendMouse");
    }

    public int x86Length(byte[] code, Boolean mode)
    {
        throw new IllegalStateException("Unimplemented sendMouse");
    }

    public Integer getPhysicalPage(Integer page, byte[] data) throws IOException
    {
        return getPage("xp/4096bx ", page, data);
    }

    public Integer getLinearPage(Integer page, byte[] data) throws IOException
    {
        return getPage("x/4096bx ", page, data);
    }

    public Integer getPage(String command, Integer page, byte[] data) throws IOException
    {
        writeCommand(command + page);
        String line = readLine();
        while (!line.contains("bogus"))
            line = readLine();
        for (int i=0; i < 512; i++)
        {
            while (line.contains("MEM"))
                line = readLine();
            if (line.contains("read error"))
                return 0;
            if (line.contains("physical address not available"))
            {
                System.out.printf("Error reading from linear address: %x\n", page);
                return 0;
            }
            String[] bytes = line.substring(line.indexOf(":")+1).trim().split("\t");
            for (int j=0; j < 8; j++)
                data[8*i+j] = (byte)Integer.parseInt(bytes[j].substring(2), 16);
            line = readLine();
        }
        while (!line.contains("Mouse capture off"))
            line = readLine();
        return 4096;
    }

    private String readLine() throws IOException
    {
        String line = in.readLine();
        if (PRINT)
            System.out.println((lineCount++) + " " + line);
        return line;
    }

    private void writeCommand(String c) throws IOException
    {
        out.write(c);
        out.newLine();
        out.flush();
    }

    public static int getBase(int high, int low)
    {
        return ((low >> 16) & 0xffff) | ((high & 0xff) << 16) | (high & 0xff000000);
    }

    public static int getLimit(int high, int low)
    {
        return (low & 0xffff) | (high & 0xf0000);
    }

    public static void print(int[] r)
    {
        for (int i=0; i < r.length; i++)
            System.out.printf("%s %08x\n", names2[i], r[i]);
    }

    public static long parseFPUReg(String line)
    {
        int start = line.indexOf(":", line.indexOf("raw")) + 1;
        long sign = Integer.parseInt(line.substring(start-5, start-4), 16) >= 8 ? 1 : 0;
        long exponent = Long.parseLong(line.substring(start - 5, start - 1), 16);
        exponent -= (0x3fff - 0x3ff);
        exponent &= 0x7ff;
        long fraction = Long.parseLong(line.substring(start, start + 14), 16);
        fraction >>= 3;
        return (sign << 63) | (exponent << 52) | (fraction & 0xfffffffffffffL);
    }

    public static int parseReg(String line, int size)
    {
        int start = line.indexOf("0x");
        int end = start+2+size;
        if (end > line.length())
            end = line.length();
        try {
        return (int)Long.parseLong(line.substring(start+2, end), 16);
        } catch (StringIndexOutOfBoundsException e)
        {
            System.out.println("Input string for bochs reg parse: "+line+"*");
            throw e;
        }
    }

    public void destroy()
    {
        try {
            writeCommand("q");
            in.close();
            out.close();
            p.getInputStream().close();
            p.destroy();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException
    {
        Bochs b = new Bochs();
        print(b.getState());
        while (true)
            b.executeInstruction();
    }
}
