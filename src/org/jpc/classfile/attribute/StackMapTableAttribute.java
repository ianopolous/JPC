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
 * Code attribute designating for a given frame the local variable, and 
 * stack types that should be present.  Used during class loading for type
 * verification.
 * @author Mike Moleschi
 */
public class StackMapTableAttribute extends AttributeInfo
{
    private StackMapFrame[] entries;

    StackMapTableAttribute(DataInputStream in, int index) throws IOException
    {
        super(in, index);
        entries = new StackMapFrame[in.readUnsignedShort()];
        for (int i = 0; i < entries.length; i++) entries[i] = StackMapFrame.construct(in);
    }

    public void write(DataOutputStream out) throws IOException
    {
        super.write(out);
        out.writeInt(entries.length);
        for (StackMapFrame s : entries)
            s.write(out);
    }

    static abstract class StackMapFrame
    {
        protected int frameType;
        static final int SAME_L = 0;
        static final int SAME_H = 63;
        static final int SAME_LOCALS_1_STACK_ITEM_L = 64;
        static final int SAME_LOCALS_1_STACK_ITEM_H = 127;
        static final int SAME_LOCALS_1_STACK_ITEM_EXTENDED = 247;
        static final int CHOP_L = 248;
        static final int CHOP_H = 250;
        static final int SAME_FRAME_EXTENDED = 251;
        static final int APPEND_L = 252;
        static final int APPEND_H = 254;
        static final int FULL_FRAME = 255;

        abstract void write(DataOutputStream out) throws IOException;

        static StackMapFrame construct(DataInputStream in) throws IOException
        {
            int tag = in.readUnsignedByte();
            if ((tag >= SAME_L) && (tag <= SAME_H)) return new SameFrame(in, tag);
            else if ((tag >= SAME_LOCALS_1_STACK_ITEM_L) && (tag <= SAME_LOCALS_1_STACK_ITEM_H)) return new SameLocals1StackItemFrame(in, tag);
            else if (tag == SAME_LOCALS_1_STACK_ITEM_EXTENDED) return new SameLocals1StackItemFrameExtended(in, tag);
            else if ((tag >= CHOP_L) && (tag <= CHOP_H)) return new ChopFrame(in, tag);
            else if (tag == SAME_FRAME_EXTENDED) return new SameFrameExtended(in, tag);
            else if ((tag >= APPEND_L) && (tag <= APPEND_H)) return new AppendFrame(in, tag);
            else if (tag == FULL_FRAME) return new FullFrame(in, tag);
            else return null;
        }

        int getFrameType()
        {
            return frameType;
        }

        static class SameFrame extends StackMapFrame
        {
            SameFrame(DataInputStream in, int tag) throws IOException
            {
                super();
                frameType = tag;
            }

            void write(DataOutputStream out) throws IOException
            {
                out.writeByte(frameType);
            }
        }

        static class SameLocals1StackItemFrame extends StackMapFrame
        {
            private VerificationTypeInfo[] stack;

            SameLocals1StackItemFrame(DataInputStream in, int tag) throws IOException
            {
                super();
                frameType = tag;
                stack = new VerificationTypeInfo[1];
                stack[0] = VerificationTypeInfo.construct(in);
            }

            void write(DataOutputStream out) throws IOException
            {
                out.writeByte(frameType);
                stack[0].write(out);
            }
        }

        static class SameLocals1StackItemFrameExtended extends StackMapFrame
        {
            private int offsetDelta;
            private VerificationTypeInfo[] stack;

            SameLocals1StackItemFrameExtended(DataInputStream in, int tag) throws IOException
            {
                super();
                frameType = tag;
                offsetDelta = in.readUnsignedShort();
                stack = new VerificationTypeInfo[1];
                stack[0] = VerificationTypeInfo.construct(in);
            }

            void write(DataOutputStream out) throws IOException
            {
                out.writeByte(frameType);
                out.writeShort(offsetDelta);
                stack[0].write(out);
            }
        }

        static class ChopFrame extends StackMapFrame
        {
            private int offsetDelta;

            ChopFrame(DataInputStream in, int tag) throws IOException
            {
                super();
                frameType = tag;
                offsetDelta = in.readUnsignedShort();
            }

            void write(DataOutputStream out) throws IOException
            {
                out.writeByte(frameType);
                out.writeShort(offsetDelta);
            }
        }

        static class SameFrameExtended extends ChopFrame
        {
            SameFrameExtended(DataInputStream in, int tag) throws IOException
            {
                super(in, tag);
            }
        }

        static class AppendFrame extends StackMapFrame
        {
            private int offsetDelta;
            private VerificationTypeInfo[] locals;

            AppendFrame(DataInputStream in, int tag) throws IOException
            {
                super();
                frameType = tag;
                offsetDelta = in.readUnsignedShort();
                locals = new VerificationTypeInfo[frameType - 251];
                for (int i = 0; i < locals.length; i++) locals[i] = VerificationTypeInfo.construct(in);
            }

            void write(DataOutputStream out) throws IOException
            {
                out.writeByte(frameType);
                out.writeShort(offsetDelta);
                for (int i = 0; i < locals.length; i++) locals[i].write(out);
            }
        }

        static class FullFrame extends StackMapFrame
        {
            private int offsetDelta;
            private VerificationTypeInfo[] locals;
            private VerificationTypeInfo[] stack;

            FullFrame(DataInputStream in, int tag) throws IOException
            {
                super();
                frameType = tag;
                offsetDelta = in.readUnsignedShort();
                locals = new VerificationTypeInfo[in.readUnsignedShort()];
                for (int i = 0; i < locals.length; i++) locals[i] = VerificationTypeInfo.construct(in);
                stack = new VerificationTypeInfo[in.readUnsignedShort()];
                for (int i = 0; i < stack.length; i++) stack[i] = VerificationTypeInfo.construct(in);
            }

            void write(DataOutputStream out) throws IOException
            {
                out.writeByte(frameType);
                out.writeShort(offsetDelta);

                out.writeShort(locals.length);
                for (int i = 0; i < locals.length; i++) locals[i].write(out);

                out.writeShort(stack.length);
                for (int i = 0; i < stack.length; i++) stack[i].write(out);
            }
        }

        static abstract class VerificationTypeInfo
        {
            protected int tag;
            static final int TOP = 0;
            static final int INTEGER = 1;
            static final int FLOAT = 2;
            static final int LONG = 4;
            static final int DOUBLE = 3;
            static final int NULL = 5;
            static final int UNINITIALIZEDTHIS = 6;
            static final int OBJECT = 7;
            static final int UNINITIALIZED = 8;

            static VerificationTypeInfo construct(DataInputStream in) throws IOException
            {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case TOP:
                        return new TopVariableInfo(tag);
                    case INTEGER:
                        return new IntegerVariableInfo(tag);
                    case FLOAT:
                        return new FloatVariableInfo(tag);
                    case LONG:
                        return new LongVariableInfo(tag);
                    case DOUBLE:
                        return new DoubleVariableInfo(tag);
                    case NULL:
                        return new NullVariableInfo(tag);
                    case UNINITIALIZEDTHIS:
                        return new UninitializedThisVariableInfo(tag);
                    case OBJECT:
                        return new ObjectVariableInfo(in, tag);
                    case UNINITIALIZED:
                        return new UninitializedVariableInfo(in, tag);
                }
                return null;
            }

            int getTag()
            {
                return tag;
            }

            void write(DataOutputStream out) throws IOException
            {
                out.writeByte(tag);
            }

            static class TopVariableInfo extends VerificationTypeInfo
            {
                TopVariableInfo(int tag) throws IOException
                {
                    super();
                    this.tag = tag;
                }
            }

            static class IntegerVariableInfo extends TopVariableInfo
            {
                IntegerVariableInfo(int tag) throws IOException
                {
                    super(tag);
                }
            }

            static class FloatVariableInfo extends TopVariableInfo
            {
                FloatVariableInfo(int tag) throws IOException
                {
                    super(tag);
                }
            }

            static class LongVariableInfo extends TopVariableInfo
            {
                LongVariableInfo(int tag) throws IOException
                {
                    super(tag);
                }
            }

            static class DoubleVariableInfo extends TopVariableInfo
            {
                DoubleVariableInfo(int tag) throws IOException
                {
                    super(tag);
                }
            }

            static class NullVariableInfo extends TopVariableInfo
            {
                NullVariableInfo(int tag) throws IOException
                {
                    super(tag);
                }
            }

            static class UninitializedThisVariableInfo extends TopVariableInfo
            {
                UninitializedThisVariableInfo(int tag) throws IOException
                {
                    super(tag);
                }
            }

            static class ObjectVariableInfo extends VerificationTypeInfo
            {
                private int cpoolIndex;

                ObjectVariableInfo(DataInputStream in, int tag) throws IOException
                {
                    super();
                    this.tag = tag;
                    cpoolIndex = in.readUnsignedShort();
                }

                void write(DataOutputStream out) throws IOException
                {
                    out.writeByte(tag);
                    out.writeShort(cpoolIndex);
                }
            }

            static class UninitializedVariableInfo extends VerificationTypeInfo
            {
                private int offset;

                UninitializedVariableInfo(DataInputStream in, int tag) throws IOException
                {
                    super();
                    this.tag = tag;
                    offset = in.readUnsignedShort();
                }

                void write(DataOutputStream out) throws IOException
                {
                    out.writeByte(tag);
                    out.writeShort(offset);
                }
            }
        }
    }
}
