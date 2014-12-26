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
import java.net.*;
import java.lang.reflect.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class Fuzzer
{
    static String newJar = "JPCApplication.jar";
    static String oldJar = "OldJPCApplication.jar";
    public static final boolean compareFlags = true;

    public static void main(String[] args) throws Exception
    {
        URL[] urls = new URL[]{new File(newJar).toURL()};
        ClassLoader cl1 = new URLClassLoader(urls, tools.Fuzzer.class.getClassLoader());
        Class opts = cl1.loadClass("org.jpc.j2se.Option");
        Method parse = opts.getMethod("parse", String[].class);
        args = (String[])parse.invoke(opts, (Object)args);
        PCHandle pc1 = new PCHandle(cl1, true, args);

        URL[] urls2 = new URL[]{new File(oldJar).toURL()};
        ClassLoader cl2 = new URLClassLoader(urls2, tools.Fuzzer.class.getClassLoader());
        PCHandle pc2 = new PCHandle(cl2, false, args);
     
        // will succeed
        //byte[] add_ah_al = new byte[] {(byte)0, (byte)0xc4};
        //int[] input = new int[16];
        //executeCase(add_ah_al, input, pc1, pc2, false, true);

        // will fail
        //byte[] imul_bx = new byte[] {(byte)0xf7, (byte)0xeb};
        //int[] input2 = new int[16];
        //input2[0] = 0x50;
        //input2[1] = 0x19;
        //input2[9] = 0x46;
        //executeCase(imul_bx, input2, pc1, pc2, false, true);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        DefaultHandler rmhandler = new TestParser("rm", pc1, pc2, false, true);
        System.out.println("Starting Real Mode fuzzing...");
        //saxParser.parse("tests/rm.tests", rmhandler);

        // set PCs to protected mode
        pc1.setPM(true);
        pc2.setPM(true);

        DefaultHandler pmhandler = new TestParser("pm", pc1, pc2, true, true);
        System.out.println("Starting Protected Mode fuzzing...");
        saxParser.parse("tests/pm.tests", pmhandler);
    }

    public static boolean executeCase(String opclass, String disam, byte[] code, int[] initialState, PCHandle pc1, PCHandle pc2, boolean mem, boolean flags, BufferedWriter log) throws Exception
    {
        pc1.setState(initialState);
        pc2.setState(initialState);
        // load code at eip
        pc1.setCode(code);
        pc2.setCode(code);

        try {
            pc1.executeBlock();
        } catch (InvocationTargetException e) {
            e.printStackTrace(); 
            return false;
        }
        pc2.executeBlock();
        doCompare(mem, flags, pc1, pc2, initialState, opclass, disam, code, log);
        return true;
    }

    public static void doCompare(boolean mem, boolean compareFlags, PCHandle newpc, PCHandle oldpc, int[] input, String opclass, String disam, byte[] code, BufferedWriter log) throws Exception
    {
        compareStates(input, opclass, disam, code, newpc.getState(), oldpc.getState(), compareFlags);
        if (!mem)
            return;
        byte[] data1 = new byte[4096];
        byte[] data2 = new byte[4096];
        for (int i=0; i < 1024*1024; i++)
        {
            Integer l1 = newpc.savePage(new Integer(i), data1);
            Integer l2 = oldpc.savePage(new Integer(i), data2);
            if (l2 > 0)
                if (!comparePage(i, data1, data2, log))
                    printAllStates(code, input, newpc.getState(), oldpc.getState(), opclass, disam);
        }
    }

    public static boolean comparePage(int index, byte[] fast, byte[] old, BufferedWriter log) throws IOException
    {
        if (fast.length != old.length)
            throw new IllegalStateException(String.format("different page data lengths %d != %d", fast.length, old.length));
        for (int i=0; i < fast.length; i++)
            if (fast[i] != old[i])
            {
                log.write(String.format("Difference in memory state: %08x=> %02x - %02x\n", index*4096+i, fast[i], old[i]));
                
                return false;
            }
        return true;
    }

    public static String[] names = EmulatorControl.names;

    public static void printState(int[] state, BufferedWriter out) throws IOException
    {
        StringBuilder builder = new StringBuilder(4096);
        Formatter formatter=new Formatter(builder);
        arrayImpl(names, state, formatter, 0, 10);
        arrayImpl(names, state, formatter, 10, 17);
        arrayImpl(names, state, formatter, 17, 24);
        arrayImpl(names, state, formatter, 24, 30);
        arrayImpl(names, state, formatter, 30, 37);
        arrayImpl(names, state, formatter, 37, 45);
        arrayImpl(names, state, formatter, 45, names.length);
        doubleImpl(names, state, formatter, 37, 37 + 16);
        out.flush();
        out.write(builder.toString());
        out.newLine();
    }

    public static void printState(int[] state)
    {
        StringBuilder builder = new StringBuilder(4096);
        Formatter formatter=new Formatter(builder);
        arrayImpl(names, state, formatter, 0, 10);
        arrayImpl(names, state, formatter, 10, 17);
        arrayImpl(names, state, formatter, 17, 24);
        arrayImpl(names, state, formatter, 24, 30);
        arrayImpl(names, state, formatter, 30, 37);
        arrayImpl(names, state, formatter, 37, 45);
        arrayImpl(names, state, formatter, 45, names.length);
        doubleImpl(names, state, formatter, 37, 37+16);
        System.out.flush();
        System.out.println(builder);
    }

    public static void printAllStates(byte[] code, int[] input, int[] fast, int[] old, String opclass, String disam)
    {
        System.out.print("**" + disam + " == " + opclass + " =: ");
        for (int i=0; i < code.length; i++)
            System.out.printf("%02x ", code[i]);
        System.out.println();
        System.out.println("Input state:");
        printState(input);
        System.out.println("New JPC state:");
        printState(fast);
        System.out.println("Old JPC state:");
        printState(old);
    }

    public static void doubleImpl(String[] names, int[] vals, Formatter f, int start, int end)
    {
        for (int i=start; i < end; i+=2)
            f.format("[%8s] ", "ST"+(i-start)/2);
        f.format("\n");
        for (int i=start; i < end; i+=2)
            f.format("[%f] ", Double.longBitsToDouble((vals[i]&0xffffffffL) << 32 | (vals[i+1]&0xffffffffL)));
        f.format("\n");
    }

    public static void arrayImpl(String[] names, int[] vals, Formatter f, int start, int end)
    {
        for (int i=start; i < end; i++)
            f.format("[%8s] ", names[i]);
        f.format("\n");
        for (int i=start; i < end; i++)
            f.format("[%8X] ", vals[i]);
        f.format("\n");
    }

    public static void compareStates(int[] input, String opclass, String disam, byte[] code, int[] fast, int[] old, boolean compareFlags) throws Exception
    {
        if (old.length != fast.length)
            throw new IllegalArgumentException("old state length = "+old.length+", new state length = "+fast.length);
        StringBuilder b = new StringBuilder();
        for (int i=0; i < fast.length; i++)
            if (i != 9)
            {
                if (fast[i] != old[i])
                {
                    b.append(String.format("Difference: %d=%s %08x - %08x\n", i, names[i], fast[i], old[i]));
                    //continueExecution();
                }
            }
            else
            {
                if (compareFlags && ((fast[i] & FLAG_MASK) != (old[i] & FLAG_MASK)))
                {
                    b.append(String.format("Difference: %d=%s %08x - %08x\n", i, names[i], fast[i], old[i]));
                    //continueExecution();
                }
            }
        if (b.length() > 0)
        {
            printAllStates(code, input, fast, old, opclass, disam);
            System.out.println(b.toString());
        }
    }

    public static void continueExecution()
    {
        System.out.println("Ignore difference? (y/n)");
        String line = null;
        try {
            line = new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException f)
        {
            f.printStackTrace();
            System.exit(0);
        }
        if (line.equals("y"))
        {}
        else
            System.exit(0);
    }

    public static final int FLAG_MASK = -1;//~0x10;

    public static final int gdtBase = 0xfb632;
    public static byte[] gdt = new byte[] {
                    (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0,
                    (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0,
                    (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0,
                    (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0,
                    (byte)0xff, (byte)0xff, (byte)0x0, (byte)0x0,
                    (byte)0x0, (byte)0x9b, (byte)0xcf, (byte)0x0,
                    (byte)0xff, (byte)0xff, (byte)0x0, (byte)0x0,
                    (byte)0x0, (byte)0x93, (byte)0xcf, (byte)0x0,
                    (byte)0xff, (byte)0xff, (byte)0x0, (byte)0x0,
                    (byte)0x0f, (byte)0x9b, (byte)0x0, (byte)0x0,
                    (byte)0xff, (byte)0xff, (byte)0x0, (byte)0x0,
                    (byte)0x0, (byte)0x93, (byte)0x0, (byte)0x0
            };
    public static byte[] lgdt = new byte[]{(byte)0x2e, (byte)0x0f, (byte)0x01, (byte)0x16, (byte)0x2c, (byte)0xb6};
    public static int testEip = 0;
    public static int testCS = 0;

    public static Calendar start = Calendar.getInstance();

    public static class PCHandle
    {
        final Object pc;
        final Method state, setState, executeBlock, savePage, setCode;

        public PCHandle(ClassLoader cl1, boolean isNew, String[] args) throws Exception
        {
            Class c1 = cl1.loadClass("org.jpc.emulator.PC");


            Constructor ctor = c1.getConstructor(String[].class, Calendar.class);
            pc = ctor.newInstance((Object)args, start);

            Method m1 = c1.getMethod("hello");
            m1.invoke(pc);

            state = c1.getMethod("getState");
            setState = c1.getMethod("setState", int[].class);
            executeBlock = c1.getMethod("executeBlock");
            savePage = c1.getMethod("getPhysicalPage", Integer.class, byte[].class);
            setCode = c1.getMethod("setCode", byte[].class);
        }

        public void setPM(boolean pm) throws Exception
        {
            byte[] setcr0 = new byte[] {(byte)0x0f, (byte)0x22, (byte)0xc0};
            if (pm)
            {
                // setup gdt data
                /*int[] regs0 = new int[16];
                regs0[10] = 0xf000;// set cs
                regs0[8] = 0xb632; // cheat and set eip to where the gdt will be to load gdt
                setState.invoke(pc, regs0);
                setCode(gdt); // refers to BIOS - data is already there*/

                // lgdt
                int[] regs0 = new int[16];
                regs0[10] = 0xf000;// set cs
                regs0[8] = 0xb599; // set eip to point to lgdt
                setState.invoke(pc, regs0);
                //setCode(lgdt);
                executeBlock(); // relies on single instruction length block
                // set cr0
                executeBlock();
                executeBlock();
                executeBlock();
                // far jump
                executeBlock();
                // load other segments
                executeBlock();// mov eax, Iz
                executeBlock();//ds
                executeBlock();//es
                executeBlock();//ss

                // load test eip
                int[] newregs = getState();
                testEip = newregs[8];
                testCS = newregs[10];
                // load cr0
                /*int[] regs = new int[16];
                regs[0] = 0x60000011; // new cr0 value is in eax
                regs0[10] = 0xf000;// set cs
                setState.invoke(pc, regs);
                setCode(setcr0);
                executeBlock();*/
            }
            else
            {
                int[] regs = new int[16];
                regs[0] = 0x60000010;
                setState.invoke(pc, regs);
                setCode(setcr0);
                executeBlock();
            }
        }

        public void setCode(byte[] code) throws Exception
        {
            setCode.invoke(pc, (Object)code);
        }

        public int[] getState() throws Exception
        {
            return (int[]) state.invoke(pc);
        }

        public void setState(int[] s) throws Exception
        {
            setState.invoke(pc, s);
        }

        public void executeBlock() throws Exception
        {
            executeBlock.invoke(pc);
        }

        public Integer savePage(Integer page, byte[] buf) throws Exception
        {
            return (Integer)savePage.invoke(pc, page, (Object) buf);
        }
    }

    public static class TestParser extends DefaultHandler
    {
        final String mode;
        final PCHandle pc1, pc2;
        final boolean mem, flags;
        final Set<String> unimplemented = new HashSet<String>();
        byte[] currentCode;
        String currentClass;
        String currentDisam;
        enum Type {None, Class, Code, Disam, Input}
        Type type;
        int opcodeCount=0, testCount=0;
        BufferedWriter log;

        public TestParser(String mode, PCHandle pc1, PCHandle pc2, boolean mem, boolean flags)
        {
            this.mode = mode;
            this.pc1 = pc1;
            this.pc2 = pc2;
            this.mem = mem;
            this.flags = flags;
            try {
                this.log = new BufferedWriter(new FileWriter("Fuzz_" + mode + (mem ? "mem" : "")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void startElement(String uri, String localName,String qName, Attributes attributes) throws SAXException
        {
            if (qName.equals("class"))
                type = Type.Class;
            else if (qName.equals("disam"))
                type = Type.Disam;
            else if (qName.equals("code"))
                type = Type.Code;
            else if (qName.equals("input"))
                type = Type.Input;
        }

        public void characters(char ch[], int start, int length) throws SAXException
        {
            if (type == Type.Class)
                currentClass = new String(ch, start, length);
            else if (type == Type.Disam)
            {
                currentDisam = new String(ch, start, length);
                System.out.println("Starting fuzz of "+currentDisam);
            }
            else if (type == Type.Code)
            {
                String[] codeArr = new String(ch, start, length).trim().split(" ");
                currentCode = new byte[codeArr.length];
                for (int i=0; i < codeArr.length; i++)
                    currentCode[i] = (byte)Integer.parseInt(codeArr[i], 16);
            }
            else if (type == Type.Input)
            {
                if (unimplemented.contains(currentClass))
                    return;
                String[] inputArr = new String(ch, start, length).trim().split(" ");
                int[] input = new int[names.length];
                for (int i=0; i < inputArr.length; i++)
                    input[i] = Integer.parseInt(inputArr[i], 16);
                // set eip
                // eip will be 0 which is fine
                //input[8] = testEip;
                input[10] = testCS;
                input[11] = 0x18; // ds
                input[12] = 0x18; // es
                input[15] = 0x18; // ss
                // now do the test case
                try {
                    if (!executeCase(currentClass, currentDisam, currentCode, input, pc1, pc2, mem, flags, log))
                        unimplemented.add(currentClass);
                } catch (Exception e) {e.printStackTrace();}
                testCount++;
                if (testCount % 10000 == 0)
                    System.out.printf("Completed %d test cases from %d opcodes in %s\n", testCount, opcodeCount, mode);
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            type = Type.None;
        }
    }
}
