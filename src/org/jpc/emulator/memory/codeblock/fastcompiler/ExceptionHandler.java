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

import java.io.*;
import java.util.*;

import org.jpc.classfile.ClassFile;

import static org.jpc.classfile.JavaOpcode.*;

/**
 * Stores state information related to a single exception path.  This includes
 * start and end offsets, the graph sink set and the initial throwing node.
 * @author Chris Dennis
 */
public abstract class ExceptionHandler
{
    private final Map<Integer, RPNNode> graphSinks;
    private final RPNNode throwingNode;

    private int minPC, maxPC;

    private final int lastX86Position;

    /**
     * Constructs a new <code>ExceptionHandler</code> as thrown from the given
     * point, updating to the given x86 offset, using the given set of graph
     * sinks.
     * @param lastX86Position x86 offset to update to
     * @param initialNode node from which the exception was thrown
     * @param stateMap element set at the point of throwing
     */
    public ExceptionHandler(int lastX86Position, RPNNode initialNode, Map<Integer, ? extends RPNNode> stateMap)
    {
	graphSinks = new HashMap<Integer, RPNNode>(stateMap);
	for (int i = FASTCompiler.PROCESSOR_ELEMENT_COUNT; i < FASTCompiler.ELEMENT_COUNT; i++)
	    graphSinks.remove(Integer.valueOf(i));

	this.lastX86Position = lastX86Position;
	this.throwingNode = initialNode;

	minPC = Integer.MAX_VALUE;
	maxPC = Integer.MIN_VALUE;
    }

    int getX86Index()
    {
	return throwingNode.getX86Index();
    }

    void assignRange(int min, int max)
    {
	minPC = Math.min(minPC, min);
	maxPC = Math.max(maxPC, max);
    }

    boolean used()
    {
	return (minPC != Integer.MAX_VALUE);
    }

    int start() { return minPC; }

    int end() { return maxPC; }

    void write(OutputStream byteCodes, ClassFile cf) throws IOException
    {
	int affectedCount = 0;
        for (RPNNode rpn : graphSinks.values()) {
            if (rpn.getMicrocode() == -1)
                continue;

	    rpn.reset(minPC);
            affectedCount++;
        }
 
        int index = 0;
        RPNNode[] roots = new RPNNode[affectedCount];

        for (RPNNode rpn : graphSinks.values()) {
            if ((rpn.getMicrocode() == -1) || (rpn.getID() == FASTCompiler.PROCESSOR_ELEMENT_EIP))
		continue;
	    
	    rpn.writeExceptionCleanup(byteCodes, cf, true);
	    roots[index++] = rpn;
        }
	
	for (int i=index-1; i>=0; i--)
	    RPNNode.writeBytecodes(byteCodes, cf, BytecodeFragments.popCode(roots[i].getID()));
	
	
	RPNNode.writeBytecodes(byteCodes, cf, BytecodeFragments.pushCode(FASTCompiler.PROCESSOR_ELEMENT_EIP));
// 	byteCodes.write(cf.addToConstantPool(Integer.valueOf(throwingNode.getX86Position())));
	int cpIndex = cf.addToConstantPool(Integer.valueOf(lastX86Position));
	if (cpIndex < 0x100) {
	    byteCodes.write(LDC);
	} else {
	    byteCodes.write(LDC_W);
	    byteCodes.write(cpIndex >>> 8);
	}
        byteCodes.write(cpIndex);
	byteCodes.write(IADD);
	RPNNode.writeBytecodes(byteCodes, cf, BytecodeFragments.popCode(FASTCompiler.PROCESSOR_ELEMENT_EIP));

	writeHandlerRoutine(byteCodes, cf);

        byteCodes.write(ILOAD);
        byteCodes.write(FASTCompiler.VARIABLE_EXECUTE_COUNT_INDEX);
        byteCodes.write(IRETURN);
    }

    protected abstract void writeHandlerRoutine(OutputStream byteCodes, ClassFile cf) throws IOException;
}
