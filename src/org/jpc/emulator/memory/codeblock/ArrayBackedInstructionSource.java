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

package org.jpc.emulator.memory.codeblock;

/**
 * Converts a pair of microcode and x86 offset arrays into an instruction source.
 * <p>
 * This is used by the <code>FASTCompiler</code> to convert the arrays backing
 * an interpreted codeblock into an instruction source.
 * @author Chris Dennis
 */
public class ArrayBackedInstructionSource implements InstructionSource {

    private int[] microcodes;
    private int[] positions;
    private int readOffset;
    private int operationEnd;
    private int operationStart;
    private int x86Start;
    private int x86End;

    /**
     * Constructs an instruction source backed by the given arrays.
     * <p>
     * Copies of these arrays are not taken, but in turn the arrays supplied are
     * not modified.  Care must be taken to avoid modifying the supplied arrays
     * in other code.
     * @param microcodes array of microcode values
     * @param positions array of x86 offsets
     */
    public ArrayBackedInstructionSource(int[] microcodes, int[] positions) {
        this.microcodes = microcodes;
        this.positions = positions;

        x86Start = 0;
        x86End = 0;
    }

    public void reset() {
        x86Start = 0;
        x86End = 0;
        readOffset=0;
        operationEnd=0;
        operationStart=0;
    }

    public boolean getNext() {
        if (operationEnd >= microcodes.length) {
            return false;
        }

        operationStart = readOffset = operationEnd++;
        x86Start = x86End;

        while ((operationEnd < microcodes.length) && (positions[operationEnd] == positions[operationEnd - 1])) {
            operationEnd++;
        }

        x86End = positions[operationEnd - 1];

        return true;
    }

    public int getMicrocode() {
        if (readOffset < operationEnd) {
            return microcodes[readOffset++];
        } else {
            throw new IllegalStateException();
        }
    }

    public int getLength() {
        return operationEnd - operationStart;
    }

    public int getX86Length() {
        return x86End - x86Start;
    }
}
