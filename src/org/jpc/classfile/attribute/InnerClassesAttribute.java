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
 * Class attribute recording the inner classes and interfaces of a
 * class and their heirarchical relationships.
 * @author Mike Moleschi
 */
public class InnerClassesAttribute extends AttributeInfo
{
    private ClassEntry[] classes;

    InnerClassesAttribute(DataInputStream in, int index) throws IOException
    {
        super(in, index);
        classes = new ClassEntry[in.readUnsignedShort()];
        for (int i = 0; i < classes.length; i++) classes[i] = new ClassEntry(in);
    }

    public void write(DataOutputStream out) throws IOException
    {
        super.write(out);
        out.writeShort(classes.length);
        for (ClassEntry c : classes)
            c.write(out);
    }

    static class ClassEntry
    {
        private int innnerClassInfoIndex;
        private int outerClassInfoIndex;
        private int innnerNameIndex;
        private int innnerClassAccessFlags;
        static final int PUBLIC = 1;
        static final int PRIVATE = 2;
        static final int PROTECTED = 4;
        static final int STATIC = 8;
        static final int FINAL = 16;
        static final int INTERFACE = 512;
        static final int ABSTRACT = 1024;
        static final int SYNTHETIC = 4096;
        static final int ANNOTATION = 8192;
        static final int ENUM = 16384;

        ClassEntry(DataInputStream in) throws IOException
        {
            super();
            innnerClassInfoIndex = in.readUnsignedShort();
            outerClassInfoIndex = in.readUnsignedShort();
            innnerNameIndex = in.readUnsignedShort();
            innnerClassAccessFlags = in.readUnsignedShort();
        }

        void write(DataOutputStream out) throws IOException
        {
            out.writeShort(innnerClassInfoIndex);
            out.writeShort(outerClassInfoIndex);
            out.writeShort(innnerNameIndex);
            out.writeShort(innnerClassAccessFlags);
        }
    }
}
