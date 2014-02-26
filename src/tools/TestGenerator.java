package tools;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.jpc.emulator.execution.decoder.*;

public class TestGenerator
{
    public static void main(String[] args) throws Exception
    {
        BufferedReader disam = new BufferedReader(new FileReader(args[0]));
        BufferedWriter cases = new BufferedWriter(new FileWriter(args[0]+".cases"));
        boolean random = args.length > 1 && args[1].equals("-random");
        String line;
        while ((line = disam.readLine()) != null)
        {
            String[] parts = line.split(";");
            boolean is32Bit = parts[0].endsWith("32");
            String cname = parts[1];
            String[] hex = parts[2].trim().split(" ");
            byte[] raw = new byte[hex.length];
            for (int i=0; i < raw.length; i++)
                raw[i] = (byte) Integer.parseInt(hex[i], 16);
            if (cname.contains("hlt") || cname.contains("in_") || cname.contains("out_") || cname.contains("ins_") || cname.contains("outs_"))
                continue;
            generateCases(is32Bit, cname, raw, cases, random);
        }
    }

    private static void generateCases(boolean is32Bit, String cname, byte[] raw, BufferedWriter out, boolean random) throws IOException
    {
        int mode = OracleFuzzer.RM;
        if (cname.contains("pm."))
            mode = OracleFuzzer.PM;
        if (cname.contains("vm."))
            mode = OracleFuzzer.VM;
        int[] inputState = getInputState(is32Bit, mode, cname, random);

        out.append(String.format("%08x %08x %08x %s\n", mode, 1, 0xffffffff, is32Bit ? "32" : "16"));
        for (byte b : raw)
            out.append(String.format("%02x ", b));
        out.newLine();
        for (int i=0; i < inputState.length; i++)
            out.append(String.format("%08x ", inputState[i]));
        // memory input values

        out.append("\n*****\n");
        out.flush();
    }

    private static int[] getInputState(boolean is32Bit, int mode, String cname, boolean random)
    {
        if (mode == OracleFuzzer.PM)
            return OracleFuzzer.getCanonicalProtectedModeInput(OracleFuzzer.codeEIP, is32Bit, random);
        if (mode == OracleFuzzer.VM)
            return OracleFuzzer.getCanonicalVM86ModeInput(OracleFuzzer.codeEIP, random);
        return OracleFuzzer.getCanonicalRealModeInput(OracleFuzzer.codeEIP, random);
    }
}
