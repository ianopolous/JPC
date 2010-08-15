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

import org.jpc.classfile.constantpool.*;

import java.io.*;

/**
 * Abstract superclass for all attribute types.
 * @author Mike Moleschi
 */
public abstract class AttributeInfo
{
    protected int attributeNameIndex;
    protected int attributeLength;

    /**
     * Constructs a new AttributeInfo object from the given <code>DataInputStream</code>
     * 
     * The supplied index points to the constant pool Utf8Info entry identifying
     * the particular attribute type.
     * @param in stream to read from
     * @param index constant pool name index
     * @throws java.io.IOException on an underlying stream error
     */
    AttributeInfo(DataInputStream in, int index) throws IOException
    {
        attributeNameIndex = index;
        attributeLength = in.readInt();
    }

    /**
     * Writes this attribute out to the supplied <code>DataOutputStream</code>.
     * 
     * Classes extending using this implementatin as a superclass should make
     * sure to call <code>super.write(out)</code> as the first line of their
     * own overriding method.
     * @param out stream to write to
     * @throws java.io.IOException on an underlying stream error
     */
    public void write(DataOutputStream out) throws IOException
    {
        out.writeShort(attributeNameIndex);
        out.writeInt(attributeLength);
    }

    /**
     * Static factory method which constructs an instance of the relevant
     * subclass by reading the given <code>DataInputStream</code>
     * @param in stream to read from
     * @param pool associated constant pool
     * @return instance of AttributeInfo subclass
     * @throws java.io.IOException on an underlying stream error
     */
    public static AttributeInfo construct(DataInputStream in, ConstantPoolInfo[] pool) throws IOException
    {
        int index = in.readUnsignedShort();

        if (pool[index] instanceof Utf8Info)
        {
            String s = ((Utf8Info) pool[index]).getString();
            if ("Code".equals(s))
                return new CodeAttribute(in, index, pool);
//            else if ("SourceFile".equals(s))
//                return new SourceFileAttribute(in, index);
//            else if ("ConstantValue".equals(s))
//                return new ConstantValueAttribute(in, index);
//            else if ("StackMapTable".equals(s))
//                return new StackMapTableAttribute(in, index);
//            else if ("Exceptions".equals(s))
//                return new ExceptionsAttribute(in, index);
//            else if ("InnerClasses".equals(s))
//                return new InnerClassesAttribute(in, index);
//            else if ("EnclosingMethod".equals(s))
//                return new EnclosingMethodAttribute(in, index);
//            else if ("Synthetic".equals(s))
//                return new SyntheticAttribute(in, index);
//            else if ("Signature".equals(s))
//                return new SignatureAttribute(in, index);
//            else if ("LineNumberTable".equals(s))
//                return new LineNumberTableAttribute(in, index);
//            else if ("LocalVariableTable".equals(s))
//                return new LocalVariableTableAttribute(in, index);
//            else if ("Deprecated".equals(s))
//                return new DeprecatedAttribute(in, index);
            else
                return new UnknownAttribute(in, index);
        }
        return null;
    }
}
