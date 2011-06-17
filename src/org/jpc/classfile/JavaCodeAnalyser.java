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

package org.jpc.classfile;

/**
 * Provides static methods to calculate the maximal stack depth and local
 * variable usage of a given set of bytecode.
 * @author Mike Moleschi
 * @author Chris Dennis
 */
public class JavaCodeAnalyser
{
    /**
     * Returns the maximum stack depth with the given limits on the code.
     * 
     * This algorithm works for forward jumps and branches only.  If the
     * code is malformed incorrect values will result and verification will
     * fail on class loading.  The code would be wrong anyway so this
     * doesn't matter.
     * @param code array of bytecodes
     * @param start beginning offset
     * @param end ending offset
     * @param cf associated class file
     * @return maximum stack depth
     */
    public static int getMaxStackDepth(byte[] code, int start, int end, ClassFile cf)
    {
	int[] depth = new int[code.length];
           
	int maxDepth = 0;
        for (int i = start, currentDepth = 0; i < end; i += JavaOpcode.getOpcodeLength(code, i)) {
            currentDepth += JavaOpcode.getStackDelta(code, i, cf);

            depth[i] = currentDepth;

            maxDepth = Math.max(maxDepth, currentDepth);

            if (JavaOpcode.isReturn(code[i])) {
                i += JavaOpcode.getOpcodeLength(code, i);

                if (i >= depth.length)
                    break;

                currentDepth = depth[i];
            } else {
                int jump = JavaOpcode.getJumpOffset(code, i);
                if (jump != 0) {
                    int target = i + jump;

                    depth[target] = currentDepth + JavaOpcode.getStackDelta(code, target, cf);

                    if (JavaOpcode.isJumpInstruction(code[i])) {
                        i += JavaOpcode.getOpcodeLength(code, i);
                        currentDepth = depth[i];
                    }
                }
            }
        }

        return maxDepth;
    }


    /**
     * Returns the number of local variable slots used in this code.
     * @param code array of bytecodes
     * @return local variable slots used
     */
    public static int getMaxLocalVariables(byte[] code)
    {
        int currentMax = 0;

        for (int pc = 0; pc < code.length; pc += JavaOpcode.getOpcodeLength(code, pc))
            currentMax = Math.max(currentMax, JavaOpcode.getLocalVariableAccess(code, pc));
        
        // add one to give size
        return currentMax + 1;
    }

    private JavaCodeAnalyser()
    {
    }
}
