package tools;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

/**
 * User: Ian Preston
 */
public class History
{
    static String newJar = "JPCApplication.jar";

    public static void main(String[] args) throws Exception
    {
        String[] pcargs = Comparison.pcargs;
        URL[] urls1 = new URL[]{new File(newJar).toURL()};
        ClassLoader cl1 = new URLClassLoader(urls1, Comparison.class.getClassLoader());

        Class opts = cl1.loadClass("org.jpc.j2se.Option");
        Method parse = opts.getMethod("parse", String[].class);
        args = (String[]) parse.invoke(opts, (Object)args);


        Calendar start = Calendar.getInstance();
        Class c1 = cl1.loadClass("org.jpc.emulator.PC");
        Constructor ctor = c1.getConstructor(String[].class, Calendar.class);
        Object newpc = ctor.newInstance((Object)pcargs, start);


        Method m1 = c1.getMethod("hello");
        m1.invoke(newpc);

        Method state1 = c1.getMethod("getState");
        Method setState1 = c1.getMethod("setState", int[].class);
        Method execute1 = c1.getMethod("executeBlock");
        Method save1 = c1.getMethod("savePage", Integer.class, byte[].class, Boolean.class);
        Method startClock1 = c1.getMethod("start");
        startClock1.invoke(newpc);
        Method break1 = c1.getMethod("eipBreak", Integer.class);
        Method instructionInfo = c1.getMethod("getInstructionInfo", Integer.class);

        // setup screen from new JPC
        JPanel screen = (JPanel)c1.getMethod("getNewMonitor").invoke(newpc);
        JFrame frame = new JFrame();
        frame.getContentPane().add("Center", new JScrollPane(screen));
        frame.validate();
        frame.setVisible(true);
        frame.setBounds(100, 100, 760, 500);

        String line = null;
        int skip = 0;
        byte[] sdata1 = new byte[4096];
        byte[] sdata2 = new byte[4096];
        while (true)
        {
            try {
            execute1.invoke(newpc);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                printHistory();
                System.exit(0);
            }
            int[] fast = (int[])state1.invoke(newpc);
            try {
                line = (String) instructionInfo.invoke(newpc, new Integer(1)); // instructions per block
            } catch (Exception e)
            {
                System.out.println("Error getting instruction info..");
                e.printStackTrace();
                printHistory();
                System.out.println("******************");
                printState(new Object[] {fast, "Error at above state..."});
                System.out.println("******************");
                System.exit(0);
            }
            if (history[historyIndex] == null)
                history[historyIndex] = new Object[2];
            history[historyIndex][0] = fast;
            history[historyIndex][1] = line;
            historyIndex = (historyIndex+1)%history.length;
            Set<Integer> diff = new HashSet<Integer>();
            /*{
                int ssBase = fast[15] << 4; // real mode only
                int esp = fast[6] + ssBase;
                int espPageIndex = esp >> 12;

                Integer sl1 = (Integer)save1.invoke(newpc, new Integer(espPageIndex), sdata1);
                Integer sl2 = (Integer)save2.invoke(oldpc, new Integer(espPageIndex), sdata2);
                if (sl2 > 0)
                    if (!samePage(espPageIndex, sdata1, sdata2))
                    {
                        printHistory();
                        System.out.println("Error here... look above");
                        printPage(sdata1, sdata2, esp);
                    }
            }*/
        }
    }

    static Object[][] history = new Object[1024][];
    static int historyIndex=0;

    private static void printHistory()
    {
        printState(history[historyIndex]);
        int end = historyIndex;
        for (int j = (end+1)%history.length; j != end ; j = (j+1)%history.length)
        {
            printState(history[j]);
        }
    }

    private static void printState(Object s)
    {
        if (s == null)
            return;
        Object[] sarr = (Object[]) s;
        int[] fast = (int[]) sarr[0];
        String line = (String) sarr[1];
        Fuzzer.printState(fast);
        System.out.println(line);
    }

    public static void printPage(byte[] fast, byte[] old, int esp)
    {
        int address = esp&0xfffff000;
        // print page
        for (int i=0; i < 1 << 8; i++)
        {
            int v1 = getInt(fast, 16*i);
            int v2 = getInt(fast, 16*i+4);
            int v3 = getInt(fast, 16*i+8);
            int v4 = getInt(fast, 16*i+12);
            int r1 = getInt(old, 16*i);
            int r2 = getInt(old, 16*i+4);
            int r3 = getInt(old, 16*i+8);
            int r4 = getInt(old, 16*i+12);

            System.out.printf("0x%8x:  %8x %8x %8x %8x -- %8x %8x %8x %8x ==== ", address + 16*i, v1, v2, v3, v4, r1, r2, r3, r4);
            printIntChars(v1, r1);
            printIntChars(v2, r2);
            printIntChars(v3, r3);
            printIntChars(v4, r4);
            System.out.print(" -- ");
            printIntChars(r1, v1);
            printIntChars(r2, v2);
            printIntChars(r3, v3);
            printIntChars(r4, v4);
            System.out.println();
        }

        // print differences
        for (int i =0; i < 1<< 12; i++)
        {
            byte b1 = fast[i];
            byte b2 = old[i];
            if (b1 != b2)
            {
                System.out.println("Memory difference at 0x" + Integer.toHexString(address+i) + ", values: " + Integer.toHexString(b1 & 0xff) + " " + Integer.toHexString(b2 & 0xff));
            }
        }
    }

    public static int getInt(byte[] data, int offset)
    {
        return data[offset] & 0xff | ((data[offset+1] & 0xff) << 8)  | ((data[offset+2] & 0xff) << 16)  | ((data[offset+1] & 0xff) << 24);
    }

    public static void printIntChars(int i, int c)
    {
        int[] ia = new int[] {(i & 0xFF), ((i >> 8) & 0xFF), ((i >> 16) & 0xFF), ((i >> 24) & 0xFF)};
        int[] ca = new int[] {(c & 0xFF), ((c >> 8) & 0xFF), ((c >> 16) & 0xFF), ((c >> 24) & 0xFF)};

        for (int a = 0; a < 4; a++)
            if (ia[a] == ca[a])
                System.out.printf("%c", (ia[a] == 0 ? ' ' : (char)ia[a]));
            else
                System.out.printf("\u001b[1;44m%c\u001b[1;49m", (ia[a] == 0 ? ' ' : (char)ia[a]));
        System.out.printf(" ");
    }
}
