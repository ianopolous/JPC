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

import java.io.*;

/**
 * Abstract superclass of all constant pool entry objects.
 * @author Mike Moleschi
 */
public abstract class ConstantPoolInfo
{
    static final int UTF8 = 1;
    static final int INTEGER = 3;
    static final int FLOAT = 4;
    static final int LONG = 5;
    static final int DOUBLE = 6;
    static final int CLASS = 7;
    static final int STRING = 8;
    static final int FIELDREF = 9;
    static final int METHODREF = 10;
    static final int INTERFACEMETHODREF = 11;
    static final int NAMEANDTYPE = 12;

    /**
     * Hashcode value set by a subclass and returned by this classes final
     * method.
     */
    protected int hashCode;
    
    public final int hashCode()
    {
        return hashCode;
    }

    /**
     * Write this constant pool element to the supplied <code>DataOutputStream</code>.
     * @param out stream to write to
     * @throws java.io.IOException on an underlying stream error
     */
    public abstract void write(DataOutputStream out) throws IOException;

    public abstract boolean equals(Object obj);

    /**
     * Reads the supplied input stream and constructs an appropriate
     * <code>ConstantPoolInfo</code> subclass.
     * @param in stream to read from
     * @return relevant <code>ConstantPoolInfo</code> subclass
     * @throws java.io.IOException on an underlying stream error.
     */
    public static ConstantPoolInfo construct(DataInputStream in) throws IOException
    {
        switch (in.readUnsignedByte())
        {
        case CLASS:
            return new ClassInfo(in);
        case FIELDREF:
            return new FieldRefInfo(in);
        case METHODREF:
            return new MethodRefInfo(in);
        case INTERFACEMETHODREF:
            return new InterfaceMethodRefInfo(in);
        case STRING:
            return new StringInfo(in);
        case INTEGER:
            return new IntegerInfo(in);
        case FLOAT:
            return new FloatInfo(in);
        case LONG:
            return new LongInfo(in);
        case DOUBLE:
            return new DoubleInfo(in);
        case NAMEANDTYPE:
            return new NameAndTypeInfo(in);
        case UTF8:
            return new Utf8Info(in);
        }
        return null;
    }
}
