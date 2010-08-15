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

package org.jpc.emulator.memory.codeblock.fastcompiler.real;

import org.jpc.emulator.memory.codeblock.fastcompiler.BytecodeFragments;
import org.jpc.emulator.memory.codeblock.fastcompiler.UCodeMethodParser;

import static org.jpc.emulator.memory.codeblock.fastcompiler.FASTCompiler.ELEMENT_COUNT;
import static org.jpc.emulator.memory.codeblock.optimised.MicrocodeSet.MICROCODE_LIMIT;

/**
 * 
 * @author Chris Dennis
 */
public class RealModeBytecodeFragments extends BytecodeFragments
{
    private static final Object[][][] operationArray = new Object[MICROCODE_LIMIT][][];
    private static final int[][][] operandArray = new int[MICROCODE_LIMIT][ELEMENT_COUNT][];

    private static final boolean[][] externalEffectsArray = new boolean[MICROCODE_LIMIT][ELEMENT_COUNT];
    private static final boolean[][] explicitThrowArray = new boolean[MICROCODE_LIMIT][ELEMENT_COUNT];

    private RealModeBytecodeFragments()
    {
    }

    static {
        UCodeMethodParser p = new UCodeMethodParser(RealModeUCodeStaticMethods.class, operationArray, operandArray, externalEffectsArray, explicitThrowArray);
        p.parse();
    }

    public static Object[] getOperation(int element, int microcode, int x86Position)
    {
        Object[] ops = operationArray[microcode][element];
        if (ops == null)
            return null;

        Object[] temp = new Object[ops.length];
        System.arraycopy(ops, 0, temp, 0, temp.length);
        
        for (int i = 0; i < temp.length; i++) 
        {
            if (temp[i] == X86LENGTH) 
                temp[i] = integer(x86Position);
        }
        
        return temp;
    }

    public static Object[] getOperation(int element, int microcode, int x86Position, int immediate)
    {
        Object[] temp = getOperation(element, microcode, x86Position);
        if (temp == null)
            return null;

        for (int i = 0; i < temp.length; i++) 
        {
            if (temp[i] == IMMEDIATE)
                temp[i] = integer(immediate);
        }

        return temp;
    }

    public static Object[] getTargetsOf(int microcode)
    {
        return operationArray[microcode];
    }

    public static int[] getOperands(int element, int microcode)
    {
        return operandArray[microcode][element];
    }

    public static boolean hasExternalEffect(int element, int microcode)
    {
	return externalEffectsArray[microcode][element];
    }

    public static boolean hasExplicitThrow(int element, int microcode)
    {
	return explicitThrowArray[microcode][element];
    }
}
