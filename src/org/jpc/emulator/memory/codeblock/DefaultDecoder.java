/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

    Copyright (C) 2011 Ian Preston

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

package org.jpc.emulator.memory.codeblock;

import org.jpc.emulator.memory.codeblock.optimised.*;
import java.lang.reflect.*;
import java.util.*;

public class DefaultDecoder
{
    public static void main(String[] args)
    {
        String hex = getArg(args, "-rm", null);
        if (hex != null)
            decodeReal(hex);
        hex = getArg(args, "-pm", null);
        if (hex != null)
        {
            boolean operandSize = Boolean.parseBoolean(getArg(args, "-op", "true"));
            decodeProtected(hex, operandSize);
        }
        hex = getArg(args, "-vm", null);
        if (hex != null)
            decodeVM8086(hex);
    }

    public static void decodeReal(String hex)
    {
        Decoder d = new RealModeUDecoder();
        byte[] x86 = hexToBytes(hex);
        InstructionSource ins = d.decodeReal(new ArrayBackedByteSource(x86), x86.length);
        printInstructions(ins);
    }

    public static void decodeProtected(String hex, boolean operandSize)
    {
        Decoder d = new ProtectedModeUDecoder();
        byte[] x86 = hexToBytes(hex);
        InstructionSource ins = d.decodeProtected(new ArrayBackedByteSource(x86), operandSize, x86.length);
        printInstructions(ins);
    }

    public static void decodeVM8086(String hex)
    {
        Decoder d = new RealModeUDecoder();
        byte[] x86 = hexToBytes(hex);
        InstructionSource ins = d.decodeReal(new ArrayBackedByteSource(x86), x86.length);
        printInstructions(ins);
    }

    public static void printInstructions(InstructionSource ins)
    {
        while (ins.getNext())
        {
            for (int j=0; j < ins.getLength(); j++)
            {
                int microcode = ins.getMicrocode();
                String name = reflectedNameCache.get(String.valueOf(microcode));
                if ("EIP_UPDATE".equals(name))
                    return;
                System.out.println(reflectedNameCache.get(String.valueOf(microcode)));
            }
        }
    }

    public static byte[] hexToBytes(String hex)
    {
        int len = hex.length();
        byte[] res = new byte[len/2];
        for (int i=0; i < len/2; i++)
        {
            res[i] = (byte) Integer.valueOf(hex.substring(2*i, 2*i+2), 16).intValue();
        }
        return res;
    }

    private static Hashtable<String, String> reflectedNameCache = new Hashtable();
    static
    {
        try
        {
            Class cls = MicrocodeSet.class;
            Field[] flds = cls.getDeclaredFields();
            int count = 0;
            for (int i=0; i<flds.length; i++)
            {
                Field f = flds[i];
                int mods = f.getModifiers();
                if (!Modifier.isPublic(mods) || !Modifier.isStatic(mods) || !Modifier.isFinal(mods))
                    continue;
                if (f.getType() != int.class)
                    continue;

                int value = f.getInt(null);
                count++;
                reflectedNameCache.put(String.valueOf(value), f.getName());
            }
        }
        catch (Throwable t) {}
    }

    public static String getArg(String[] args, String arg, String def)
    {
        for (int i=0; i < args.length; i++)
            if (args[i].equals(arg))
                return args[i+1];
        return def;
    }
}