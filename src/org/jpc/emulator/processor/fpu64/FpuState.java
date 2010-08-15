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


package org.jpc.emulator.processor.fpu64;

// import java.math.BigDecimal;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.*;
import java.io.*;

/**
 * 
 * @author Jeff Tseng
 */
public abstract class FpuState implements Hibernatable
{
    // stack depth (common to all x87 FPU's)
    public final static int STACK_DEPTH = 8;

    public static final int FPU_PRECISION_CONTROL_SINGLE = 0;
    public static final int FPU_PRECISION_CONTROL_DOUBLE = 2;
    public static final int FPU_PRECISION_CONTROL_EXTENDED = 3;

    public static final int FPU_ROUNDING_CONTROL_EVEN = 0;
    public static final int FPU_ROUNDING_CONTROL_DOWN = 1;
    public static final int FPU_ROUNDING_CONTROL_UP = 2;
    public static final int FPU_ROUNDING_CONTROL_TRUNCATE = 3;

    public static final int FPU_TAG_VALID = 0;
    public static final int FPU_TAG_ZERO = 1;
    public static final int FPU_TAG_SPECIAL = 2;
    public static final int FPU_TAG_EMPTY = 3;

    // status word
    // note exception bits are "sticky" - cleared only explicitly
    // accessors to flag an exception - these will set the bit,
    // check the mask, and throw a ProcessorException if unmasked
    public abstract void setInvalidOperation();
    public abstract void setDenormalizedOperand();
    public abstract void setZeroDivide();
    public abstract void setOverflow();
    public abstract void setUnderflow();
    public abstract void setPrecision();
    public abstract void setStackFault();
    public abstract void setTagEmpty(int index);
    public abstract void clearExceptions();
    public abstract void checkExceptions() throws ProcessorException;
    // read accessors
    public abstract boolean getInvalidOperation();
    public abstract boolean getDenormalizedOperand();
    public abstract boolean getZeroDivide();
    public abstract boolean getOverflow();
    public abstract boolean getUnderflow();
    public abstract boolean getPrecision();
    public abstract boolean getStackFault();
    public abstract boolean getErrorSummaryStatus(); // derived from other bits
    public abstract boolean getBusy();//same as fpuErrorSummaryStatus() (legacy)
    public int conditionCode; // 4 bits
    public int top; // top of stack pointer (3 bits)
    // control word
    public abstract boolean getInvalidOperationMask();
    public abstract boolean getDenormalizedOperandMask();
    public abstract boolean getZeroDivideMask();
    public abstract boolean getOverflowMask();
    public abstract boolean getUnderflowMask();
    public abstract boolean getPrecisionMask();
    public boolean infinityControl; // legacy:  not really used anymore
    public abstract int getPrecisionControl();  // 2 bits
    public abstract int getRoundingControl();   // 2 bits
    public abstract void setInvalidOperationMask(boolean value);
    public abstract void setDenormalizedOperandMask(boolean value);
    public abstract void setZeroDivideMask(boolean value);
    public abstract void setOverflowMask(boolean value);
    public abstract void setUnderflowMask(boolean value);
    public abstract void setPrecisionMask(boolean value);
    public abstract void setPrecisionControl(int value);
    public abstract void setRoundingControl(int value);
    public abstract void setAllMasks(boolean value);
    // other registers
    public long lastIP; // last instruction pointer
    public long lastData; // last data (operand) pointer
    public int lastOpcode; // 11 bits

    // x87 access
    public abstract void init();
    public abstract void push(double x) throws ProcessorException;
    public abstract double pop() throws ProcessorException;
    public abstract double ST(int index) throws ProcessorException;
    public abstract void setST(int index, double value);
//     public abstract void pushBig(BigDecimal x) throws ProcessorException;
//     public abstract BigDecimal popBig() throws ProcessorException;
//     public abstract BigDecimal bigST(int index) throws ProcessorException;
//     public abstract void setBigST(int index, BigDecimal value);
    public abstract int getStatus();
    public abstract void setStatus(int w);
    public abstract int getControl();
    public abstract void setControl(int w);
    public abstract int getTagWord();
    public abstract void setTagWord(int w);
    public abstract int getTag(int index);

    public void copyStateInto(FpuState copy)
    {
        copy.conditionCode = conditionCode;
        copy.top = top;
        copy.infinityControl = infinityControl;
        copy.lastIP = lastIP;
        copy.lastData = lastData;
        copy.lastOpcode = lastOpcode;
    }

    public boolean equals(Object another)
    {
        if (!(another instanceof FpuState))
            return false;
        FpuState s = (FpuState) another;
        if ((s.conditionCode != conditionCode) || (s.top != top) || (s.infinityControl != infinityControl) || (s.lastIP != lastIP) || (s.lastData != lastData) || (s.lastOpcode != lastOpcode))
            return false;

        return true;
    }
}
