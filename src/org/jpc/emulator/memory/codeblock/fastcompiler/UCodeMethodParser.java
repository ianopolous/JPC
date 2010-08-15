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

package org.jpc.emulator.memory.codeblock.fastcompiler;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import org.jpc.emulator.memory.codeblock.optimised.MicrocodeSet;
import org.jpc.classfile.JavaOpcode;

/**
 * 
 * @author Chris Dennis
 */
public class UCodeMethodParser
{
    private static final Logger LOGGING = Logger.getLogger(UCodeMethodParser.class.getName());

    private static final Map<String, Integer> microcodeIndex = new HashMap<String, Integer>();
    private static final Map<String, Integer> elementIndex = new HashMap<String, Integer>();
    private static final Map<String, Integer> opcodeIndex = new HashMap<String, Integer>();

    static {
        try {
            for (Field f : MicrocodeSet.class.getDeclaredFields()) {
                int mods = f.getModifiers();
                if (!Modifier.isStatic(mods) || !Modifier.isPublic(mods) || !Modifier.isFinal(mods))
                    continue;
                microcodeIndex.put(f.getName(), Integer.valueOf(f.getInt(null)));
            }
        } catch (IllegalAccessException e) {
            LOGGING.log(Level.WARNING, "microcode lookup table incomplete", e);
        }
        
        try {
            for (Field f : FASTCompiler.class.getDeclaredFields()) {
                int mods = f.getModifiers();
                if (!Modifier.isStatic(mods) || !Modifier.isFinal(mods) || Modifier.isPrivate(mods))
                    continue;

                int value = f.getInt(null);
                if ((value >= 0) && (value < FASTCompiler.ELEMENT_COUNT)) {
                    String name = f.getName();
                    if (name.startsWith("PROCESSOR_ELEMENT_")) {
                        name = name.substring("PROCESSOR_ELEMENT_".length());
                        elementIndex.put(name, Integer.valueOf(value));
                    }
                }
            }
        } catch (IllegalAccessException e) {
            LOGGING.log(Level.WARNING, "element lookup table incomplete", e);
        }

        try {
            for (Field f : JavaOpcode.class.getDeclaredFields()) {
                int mods = f.getModifiers();
                if (!Modifier.isStatic(mods) || !Modifier.isPublic(mods) || !Modifier.isFinal(mods))
                    continue;
                if (f.getType() != Integer.TYPE)
                    continue;
                opcodeIndex.put(f.getName(), Integer.valueOf(f.getInt(null)));
            }
        } catch (IllegalAccessException e) {
            LOGGING.log(Level.WARNING, "opcode lookup table incomplete", e);
        }
    }
        
    private final Class fragments;
    private final Object[][][] operations;
    private final int[][][] operandArray;
    private final boolean[][] externalEffectsArray;
    private final boolean[][] explicitThrowArray;

    private final Map<String, Object> constantPoolIndex = new HashMap<String, Object>();

    public UCodeMethodParser(Class fragments, Object[][][] operations, int[][][] operandArray, boolean[][] externalEffectsArray, boolean[][] explicitThrowArray)
    {
        this.fragments = fragments;
        this.operations = operations;
        this.operandArray = operandArray;
        this.externalEffectsArray = externalEffectsArray;
        this.explicitThrowArray = explicitThrowArray;

        constantPoolIndex.put("IMMEDIATE", BytecodeFragments.IMMEDIATE);
        constantPoolIndex.put("X86LENGTH", BytecodeFragments.X86LENGTH);

        for (Method m : fragments.getDeclaredMethods())
            constantPoolIndex.put(m.getName(), new ConstantPoolSymbol(m));
    }

    private void insertIntoFragmentArrays(String uCodeName, String resultName, String[] args, boolean externalEffect, boolean explicitThrow, List instructions)
    {
        Integer codeVal = microcodeIndex.get(uCodeName);
        if (codeVal == null) {
            LOGGING.log(Level.INFO, "unknown microcode {0}", uCodeName);
            return;
        }

        int uCode = codeVal.intValue();
        if (operations[uCode] == null)
            operations[uCode] = new Object[FASTCompiler.ELEMENT_COUNT][];
        if (operandArray[uCode] == null)
            operandArray[uCode] = new int[FASTCompiler.ELEMENT_COUNT][];

        Integer elementValue = elementIndex.get(resultName);
        if (elementValue == null) {
            LOGGING.log(Level.INFO, "unknown processor element {0}", resultName);
            operations[uCode] = null;
            operandArray[uCode] = null;
            return;
        }

        int elementId = elementValue.intValue();

        int[] argIds = new int[args.length];
        for (int i = 0; i < argIds.length; i++)
            argIds[i] = (elementIndex.get(args[i])).intValue();

        operandArray[uCode][elementId] = argIds;

        operations[uCode][elementId] = instructions.toArray();

        externalEffectsArray[uCode][elementId] = externalEffect;
        explicitThrowArray[uCode][elementId] = explicitThrow;
    }

    private void parseMethod(Method m)
    {
        String name = m.getName();

        int pos = name.indexOf('_');
        String result = name.substring(0, pos).toUpperCase(Locale.ROOT);
        pos++;

        int start = pos;
        boolean externalEffect = false;
        pos = name.indexOf('_', start);
        if ("hef".equals(name.substring(start, pos)))
            externalEffect = true;
        pos++;
        start = pos;

	boolean explicitThrow = false;
        for (Class c : m.getExceptionTypes()) {
	    if (c == org.jpc.emulator.processor.ProcessorException.class) {
		explicitThrow = true;
		break;
	    }
	}

        int argc = m.getParameterTypes().length;
        String[] args = new String[argc];
        int end = name.length();
        for (int i = argc - 1; i >= 0; i--) {
            pos = name.lastIndexOf('_', end - 1);
            args[i] = name.substring(pos + 1, end).toUpperCase(Locale.ROOT);
            end = pos;
        }

        String uCode = name.substring(start, end);

        List<Object> instructions = new ArrayList<Object>();
        int newArgc = argc;
        for (int i = 0; i < argc; i++)
            if (constantPoolIndex.containsKey(args[i])) {
                instructions.add(Integer.valueOf(JavaOpcode.LDC));
                instructions.add(constantPoolIndex.get(args[i]));
                args[i] = null;
                newArgc--;
            }
    
        instructions.add(Integer.valueOf(JavaOpcode.INVOKESTATIC));
        instructions.add(constantPoolIndex.get(name));
        if ("EXECUTECOUNT".equals(result)) {
            instructions.add(Integer.valueOf(JavaOpcode.ILOAD));
            instructions.add(Integer.valueOf(FASTCompiler.VARIABLE_EXECUTE_COUNT_INDEX));
            instructions.add(Integer.valueOf(JavaOpcode.IADD));
            instructions.add(Integer.valueOf(JavaOpcode.ISTORE));
            instructions.add(Integer.valueOf(FASTCompiler.VARIABLE_EXECUTE_COUNT_INDEX));
        }

        String[] newArgs;
        if (newArgc < argc) {
            newArgs = new String[newArgc];
            for (int i = 0, j = 0; i < argc; i++)
                if (args[i] != null)
                    newArgs[j++] = args[i];
        } else
            newArgs = args;

        insertIntoFragmentArrays(uCode, result, newArgs, externalEffect, explicitThrow, instructions);
    }

    public void parse()
    {
        for (Method m : fragments.getDeclaredMethods()) {
	    if (Modifier.isPrivate(m.getModifiers())) continue;
            parseMethod(m);
        }
    }
}



    /* Fragment format:

      public static int reg1_nef_load1_iw(int immediate)
      {
        return immediate & 0xffff;
      }
      
      operands = immediate
      resultElement = reg1
      uCode = load1_iw
      externalEffect = false
      operations = ldc [immediate], invokestatic [reg1_load1_iw]



      public static int reg0_hef_load0_bp(int ebp)
      {
        return ebp & 0xffff;
      }

      operands = ebp
      resultElement = reg0
      uCode = load0_bp
      externalEffect = true  //body doesn't really, but that what hef means
      operations = invokestatic [reg1_load1_iw]

      <[result]>_<exteralEffect>_<uCode>_<[operands1]>_...

      last operand is top of the stack
      result and operands are optional
      
      non-element operands (these mean there needs to be ldc's in the operations):
      immediate
      x86count
      ioports


    */


