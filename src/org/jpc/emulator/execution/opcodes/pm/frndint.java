package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class frndint extends Executable
{

    public frndint(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        double freg0 = cpu.fpu.ST(0);
        if (!Double.isInfinite(freg0)) // preserve infinities
        {
            switch(cpu.fpu.getRoundingControl())
            {
                case FpuState.FPU_ROUNDING_CONTROL_EVEN:
                    cpu.fpu.setST(0, Math.rint(freg0));
                    break;
                case FpuState.FPU_ROUNDING_CONTROL_DOWN:
                    cpu.fpu.setST(0, Math.floor(freg0));
                    break;
                case FpuState.FPU_ROUNDING_CONTROL_UP:
                    cpu.fpu.setST(0, Math.ceil(freg0));
                    break;
                case FpuState.FPU_ROUNDING_CONTROL_TRUNCATE:
                    cpu.fpu.setST(0, Math.signum(freg0) * Math.floor(Math.abs(freg0)));
                    break;
                default:
                    throw new IllegalStateException("Invalid rounding control value");
            }
        }

        return Branch.None;
    }

    public boolean isBranch()
    {
        return false;
    }

    public String toString()
    {
        return this.getClass().getName();
    }
}