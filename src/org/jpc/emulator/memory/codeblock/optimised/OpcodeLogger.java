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

package org.jpc.emulator.memory.codeblock.optimised;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Hashtable;

/**Logs frequencies of opcodes and periodically prints the results
 *
 * @author Ian Preston
 */
public class OpcodeLogger {

    int[] opcodeCounts = new int[MicrocodeSet.MICROCODE_LIMIT];
    int count = 0;
    int MAX = 5000000;
    private String name;

    OpcodeLogger(String name) {
        this.name = name;
    }

    public boolean hasImmediate(int opcode)
    {
        if ((opcode == 3) | (opcode == 8) | (opcode == 13)
                | (opcode == 26) | (opcode == 27) | (opcode == 49)
                | (opcode == 168) | (opcode == 229) | (opcode == 255))
           return true;
        else
           return false;
    }

    public void addBlock(int[] microcodes)
    {
        boolean IM  = false;
        for (int j=0; j < microcodes.length; j++)
        {
            if (!IM)
            {
                addOpcode(microcodes[j]);
                if (hasImmediate(microcodes[j]))
                    IM = true;
            } else
                IM = false;
        }
    }

    public void addOpcode(int opcode) {
        opcodeCounts[opcode]++;
        count++;
        if (count >= MAX)
        {
            printStats();
            count = 0;
        }
    }

    public void printStats()
    {
        System.out.println("*******************************");
        System.out.println(name);
        for (int i=0; i < opcodeCounts.length; i++)
        {
            if (opcodeCounts[i] > 0)
            {
                System.out.println(reflectedNameCache.get(String.valueOf(i)) + ": " + opcodeCounts[i]);
            }
        }
    }

    private static Hashtable reflectedNameCache = new Hashtable();
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
}
