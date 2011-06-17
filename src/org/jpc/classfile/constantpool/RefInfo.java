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
 * Common superclass for field and method reference constant pool elements.
 * @author Mike Moleschi
 */
public abstract class RefInfo extends ConstantPoolInfo
{
    private final int classIndex;
    private final int nameAndTypeIndex;

    RefInfo(DataInputStream in) throws IOException
    {
        this(in.readUnsignedShort(), in.readUnsignedShort());
    }

    /**
     * Constructs a reference with the given class and name-and-type element
     * indices.
     * @param classIndex class reference index
     * @param nameAndTypeIndex name-and-type reference index
     */
    RefInfo(int classIndex, int nameAndTypeIndex)
    {
        this.classIndex = classIndex;
        this.nameAndTypeIndex = nameAndTypeIndex;
        hashCode = (RefInfo.class.hashCode() * 31) ^ (classIndex * 37) ^ (nameAndTypeIndex * 41);
    }

    /**
     * Returns the class reference constant pool index.
     * @return class reference index
     */
    public int getClassIndex()
    {
        return classIndex;
    }

    /**
     * Returns the name-and-type reference constant pool index
     * @return name-and-type reference index
     */
    public int getNameAndTypeIndex()
    {
        return nameAndTypeIndex;
    }

    abstract int getTag();

    public void write(DataOutputStream out) throws IOException
    {
        out.writeByte(getTag());
        out.writeShort(classIndex);
        out.writeShort(nameAndTypeIndex);
    }

    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;

        if (!(obj instanceof RefInfo))
            return false;

        return (getClassIndex() == ((RefInfo) obj).getClassIndex()) && (getNameAndTypeIndex() == ((RefInfo) obj).getNameAndTypeIndex());
    }
}
