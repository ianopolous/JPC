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
import org.jpc.classfile.attribute.*;
import java.io.*;

/**
 * 
 * @author Mike Moleschi
 */
class MethodInfo
{
    private int accessFlags;
    private int nameIndex;
    private int descriptorIndex;
    private AttributeInfo[] attributes;
    private CodeAttribute codeAttribute;
    
    static final int PUBLIC = 0x0001;
    static final int PRIVATE = 0x0002;
    static final int PROTECTED = 0x0004;
    static final int STATIC = 0x0008;
    static final int FINAL = 0x0010;
    static final int SYNCHRONIZED = 0x0020;
    static final int BRIDGE = 0x0040;
    static final int VARARGS = 0x0080;
    static final int NATIVE = 0x0100;
    static final int ABSTRACT = 0x0400;
    static final int STRICT = 0x0800;
    static final int SYNTHETIC = 0x1000;

    MethodInfo(DataInputStream in, ConstantPoolInfo[] pool) throws IOException
    {
        accessFlags = in.readUnsignedShort();
        nameIndex = in.readUnsignedShort();
        descriptorIndex = in.readUnsignedShort();

        attributes = new AttributeInfo[in.readUnsignedShort()];
        for (int i = 0; i < attributes.length; i++) {
            attributes[i] = AttributeInfo.construct(in, pool);
            if (attributes[i] instanceof CodeAttribute)
                codeAttribute = (CodeAttribute) attributes[i];
        }
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

    int getNameIndex() { return nameIndex; }

    int getDescriptorIndex() { return descriptorIndex; }

    void setCode(byte[] code, CodeAttribute.ExceptionEntry[] exceptionTable, ClassFile cf)
    {
        String descriptor = cf.getConstantPoolUtf8(this.getDescriptorIndex());
        int argLength = ClassFile.getMethodArgLength(descriptor);
        codeAttribute.setCode(code, exceptionTable, cf, argLength);
    }
}
