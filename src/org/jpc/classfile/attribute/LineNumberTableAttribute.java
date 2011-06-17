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

package org.jpc.classfile.attribute;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Code attribute representing the mapping between the bytecode of a
 * particular code attribute and the associated source file line numbers.
 * @author Mike Moleschi
 */
public class LineNumberTableAttribute extends AttributeInfo
{
    private LineNumberEntry[] lineNumberTable;

    LineNumberTableAttribute(DataInputStream in, int index) throws IOException
    {
        super(in, index);
        lineNumberTable = new LineNumberEntry[in.readUnsignedShort()];
        for (int i = 0; i < lineNumberTable.length; i++) lineNumberTable[i] = new LineNumberEntry(in);
    }

    public void write(DataOutputStream out) throws IOException
    {
        super.write(out);
        out.writeShort(lineNumberTable.length);
        for (LineNumberEntry l : lineNumberTable)
            l.write(out);
    }

    private static class LineNumberEntry
    {
        private int startPC;
        private int lineNumber;

        LineNumberEntry(DataInputStream in) throws IOException
        {
            super();
            startPC = in.readUnsignedShort();
            lineNumber = in.readUnsignedShort();
        }

        void write(DataOutputStream out) throws IOException
        {
            out.writeShort(startPC);
            out.writeShort(lineNumber);
        }
    }
}
