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

import org.jpc.classfile.constantpool.ConstantPoolInfo;
import org.jpc.classfile.attribute.AttributeInfo;
import java.io.*;

/**
 * 
 * @author Mike Moleschi
 */
class FieldInfo
{
    private int accessFlags;
    private int nameIndex;
    private int descriptorIndex;
    private AttributeInfo[] attributes;

    static final int PUBLIC = 0x0001;
    static final int PRIVATE = 0x0002;
    static final int PROTECTED = 0x0004;
    static final int STATIC = 0x0008;
    static final int FINAL = 0x0010;
    static final int VOLATILE = 0x0040;
    static final int TRANSIENT = 0x0080;
    static final int SYNTHETIC = 0x1000;
    static final int ENUM = 0x4000;


    FieldInfo(DataInputStream in, ConstantPoolInfo[] pool) throws IOException
    {
        accessFlags = in.readUnsignedShort();
        nameIndex = in.readUnsignedShort();
        descriptorIndex = in.readUnsignedShort();

        attributes = new AttributeInfo[in.readUnsignedShort()];
        for(int i = 0; i < attributes.length; i++)
            attributes[i] = AttributeInfo.construct(in, pool);
    }

    void write(DataOutputStream out) throws IOException
    {
        out.writeShort(accessFlags);
        out.writeShort(nameIndex);
        out.writeShort(descriptorIndex);

        out.writeShort(attributes.length);
        for (AttributeInfo a : attributes)
            a.write(out);
    }
}
