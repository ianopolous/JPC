package org.jpc.emulator.execution.opcodes.pm;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.*;
import static org.jpc.emulator.processor.Processor.*;

public class cli extends Executable
{

    public cli(int blockStart, Instruction parent)
    {
        super(blockStart, parent);
    }

    public Branch execute(Processor cpu)
    {
        /* uncomment if we support VME
                        if ((cpu.getCPL() == 3) && ((cpu.getCR4() & 2) != 0)) {
                            if (cpu.getIOPrivilegeLevel() < 3)
                            {
                               cpu.eflagsInterruptEnableSoon = false;
                               return;
                            }
                        } else
                    */
                    {
                        if (cpu.getIOPrivilegeLevel() < cpu.getCPL())
                        {
                            System.out.println("IOPL=" + cpu.getIOPrivilegeLevel() + ", CPL=" + cpu.getCPL() + "CR4=0x" + Integer.toHexString(cpu.getCR4()));
                            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);
                        }
                    }
                    cpu.eflagsInterruptEnable = false;
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