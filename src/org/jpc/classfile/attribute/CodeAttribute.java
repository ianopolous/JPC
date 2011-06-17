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

import org.jpc.classfile.constantpool.ConstantPoolInfo;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.jpc.classfile.*;

/**
 * Method attribute that holds the code related data and attributes.
 * 
 * @author Mike Moleschi
 */
public class CodeAttribute extends AttributeInfo
{
    private int maxStack;
    private int maxLocals;
    private byte[] code;
    private ExceptionEntry[] exceptionTable;
    private AttributeInfo[] attributes;

    CodeAttribute(DataInputStream in, int index, ConstantPoolInfo[] pool) throws IOException
    {
        super(in, index);
        maxStack = in.readUnsignedShort();
        maxLocals = in.readUnsignedShort();

        code = new byte[in.readInt()];
        in.readFully(code);
        
        exceptionTable = new ExceptionEntry[in.readUnsignedShort()];
        for (int i = 0; i < exceptionTable.length; i++)
            exceptionTable[i] = new ExceptionEntry(in);
        
        attributes = new AttributeInfo[in.readUnsignedShort()];
        for (int i = 0; i < attributes.length; i++)
            attributes[i] = AttributeInfo.construct(in, pool);
    }

    /**
     * Replaces the bytecode and exception table in this attribute.
     * @param newCode new bytecode set
     * @param newTable new exception table
     * @param cf associated classfile
     * @param argLength number of method arguments
     */
    public void setCode(byte[] newCode, ExceptionEntry[] newTable, ClassFile cf, int argLength)
    {
        attributeLength += newCode.length - code.length;
        code = newCode;
        maxLocals = Math.max(JavaCodeAnalyser.getMaxLocalVariables(code), argLength + 1);
        //+1 accounts for 'this' the hidden argument
        if ((newTable == null) || (newTable.length == 0)) maxStack = JavaCodeAnalyser.getMaxStackDepth(code, 0, newCode.length, cf);
        else {
            maxStack = JavaCodeAnalyser.getMaxStackDepth(code, 0, newTable[0].handlerPC, cf);
            for (int i = 0; i < newTable.length; i++)
                if (i < newTable.length - 1)
                    maxStack = Math.max(maxStack, JavaCodeAnalyser.getMaxStackDepth(code, newTable[i].handlerPC, newTable[i + 1].handlerPC, cf) + 1);
                else
                    maxStack = Math.max(maxStack, JavaCodeAnalyser.getMaxStackDepth(code, newTable[i].handlerPC, newCode.length, cf) + 1);

            attributeLength += 8 * (newTable.length - exceptionTable.length);
            exceptionTable = newTable;
        }
    }

    public void write(DataOutputStream out) throws IOException
    {
        super.write(out);
        out.writeShort(maxStack);
        out.writeShort(maxLocals);

        out.writeInt(code.length);
        out.write(code);

        out.writeShort(exceptionTable.length);
        for (ExceptionEntry e : exceptionTable)
            e.write(out);

        out.writeShort(attributes.length);
        for (AttributeInfo a : attributes)
            a.write(out);
    }

    /**
     * Represents a single exception table entry.
     */
    public static class ExceptionEntry
    {
        private final int startPC;
        private final int endPC;
        private final int handlerPC;
        private final int catchType;

        /**
         * Constructs a new exception entry with explicit parameters.
         * @param start start of catch range
         * @param end end of catch range
         * @param handler offset of handler entry point
         * @param type constant pool index of exception catch type
         */
        public ExceptionEntry(int start, int end, int handler, int type)
        {
            startPC = start;
            endPC = end;
            handlerPC = handler;
            catchType = type;
        }

        ExceptionEntry(DataInputStream in) throws IOException
        {
            startPC = in.readUnsignedShort();
            endPC = in.readUnsignedShort();
            handlerPC = in.readUnsignedShort();
            catchType = in.readUnsignedShort();
        }

        void write(DataOutputStream out) throws IOException
        {
            out.writeShort(startPC);
            out.writeShort(endPC);
            out.writeShort(handlerPC);
            out.writeShort(catchType);
        }
    }
}
