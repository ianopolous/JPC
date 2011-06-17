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
 * Converts a stream of x86 bytes into a stream of JPC instructions.
 * @author Chris Dennis
 */
public interface Decoder
{
    /**
     * Decodes a sequence of real-mode x86 bytes into an instruction stream.
     * @param source sequence of x86 bytes to decode.
     * @param limit max x86 instructions to decode.
     * @return decoded instruction stream
     */
    public InstructionSource decodeReal(ByteSource source, int limit);

    /**
     * Decodes a sequence of protected-mode x86 bytes into an instruction stream.
     * <p>
     * If the default operand size is 32-bit then the operandSize flag should be
     * set.
     * @param source sequence of x86 bytes to decode
     * @param operandSize <code>true</code> true if the default operand size is 32-bit
     * @param limit max x86 instructions to decode.
     * @return decoded instruction stream
     */
    public InstructionSource decodeProtected(ByteSource source, boolean operandSize, int limit);

    /**
     * Decodes a sequence of virtual8086-mode x86 bytes into an instruction stream.
     * @param source sequence of x86 bytes to decode.
     * @param limit max x86 instructions to decode.
     * @return decoded instruction stream
     */
    public InstructionSource decodeVirtual8086(ByteSource source, int limit);
}
