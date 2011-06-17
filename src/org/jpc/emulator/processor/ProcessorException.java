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

package org.jpc.emulator.processor;

/**
 * 
 * @author Chris Dennis
 */
public final class ProcessorException extends RuntimeException
{
    public static final ProcessorException DIVIDE_ERROR = new ProcessorException(Type.DIVIDE_ERROR, true);
    public static final ProcessorException BOUND_RANGE = new ProcessorException(Type.BOUND_RANGE, true);
    public static final ProcessorException UNDEFINED = new ProcessorException(Type.UNDEFINED, true);
    public static final ProcessorException DOUBLE_FAULT_0 = new ProcessorException(Type.DOUBLE_FAULT, 0, true);
    public static final ProcessorException STACK_SEGMENT_0 = new ProcessorException(Type.STACK_SEGMENT, 0, true);
    public static final ProcessorException GENERAL_PROTECTION_0 = new ProcessorException(Type.GENERAL_PROTECTION, 0, true);
    public static final ProcessorException FLOATING_POINT = new ProcessorException(Type.FLOATING_POINT, true);
    public static final ProcessorException ALIGNMENT_CHECK_0 = new ProcessorException(Type.ALIGNMENT_CHECK, 0, true);
    
    public static enum Type {
        DIVIDE_ERROR(0x00), DEBUG(0x01), BREAKPOINT(0x03), OVERFLOW(0x04),
        BOUND_RANGE(0x05), UNDEFINED(0x06), NO_FPU(0x07), DOUBLE_FAULT(0x08),
        FPU_SEGMENT_OVERRUN(0x09), TASK_SWITCH(0x0a), NOT_PRESENT(0x0b),
        STACK_SEGMENT(0x0c), GENERAL_PROTECTION(0x0d), PAGE_FAULT(0x0e),
        FLOATING_POINT(0x10), ALIGNMENT_CHECK(0x11), MACHINE_CHECK(0x12),
        SIMD_FLOATING_POINT(0x13);

        //Traps: BREAKPOINT, OVERFLOW

        private final int vector;
        
        Type(int vector)
        {
            this.vector = vector;
        }
        
        public int vector()
        {
            return vector;
        }
    }
    
    private final Type type;
    private final int errorCode;
    private final boolean pointsToSelf;
    private final boolean hasErrorCode;

    public ProcessorException(Type type, int errorCode, boolean pointsToSelf)
    {
        this.type = type;
        this.hasErrorCode = true;
        this.errorCode = errorCode;
        this.pointsToSelf = pointsToSelf;
    }

    private ProcessorException(Type type, boolean pointsToSelf)
    {
        this.type = type;
        this.hasErrorCode = false;
        this.errorCode = 0;
        this.pointsToSelf = pointsToSelf;
    }
    
    public Type getType()
    {
        return type;
    }
    
    public boolean hasErrorCode()
    {
        return hasErrorCode;
    }
    
    public int getErrorCode()
    {
	    return errorCode;
    }
    
    public boolean pointsToSelf()
    {
        return pointsToSelf;
    }

    public boolean combinesToDoubleFault(ProcessorException original)
    {
        switch (getType()) {
            case DIVIDE_ERROR:
            case TASK_SWITCH:
            case NOT_PRESENT:
            case STACK_SEGMENT:
            case GENERAL_PROTECTION:
                switch (original.getType()) {
                    case DIVIDE_ERROR:
                    case TASK_SWITCH:
                    case NOT_PRESENT:
                    case STACK_SEGMENT:
                    case GENERAL_PROTECTION:
                    case PAGE_FAULT:
                        return true;
                    default:
                        return false;
                }
            case PAGE_FAULT:
                return (original.getType() == Type.PAGE_FAULT);
            default:
                return false;
        }
    }

    public String toString()
    {
	if (hasErrorCode())
	    return "Processor Exception: " + type + " [errorcode:0x" + Integer.toHexString(getErrorCode()) + "]";
	else
	    return "Processor Exception: " + type;
    }
}
