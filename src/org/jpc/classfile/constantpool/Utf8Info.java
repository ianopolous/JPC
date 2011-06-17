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
 * Constant pool element containing a UTF8 string constant.
 * @author Mike Moleschi
 */
public class Utf8Info extends ConstantPoolInfo
{
    private final String value;

    Utf8Info(DataInputStream in) throws IOException
    {
        this(in.readUTF());
    }

    /**
     * Constructs a constant pool element containing the given
     * <code>String</code>.
     * @param val constant <code>String</code> value
     */
    public Utf8Info(String val)
    {
        super();
        value = val;
        hashCode = (Utf8Info.class.hashCode() * 31) ^ (value.hashCode() * 37);
    }

    /**
     * Returns the elements <code>String</code> constant value.
     * @return <code>String</code> value
     */
    public String getString()
    {
        return value;
    }

    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(UTF8);
        out.writeUTF(value);
    }

    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof Utf8Info)) return false;

        return getString().equals(((Utf8Info) obj).getString());
    }

    public String toString()
    {
        return "CONSTANT_Utf8_info : value=" + getString();
    }
}
