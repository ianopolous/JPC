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

import org.jpc.classfile.constantpool.*;
import org.jpc.classfile.attribute.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Java classfile manipulation
 *
 * Written to the following specs: 
 *   JVM Spec (http://java.sun.com/docs/books/jvms/second_edition/html/VMSpecTOC.doc.html)
 *   JSR202 (http://www.jcp.org/en/jsr/detail?id=202)
 *
 * @author Mike Moleschi
 */
public class ClassFile
{
    private int magic;
    private int minorVersion;
    private int majorVersion;
    private int constantPoolCount;
    private ConstantPoolInfo[] constantPool;
    private Map<Object, Integer> constantPoolObjects;

    private int accessFlags;
    private int thisClass;
    private int superClass;
    private int[] interfaces;
    private FieldInfo[] fields;
    private MethodInfo[] methods;
    private AttributeInfo[] attributes;

    static final int PUBLIC = 0x0001;
    static final int FINAL = 0x0010;
    static final int SUPER = 0x0020;
    static final int INTERFACE = 0x0200;
    static final int ABSTRACT = 0x0400;

    static final int MAX_CONSTANT_POOL_SIZE = 64 * 1024;
    static final int MAX_METHOD_CODE_SIZE = 64 * 1024;
    
    /**
     * Load a set of class bytes from the given input stream.
     * @param in stream to read from
     * @throws java.io.IOException if the class bytes are detected as malformed
     */
    public void read(InputStream in) throws IOException
    {
        DataInputStream din = new DataInputStream(in);
        readMagic(din);
        readVersion(din);
        readConstantPool(din);
        readAccessFlags(din);
        readThisClass(din);
        readSuperClass(din);
        readInterfaces(din);
        readFields(din);
        readMethods(din);
        readAttributes(din);
    }

    /**
     * Writes out this class to the given output stream.
     * @param out stream to write to
     * @throws java.io.IOException if there is a problem writing to the stream
     */
    public void write(OutputStream out) throws IOException
    {
        DataOutputStream dout = new DataOutputStream(out);
        writeMagic(dout);
        writeVersion(dout);
        writeConstantPool(dout);
        writeAccessFlags(dout);
        writeThisClass(dout);
        writeSuperClass(dout);
        writeInterfaces(dout);
        writeFields(dout);
        writeMethods(dout);
        writeAttributes(dout);
    }

//    public void update()
//    {
//        /* this function will supposedly sync all class info.
//           for now I am hoping that by changing to a lower version I can get
//           the class loader to ignore half my problems */
//        minorVersion = 0;
//        majorVersion = 46;
//    }

//    String[] getMethodNames()
//    {
//        String[] names = new String[methods.length];
//        for(int i = 0; i < methods.length; i++)
//        {
//            int index = methods[i].getNameIndex();
//            names[i] = ((Utf8Info) constantPool[index]).getString();
//        }
//        return names;
//    }
    
    /**
     * Set the methods bytecode and exception table to the given arrays
     * @param methodName name of target method
     * @param codeBytes array of bytecode
     * @param exceptionTable array of exception table entries
     */
    public void setMethodCode(String methodName, byte[] codeBytes, CodeAttribute.ExceptionEntry[] exceptionTable)
    {
        MethodInfo mi = getMethodInfo(methodName);
        mi.setCode(codeBytes, exceptionTable, this);
    }

    /**
     * Returns the name of this class as recorded in the constant pool.
     * @return this classes class name
     */
    public String getClassName()
    {
        if (!(constantPool[thisClass] instanceof ClassInfo))
            throw new ClassFormatError("thisClass points to non-class constant pool entry");

        int nameIndex = ((ClassInfo) constantPool[thisClass]).getNameIndex();

        if (!(constantPool[nameIndex] instanceof Utf8Info))
            throw new ClassFormatError("thisClass constant pool entry points to non-utf8 constant pool entry");

        return ((Utf8Info) constantPool[nameIndex]).getString().replace('/','.');
    }

    /**
     * Sets this classes name in the constant pool.
     * @param name new class name
     */
    public void setClassName(String name)
    {
        if (!(constantPool[thisClass] instanceof ClassInfo))
            throw new ClassFormatError("thisClass points to a non-class constant pool entry");

        int nameIndex = ((ClassInfo) constantPool[thisClass]).getNameIndex();

        if (!(constantPool[nameIndex] instanceof Utf8Info))
            throw new ClassFormatError("thisClass constant pool entry points to a non-utf8 constant pool entry");
            
        constantPool[nameIndex] = new Utf8Info(name.replace('.','/'));
    }

    /**
     * Returns the index of this object in the constant pool.
     * 
     * This will add a new entry to the pool if necessary.
     * @param o object to be added to the constant pool
     * @return index into constant pool where value is stored
     */
    public int addToConstantPool(Object o)
    {
        int index = searchConstantPool(o);
        if (index >= 0)
            return index;

        ConstantPoolInfo cpInfo = createConstantPoolInfo(o);

        index = searchConstantPool(cpInfo);
        if (index >= 0)
            return index;            
        
        if ((cpInfo instanceof DoubleInfo) || (cpInfo instanceof LongInfo)) {            
            int newIndex = constantPoolCount;
	    constantPool[newIndex] = cpInfo;
	    constantPool[newIndex + 1] = cpInfo;
            constantPoolCount += 2;
            
            constantPoolObjects.put(o, Integer.valueOf(newIndex));
            return newIndex;
        } else {
            int newIndex = constantPoolCount;
	    constantPool[newIndex] = cpInfo;
            constantPoolCount += 1;
            
            constantPoolObjects.put(o, Integer.valueOf(newIndex));
            return newIndex;
        }
    }

    private ConstantPoolInfo createConstantPoolInfo(Object o)
    {
        if (o instanceof Field) {
            Field fld = (Field) o;
            String descriptor = getDescriptor(fld.getType());

            ConstantPoolInfo nameInfo = new Utf8Info(fld.getName());
            int nameIndex = addToConstantPool(nameInfo);
            ConstantPoolInfo descriptorInfo = new Utf8Info(descriptor);
            int descriptorIndex = addToConstantPool(descriptorInfo);
            ConstantPoolInfo nameAndTypeInfo = new NameAndTypeInfo(nameIndex, descriptorIndex);
            int nameAndTypeIndex = addToConstantPool(nameAndTypeInfo);

            Class cls = ((Field) o).getDeclaringClass();
            int classIndex = addToConstantPool(cls);

            return new FieldRefInfo(classIndex, nameAndTypeIndex);
        } else if (o instanceof Method) {
            Method mtd = (Method) o;            
            StringBuilder buf = new StringBuilder("(");
            for (Class c : mtd.getParameterTypes())
                buf.append(getDescriptor(c));
            buf.append(')');
            buf.append(getDescriptor(mtd.getReturnType()));
            String descriptor = buf.toString();

            ConstantPoolInfo nameInfo = new Utf8Info(mtd.getName());
            int nameIndex = addToConstantPool(nameInfo);
            ConstantPoolInfo descriptorInfo = new Utf8Info(descriptor);
            int descriptorIndex = addToConstantPool(descriptorInfo);
            ConstantPoolInfo nameAndTypeInfo = new NameAndTypeInfo(nameIndex, descriptorIndex);
            int nameAndTypeIndex = addToConstantPool(nameAndTypeInfo);

            Class cls = mtd.getDeclaringClass();
            int classIndex = addToConstantPool(cls);

            if (cls.isInterface())
                return new InterfaceMethodRefInfo(classIndex, nameAndTypeIndex);
            else
                return new MethodRefInfo(classIndex, nameAndTypeIndex);
        } else if (o instanceof Class) {
            String className = ((Class) o).getName().replace('.', '/');
            int utf8Index = addToConstantPool(new Utf8Info(className));
            return new ClassInfo(utf8Index);
        } else if (o instanceof String) {
            int utf8Index = addToConstantPool(new Utf8Info((String) o));
            return new StringInfo(utf8Index);
        } else if (o instanceof Integer)
            return new IntegerInfo(((Integer) o).intValue());
        else if (o instanceof Float)
            return new FloatInfo(((Float) o).floatValue());
        else if (o instanceof Long)
            return new LongInfo(((Long) o).longValue());
        else if (o instanceof Double)
            return new DoubleInfo(((Double) o).doubleValue());
        else if (o instanceof ConstantPoolInfo)
            return (ConstantPoolInfo) o;
        else
            throw new IllegalArgumentException("Invalid Class To Add To Constant Pool: " + o);
    }
    
    /**
     * Returns the descriptor for the given field ref object.
     * @param index constant pool index
     * @return field descriptor
     */
    String getConstantPoolFieldDescriptor(int index)
    {
        ConstantPoolInfo cpi = constantPool[index];
        //get name and type index from method ref
        index = ((FieldRefInfo) cpi).getNameAndTypeIndex();        
        cpi = constantPool[index];
        //get descriptor index from name and type
        index = ((NameAndTypeInfo) cpi).getDescriptorIndex();
        cpi = constantPool[index];
        
        return ((Utf8Info) cpi).getString();
    }

    /**
     * Returns the size (in stack elements) of the given field descriptors type.
     * @param fieldDescriptor field descriptor
     * @return stack size of the type (0,1,2)
     */
    static int getFieldLength(String fieldDescriptor)
    {
        return  getFieldLength(fieldDescriptor.charAt(0));
    }

    private static int getFieldLength(char ch)
    {
        switch(ch)
        {
        case 'V':
            return 0;
        case 'B':
        case 'C':
        case 'F':
        case 'I':
        case 'L':
        case 'S':
        case 'Z':
        case '[':
            return 1;
        case 'D':
        case 'J':
            return 2;
        }
        throw new IllegalStateException();
    }

    /**
     * Returns the Utf8Info constant pool object at the given index as a String
     * @param index position of the Utf8Info object
     * @return string representation
     */
    String getConstantPoolUtf8(int index)
    {
        return ((Utf8Info)constantPool[index]).getString();
    }

    /**
     * Returns the descriptor for the given constant pool method ref object.
     * @param index constant pool object
     * @return method descriptor
     */
    String getConstantPoolMethodDescriptor(int index)
    {
        ConstantPoolInfo cpi = constantPool[index];
        //get name and type index from method ref
        index = ((MethodRefInfo) cpi).getNameAndTypeIndex();
        cpi = constantPool[index];
        //get descriptor index from name and type
        index = ((NameAndTypeInfo) cpi).getDescriptorIndex();
        cpi = constantPool[index];
        
        return ((Utf8Info) cpi).getString();
    }

    /**
     * Returns the stack delta caused by an invoke on a method with the given
     * descriptor
     * @param methodDescriptor
     * @return method's stack delta
     */
    static int getMethodStackDelta(String methodDescriptor)
    {
        int argLength = getMethodArgLength(methodDescriptor);
        
        for(int i = 0; i < methodDescriptor.length(); i++)
        {
            char ch = methodDescriptor.charAt(i);
            if (ch == ')')
                return argLength - getFieldLength(methodDescriptor.charAt(i + 1));
        }
        throw new IllegalStateException("Invalid method descriptor");
    }

    /** @return count of arguments -- within () */
    /**
     * Returns the total argument size in stack elements of the given method
     * descriptor
     * @param methodDescriptor method descriptor
     * @return argument size in stack elements
     */
    static int getMethodArgLength(String methodDescriptor)
    {
        int count = 0;

        for(int i = 0; i < methodDescriptor.length(); i++)
        {
            char ch = methodDescriptor.charAt(i);
            switch (ch) {
                case '[':
                    while ((ch = methodDescriptor.charAt(++i)) == '[') ;
                    if (ch == 'L')
                        while (methodDescriptor.charAt(++i) != ';') ;
                    count += 1;
                    break;
                case 'L':
                    while (methodDescriptor.charAt(++i) != ';') ;
                    count += 1;
                    break;
                case 'B':
                case 'C':
                case 'F':
                case 'I':
                case 'S':
                case 'Z':
                    count += 1;
                    break;
                case 'D':
                case 'J':
                    count += 2;
                    break;
                case ')':
                    return count;
                default:
                    break;
            }
        }
        throw new IllegalStateException("Invalid method descriptor");
    }


    private static String getDescriptor(Class cls)
    {
        if (cls.isArray())
            return cls.getName().replace('.','/');
        else
        {
            if (cls.isPrimitive()) {
                if (cls.equals(Byte.TYPE))
                    return "B";
                else if (cls.equals(Character.TYPE))
                    return "C";
                else if (cls.equals(Double.TYPE))
                    return "D";
                else if (cls.equals(Float.TYPE))
                    return "F";
                else if (cls.equals(Integer.TYPE))
                    return "I";
                else if (cls.equals(Long.TYPE))
                    return "J";
                else if (cls.equals(Short.TYPE))
                    return "S";
                else if (cls.equals(Boolean.TYPE))
                    return "Z";
                else if (cls.equals(Void.TYPE))
                    return "V";
                else
                    throw new IllegalStateException("They added a primitive!!! Is it unsigned!!! " + cls.getName());
            } else {
                return 'L' + cls.getName().replace('.','/') + ';';
            }
        }
    }

    private int searchConstantPool(Object query)
    {
	Integer value = constantPoolObjects.get(query);
	if (value == null)
            return -1;
        else
	    return value.intValue();
    }

    private MethodInfo getMethodInfo(String methodName)
    {
        for (MethodInfo m : methods) {
            ConstantPoolInfo cp = constantPool[m.getNameIndex()];
            
            if ((cp instanceof Utf8Info) && ((Utf8Info) cp).getString().equals(methodName))
                    return m;
        }
        return null;
    }

    private void readMagic(DataInputStream in) throws IOException
    {
        magic = in.readInt();
    }

    private void writeMagic(DataOutputStream out) throws IOException
    {
        out.writeInt(magic);
    }

    private void readVersion(DataInputStream in) throws IOException
    {
        minorVersion = in.readUnsignedShort();
        majorVersion = in.readUnsignedShort();
    }

    private void writeVersion(DataOutputStream out) throws IOException
    {
        out.writeShort(minorVersion);
        out.writeShort(majorVersion);
    }

    private void readConstantPool(DataInputStream in) throws IOException
    {
        constantPoolCount = in.readUnsignedShort();
        // be aware that constant pool indices start at 1!! (not 0)
	constantPool = new ConstantPoolInfo[MAX_CONSTANT_POOL_SIZE];
	constantPoolObjects = new HashMap<Object, Integer>();
        for (int i = 1; i < constantPoolCount; i++) {
            ConstantPoolInfo cpInfo = ConstantPoolInfo.construct(in);
            constantPool[i] = cpInfo;
            constantPoolObjects.put(cpInfo, Integer.valueOf(i));
            if ((constantPool[i] instanceof DoubleInfo) || (constantPool[i] instanceof LongInfo)) {
                i++;
                constantPool[i] = constantPool[i - 1];
            }
        }
    }

    private void writeConstantPool(DataOutputStream out) throws IOException
    {
        out.writeShort(constantPoolCount);
        // be aware that constant pool indices start at 1!! (not 0)
        for (int i = 1; i < constantPoolCount; i++) {
            constantPool[i].write(out);
            if ((constantPool[i] instanceof DoubleInfo) || (constantPool[i] instanceof LongInfo))
                i++;
        }
    }

    private void readAccessFlags(DataInputStream in) throws IOException
    {
        accessFlags = in.readUnsignedShort();
    }

    private void writeAccessFlags(DataOutputStream out) throws IOException
    {
        out.writeShort(accessFlags);
    }

    private void readThisClass(DataInputStream in) throws IOException
    {
        thisClass = in.readUnsignedShort();
    }

    private void writeThisClass(DataOutputStream out) throws IOException
    {
        out.writeShort(thisClass);
    }

    private void readSuperClass(DataInputStream in) throws IOException
    {
        superClass = in.readUnsignedShort();
    }

    private void writeSuperClass(DataOutputStream out) throws IOException
    {
        out.writeShort(superClass);
    }

    private void readInterfaces(DataInputStream in) throws IOException
    {
        interfaces = new int[in.readUnsignedShort()];
        for (int i = 0; i < interfaces.length; i++)
            interfaces[i] = in.readUnsignedShort();
    }

    private void writeInterfaces(DataOutputStream out) throws IOException
    {
        out.writeShort(interfaces.length);
        for (int i : interfaces)
            out.writeShort(i);
    }

    private void readFields(DataInputStream in) throws IOException
    {
        fields = new FieldInfo[in.readUnsignedShort()];
        for (int i = 0; (i < fields.length); i++)
            fields[i] = new FieldInfo(in, constantPool);
    }

    private void writeFields(DataOutputStream out) throws IOException
    {
        out.writeShort(fields.length);
        for (FieldInfo f : fields)
            f.write(out);
    }

    private void readMethods(DataInputStream in) throws IOException
    {
        methods = new MethodInfo[in.readUnsignedShort()];
        for(int i = 0; (i < methods.length); i++)
            methods[i] = new MethodInfo(in, constantPool);
    }

    private void writeMethods(DataOutputStream out) throws IOException
    {
        out.writeShort(methods.length);
        for (MethodInfo m : methods)
            m.write(out);
    }

    private void readAttributes(DataInputStream in) throws IOException
    {
        attributes = new AttributeInfo[in.readUnsignedShort()];
        for(int i = 0; (i < attributes.length); i++)
            attributes[i] = AttributeInfo.construct(in, constantPool);
    }

    private void writeAttributes(DataOutputStream out) throws IOException
    {
        out.writeShort(attributes.length);
        for (AttributeInfo a : attributes)
            a.write(out);
    }
}
