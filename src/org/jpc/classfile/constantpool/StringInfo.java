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

package org.jpc.classfile.constantpool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Constant pool element representing a <code>String</code> constant and
 * referencing a UTF8 element for its contents.
 * @author Mike Moleschi
 */
public class StringInfo extends ConstantPoolInfo
{
    private final int utf8Index;

    StringInfo(DataInputStream in) throws IOException
    {
        this(in.readUnsignedShort());
    }

    /**
     * Constructs a string element referencing the given UTF8 constant pool element
     * @param val UTF8 constant pool index
     */
    public StringInfo(int val)
    {
        super();
        utf8Index = val;
        hashCode = (StringInfo.class.hashCode() * 31) ^ (utf8Index * 37);
    }

    /**
     * Returns the associated UTF8 constant pool index
     * @return UTF8 constant pool index
     */
    public int getUtf8Index()
    {
        return utf8Index;
    }

    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(STRING);
        out.writeShort(utf8Index);
    }

    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof StringInfo)) return false;

        return getUtf8Index() == ((StringInfo) obj).getUtf8Index();
    }

    public String toString()
    {
        return "CONSTANT_String_info : string=" + getUtf8Index();
    }
}
