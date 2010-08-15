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
 * Constant pool element for a name-and-type info reference.
 * 
 * The referenced name will be a field or method name.  The referenced type will
 * be a field or method type descriptor.
 * @author Mike Moleschi
 */
public class NameAndTypeInfo extends ConstantPoolInfo
{
    private final int nameIndex;
    private final int descriptorIndex;

    NameAndTypeInfo(DataInputStream in) throws IOException
    {
        this(in.readUnsignedShort(), in.readUnsignedShort());
    }

    /**
     * Constructs a name-and-type reference with the given name and descriptor
     * indices
     * @param nameIndex name constant pool index
     * @param descriptorIndex descriptor constant pool index
     */
    public NameAndTypeInfo(int nameIndex, int descriptorIndex)
    {
        super();
        this.nameIndex = nameIndex;
        this.descriptorIndex = descriptorIndex;
        hashCode = (NameAndTypeInfo.class.hashCode() * 31) ^ (nameIndex * 37) ^ (descriptorIndex * 41);
    }

    /**
     * Returns the name element constant pool index
     * @return name constant pool index
     */
    public int getNameIndex()
    {
        return nameIndex;
    }

    /**
     * Returns the descriptor element constant pool index
     * @return descriptor constant pool index
     */
    public int getDescriptorIndex()
    {
        return descriptorIndex;
    }

    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(NAMEANDTYPE);
        out.writeShort(nameIndex);
        out.writeShort(descriptorIndex);
    }

    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof NameAndTypeInfo)) return false;

        return (getNameIndex() == ((NameAndTypeInfo) obj).getNameIndex()) && (getDescriptorIndex() == ((NameAndTypeInfo) obj).getDescriptorIndex());
    }

    public String toString()
    {
        return "CONSTANT_NameAndType_info : descriptor=" + getDescriptorIndex() + " : name=" + getNameIndex();
    }
}
