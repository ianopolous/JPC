package org.jpc.j2se;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * User: Ian Preston
 */
public class OpcodeGenerator
{
    static String PATH = "org/jpc/emulator/execution/opcodes/";
    static String[] opcode_names = new String[] {""};

    public static void main(String[] args) throws IOException
    {
        for (String name: opcode_names)
        {

        }
    }

    public static void writeClass(String text, String name) throws IOException
    {
        BufferedWriter w = new BufferedWriter(new FileWriter(PATH+name+".java"));
        w.write(text);
        w.flush();
        w.close();
    }
}
