/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

    Copyright (C) 2012-2013 Ian Preston

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

    End of licence header
*/

package org.jpc.emulator.processor;

public class Tasking
{
    public static enum Source {CALL, INT, IRET, JUMP}

    public static void task_switch(Processor cpu, Source source, ProtectedModeSegment tss, boolean hasErrorCode, int errorCode)
    {
        // STEP 1: The following checks are made before calling task_switch(),
        //         for JUMP & CALL only. These checks are NOT made for exceptions,
        //         interrupts & IRET.
        //
        //   1) TSS DPL must be >= CPL
        //   2) TSS DPL must be >= TSS selector RPL
        //   3) TSS descriptor is not busy.

        // STEP 2: The processor performs limit-checking on the target TSS
        //         to verify that the TSS limit is greater than or equal
        //         to 67h (2Bh for 16-bit TSS).

        int newTssMax;
        if (tss.getType() <= 3) // 1, 3
            newTssMax = 0x2B;
        else // 9, 11
            newTssMax = 0x67;

        int newBase32 = tss.getBase();
        int newTSSLimit = tss.getLimit();

        if (newTSSLimit < newTssMax)
            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, tss.getSelector() & 0xfffc, true);


        // TODO support SVM

        // TODO support VMX

        int oldTssMax;
        if (cpu.tss.getType() <= 3)
            oldTssMax = 0x29;
        else
            oldTssMax = 0x5f;
        int oldBase32 = cpu.tss.getBase();
        int oldTssLimit = cpu.tss.getLimit();

        if (oldTssLimit < oldTssMax)
            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, cpu.tss.getSelector() & 0xfffc, true);

        if (oldBase32 == newBase32)
            System.out.println("TSS: Switching to the same Task!");

        // ensure old and new TSS are in the TLB
        if (cpu.pagingEnabled())
        {

        }

        // Step 3: If JMP or IRET, clear busy bit in old task TSS descriptor, otherwise leave set.
        if (source == Source.JUMP || source == Source.IRET)
        {
            int addr = cpu.gdtr.getBase() + 8*(cpu.tss.getSelector() & 0xfffc) + 4;
            int tmp = cpu.linearMemory.getDoubleWord(addr);
            tmp &= ~0x200;
            cpu.linearMemory.setDoubleWord(addr, tmp);
        }

        // STEP 4: If the task switch was initiated with an IRET instruction,
        //         clears the NT flag in a temporarily saved EFLAGS image;
        //         if initiated with a CALL or JMP instruction, an exception, or
        //         an interrupt, the NT flag is left unchanged.
        int oldEFlags = cpu.getEFlags();
        if ((tss instanceof ProtectedModeSegment.Busy16BitTSS) || (tss instanceof ProtectedModeSegment.Busy32BitTSS))
            oldEFlags &= ~Processor.EFLAGS_NT_MASK;

        // STEP 5: Save the current task state in the TSS. Up to this point,
        //         any exception that occurs aborts the task switch without
        //         changing the processor state.

        // save current machine state in old task's TSS
        if (cpu.tss.getType() <= 3)
        {
            // check that we won't page fault while writing
            if (cpu.pagingEnabled())
            {
                int start = oldBase32 + 14;
                int end = oldBase32 + 41;
                cpu.linearMemory.setByte(start, cpu.linearMemory.getByte(start));
                cpu.linearMemory.setByte(end, cpu.linearMemory.getByte(end));
            }
            boolean isSup = cpu.linearMemory.isSupervisor();
            try {
                cpu.linearMemory.setSupervisor(true);
                cpu.linearMemory.setWord(oldBase32 + 14, (short) cpu.eip);
                cpu.linearMemory.setWord(oldBase32 + 16, (short) oldEFlags);
                cpu.linearMemory.setWord(oldBase32 + 18, cpu.r_ax.get16());
                cpu.linearMemory.setWord(oldBase32 + 20, cpu.r_cx.get16());
                cpu.linearMemory.setWord(oldBase32 + 22, cpu.r_dx.get16());
                cpu.linearMemory.setWord(oldBase32 + 24, cpu.r_bx.get16());
                cpu.linearMemory.setWord(oldBase32 + 26, cpu.r_sp.get16());
                cpu.linearMemory.setWord(oldBase32 + 28, cpu.r_bp.get16());
                cpu.linearMemory.setWord(oldBase32 + 30, cpu.r_si.get16());
                cpu.linearMemory.setWord(oldBase32 + 32, cpu.r_di.get16());
                cpu.linearMemory.setWord(oldBase32 + 34, (short) cpu.es());
                cpu.linearMemory.setWord(oldBase32 + 36, (short) cpu.cs());
                cpu.linearMemory.setWord(oldBase32 + 38, (short) cpu.ss());
                cpu.linearMemory.setWord(oldBase32 + 40, (short) cpu.ds());
            } finally {
                cpu.linearMemory.setSupervisor(isSup);
            }
        } else {
            // check that we won't page fault while writing
            if (cpu.pagingEnabled())
            {
                int start = oldBase32 + 0x20;
                int end = oldBase32 + 0x5d;
                cpu.linearMemory.setByte(start, cpu.linearMemory.getByte(start));
                cpu.linearMemory.setByte(end, cpu.linearMemory.getByte(end));
            }
            boolean isSup = cpu.linearMemory.isSupervisor();
            try {
                cpu.linearMemory.setSupervisor(true);
                cpu.linearMemory.setDoubleWord(oldBase32 + 0x20, cpu.eip);
                cpu.linearMemory.setDoubleWord(oldBase32 + 0x24, oldEFlags);
                cpu.linearMemory.setDoubleWord(oldBase32 + 0x28, cpu.r_eax.get32());
                cpu.linearMemory.setDoubleWord(oldBase32 + 0x2c, cpu.r_ecx.get32());
                cpu.linearMemory.setDoubleWord(oldBase32 + 0x30, cpu.r_edx.get32());
                cpu.linearMemory.setDoubleWord(oldBase32 + 0x34, cpu.r_ebx.get32());
                cpu.linearMemory.setDoubleWord(oldBase32 + 0x38, cpu.r_esp.get32());
                cpu.linearMemory.setDoubleWord(oldBase32 + 0x3c, cpu.r_ebp.get32());
                cpu.linearMemory.setDoubleWord(oldBase32 + 0x40, cpu.r_esi.get32());
                cpu.linearMemory.setDoubleWord(oldBase32 + 0x44, cpu.r_edi.get32());
                cpu.linearMemory.setWord(oldBase32 + 0x48, (short) cpu.es());
                cpu.linearMemory.setWord(oldBase32 + 0x4c, (short) cpu.cs());
                cpu.linearMemory.setWord(oldBase32 + 0x50, (short) cpu.ss());
                cpu.linearMemory.setWord(oldBase32 + 0x54, (short) cpu.ds());
                cpu.linearMemory.setWord(oldBase32 + 0x58, (short) cpu.fs());
                cpu.linearMemory.setWord(oldBase32 + 0x5c, (short) cpu.gs());
            } finally {
                cpu.linearMemory.setSupervisor(isSup);
            }
        }

        // effect on link field of new task
        if ((source == Source.CALL) || (source == Source.INT))
        {
            boolean isSup = cpu.linearMemory.isSupervisor();
            try {
                cpu.linearMemory.setSupervisor(true);
                cpu.linearMemory.setWord(newBase32, (short) cpu.tss.getSelector());
            } finally {
                cpu.linearMemory.setSupervisor(isSup);
            }
        }

        // STEP 6: The new-task state is loaded from the TSS
        int newEip, newEflags, newEax, newEcx, newEdx, newEbx, newEsp, newEbp, newEsi, newEdi;
        int newEs, newCs, newSs, newDs, newFs, newGs, newLdt, newCR3, trap_word;
        if (cpu.tss.getType() <= 3)
        {
            boolean isSup = cpu.linearMemory.isSupervisor();
            try {
                cpu.linearMemory.setSupervisor(true);
                newEip = 0xffff & cpu.linearMemory.getWord(newBase32 + 14);
                newEflags = 0xffff & cpu.linearMemory.getWord(newBase32 + 16);
                newEax = cpu.linearMemory.getWord(newBase32 + 18) | 0xffff0000;
                newEcx = cpu.linearMemory.getWord(newBase32 + 20) | 0xffff0000;
                newEdx = cpu.linearMemory.getWord(newBase32 + 22) | 0xffff0000;
                newEbx = cpu.linearMemory.getWord(newBase32 + 24) | 0xffff0000;
                newEsp = cpu.linearMemory.getWord(newBase32 + 26) | 0xffff0000;
                newEbp = cpu.linearMemory.getWord(newBase32 + 28) | 0xffff0000;
                newEsi = cpu.linearMemory.getWord(newBase32 + 30) | 0xffff0000;
                newEdi = cpu.linearMemory.getWord(newBase32 + 32) | 0xffff0000;

                newEs = cpu.linearMemory.getWord(newBase32 + 34) & 0xffff;
                newCs = cpu.linearMemory.getWord(newBase32 + 36) & 0xffff;
                newSs = cpu.linearMemory.getWord(newBase32 + 38) & 0xffff;
                newDs = cpu.linearMemory.getWord(newBase32 + 40) & 0xffff;
                newLdt = cpu.linearMemory.getWord(newBase32 + 42) & 0xffff;

                newFs = newGs = 0;
                // No CR3 change for 286 task switch
                newCR3 = trap_word = 0;
            } finally {
                cpu.linearMemory.setSupervisor(isSup);
            }
        } else {
            boolean isSup = cpu.linearMemory.isSupervisor();
            try {
                cpu.linearMemory.setSupervisor(true);
                if (cpu.pagingEnabled())
                    newCR3 = cpu.linearMemory.getDoubleWord(newBase32 + 0x1c);
                else
                newCR3 = 0;

                newEip = cpu.linearMemory.getDoubleWord(newBase32 + 0x20);
                newEflags = cpu.linearMemory.getDoubleWord(newBase32 + 0x24);
                newEax = cpu.linearMemory.getDoubleWord(newBase32 + 0x28);
                newEcx = cpu.linearMemory.getDoubleWord(newBase32 + 0x2c);
                newEdx = cpu.linearMemory.getDoubleWord(newBase32 + 0x30);
                newEbx = cpu.linearMemory.getDoubleWord(newBase32 + 0x34);
                newEsp = cpu.linearMemory.getDoubleWord(newBase32 + 0x38);
                newEbp = cpu.linearMemory.getDoubleWord(newBase32 + 0x3c);
                newEsi = cpu.linearMemory.getDoubleWord(newBase32 + 0x40);
                newEdi = cpu.linearMemory.getDoubleWord(newBase32 + 0x44);

                newEs = cpu.linearMemory.getWord(newBase32 + 0x48) & 0xffff;
                newCs = cpu.linearMemory.getWord(newBase32 + 0x4c) & 0xffff;
                newSs = cpu.linearMemory.getWord(newBase32 + 0x50) & 0xffff;
                newDs = cpu.linearMemory.getWord(newBase32 + 0x54) & 0xffff;
                newFs = cpu.linearMemory.getWord(newBase32 + 0x58) & 0xffff;
                newGs = cpu.linearMemory.getWord(newBase32 + 0x5c) & 0xffff;
                newLdt = cpu.linearMemory.getWord(newBase32 + 0x60) & 0xffff;
                trap_word = cpu.linearMemory.getWord(newBase32 + 0x64) & 0xffff;
            } finally {
                cpu.linearMemory.setSupervisor(isSup);
            }
        }

        // Step 7: If CALL, interrupt, or JMP, set busy flag in new task's TSS descriptor. If IRET, leave set.
        if (source != Source.IRET)
        {
            // set the new task's busy bit
            int addr = cpu.gdtr.getBase() + 8*(cpu.tss.getSelector() & 0xfffc) + 4;
            int tmp = cpu.linearMemory.getDoubleWord(addr);
            tmp |= 0x200;
            cpu.linearMemory.setDoubleWord(addr, tmp);
            tss = (ProtectedModeSegment) cpu.getSegment(tss.getSelector());
        }

        // Commit point.  At this point, we commit to the new
        // context.  If an unrecoverable error occurs in further
        // processing, we complete the task switch without performing
        // additional access and segment availablility checks and
        // generate the appropriate exception prior to beginning
        // execution of the new task.

        // Step 8: Load the task register with the new TSS
        cpu.tss = tss;

        // Step 9: Set TS flag in the CR0 image stored in the new task TSS.
        cpu.setCR0(cpu.getCR0() | cpu.CR0_TASK_SWITCHED);

        // Task switch clears LE/L3/L2/L1/L0 in DR7
        cpu.dr7 &= ~0x00000155;

        // Step 10: If call or interrupt, set the NT flag in the eflags
        //          image stored in new task's TSS.  If IRET or JMP,
        //          NT is restored from new TSS eflags image. (no change)
        if ((source == Source.CALL) || (source == Source.INT))
            newEflags |= Processor.EFLAGS_NT_MASK;

        // Step 11: Load the new task (dynamic) state from new TSS.
        //          Any errors associated with loading and qualification of
        //          segment descriptors in this step occur in the new task's
        //          context.  State loaded here includes LDTR, CR3,
        //          EFLAGS, EIP, general purpose registers, and segment
        //          descriptor parts of the segment registers.

        cpu.eip = newEip;
        cpu.r_eax.set32(newEax);
        cpu.r_ecx.set32(newEcx);
        cpu.r_edx.set32(newEdx);
        cpu.r_ebx.set32(newEbx);
        cpu.r_esp.set32(newEsp);
        cpu.r_ebp.set32(newEbp);
        cpu.r_esi.set32(newEsi);
        cpu.r_edi.set32(newEdi);

        cpu.setEFlags(newEflags, Processor.EFLAGS_VALID_MASK);

        // Set selectors TODO
//        cpu.cs(newCs);
//        cpu.ss(newSs);
//        cpu.ds(newDs);
//        cpu.es(newEs);
//        cpu.fs(newFs);
//        cpu.gs(newGs);

        if (newLdt == 0)
        {
            cpu.ldtr = SegmentFactory.NULL_SEGMENT;
        }
        else
        {
            Segment newSegment = cpu.getSegment(newLdt & ~0x4);
            cpu.ldtr = newSegment;
        }

        if ((tss.getType() >= 9) && (cpu.pagingEnabled()))
        {
            // change CR3 only if it actually modified
            if (newCR3 != cpu.getCR3())
            {
                cpu.setCR3(newCR3);

                if (Processor.cpuLevel >= 6)
                {
                    if (cpu.pagingEnabled() && cpu.physicalAddressExtension())
                    {
                        // TODO
                        // clear PDPTRs before raising task switch exception

                    }
                }
            }
        }

        int saveCPL = cpu.getCPL();
        // set CPL to 3 to force a privilege level change and stack switch if SS is not properly loaded
        cpu.setCPL(3);

        if ((newLdt & 4) != 0)
            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newLdt & 0xfffc, true);

        if ((newLdt & 0xfffc) != 0)
        {
            if (!cpu.ldtr.isPresent() || !cpu.ldtr.isSystem() || !(cpu.ldtr instanceof ProtectedModeSegment.LDT))
                throw new ProcessorException(ProcessorException.Type.NOT_PRESENT, newLdt & 0xfffc, true);
        } else {
            // OK
        }

        if (cpu.isVirtual8086Mode())
        {
            cpu.ss(newSs);
            cpu.ds(newDs);
            cpu.es(newEs);
            cpu.fs(newFs);
            cpu.gs(newGs);
            cpu.cs(newCs);
        } else {
            // SS
            if ((newSs & 0xfffc) == 0)
                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newSs & 0xfffc, true);

            Segment ss = cpu.loadSegment(newSs, true);

            if (!((ProtectedModeSegment)ss).isDataWritable() || ((ProtectedModeSegment)ss).isCode())
                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newSs & 0xfffc, true);

            if (ss.getDPL() != (newCs & 3))
                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newSs & 0xfffc, true);

            if (ss.getDPL() != ss.getRPL())
                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newSs & 0xfffc, true);

            if (touch_segment((ProtectedModeSegment)ss, cpu))
                ss = cpu.loadSegment(newSs, true);
            cpu.ss(ss);

            cpu.setCPL(saveCPL);

            Segment ds = cpu.loadSegment(newDs);
            checkSegment(ds, newCs & 3, cpu);
            cpu.ds(ds);
            Segment es = cpu.loadSegment(newEs);
            checkSegment(es, newCs & 3, cpu);
            cpu.es(es);
            Segment fs = cpu.loadSegment(newFs);
            checkSegment(fs, newCs & 3, cpu);
            cpu.fs(fs);
            Segment gs = cpu.loadSegment(newGs);
            checkSegment(gs, newCs & 3, cpu);
            cpu.gs(gs);

            // CS
            if ((newCs & 0xfffc) == 0)
                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newCs & 0xfffc, true);

            Segment cs = cpu.loadSegment(newCs);

            if (((ProtectedModeSegment)cs).isDataWritable())
                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newCs & 0xfffc, true);

            if (!((ProtectedModeSegment) cs).isConforming() && cs.getDPL() != cs.getRPL())
                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newCs & 0xfffc, true);

            if (((ProtectedModeSegment) cs).isConforming() && cs.getDPL() > cs.getRPL())
                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, newCs & 0xfffc, true);

            if (touch_segment((ProtectedModeSegment)cs, cpu))
                cs = cpu.loadSegment(newCs);
            // TODO support cs limit demotion
            cpu.cs(cs);

            if (Processor.cpuLevel >= 4)
            {
                cpu.checkAlignmentChecking();
            }

            if ((tss.getType() >= 9) && ((trap_word & 1) != 0))
                // TODO debug trap

            if (Processor.cpuLevel >= 6)
            {
                // TODO handle SSE mode change

                // TODO if support AVX, handleAVXmodeChange()
            }

            if (hasErrorCode)
            {
                boolean isSup = cpu.linearMemory.isSupervisor();
                try {
                    cpu.linearMemory.setSupervisor(true);
                    if (tss.getType() >= 9)
                        cpu.push32(errorCode);
                    else
                        cpu.push16((short) errorCode);
                } finally {
                    cpu.linearMemory.setSupervisor(isSup);
                }
            }

            if (cpu.eip > cpu.cs.getLimit())
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);
        }
    }

    private static boolean checkSegment(Segment seg, int cs_rpl, Processor cpu)
    {
        if (seg instanceof SegmentFactory.NullSegment)
            return false;

        ProtectedModeSegment s = (ProtectedModeSegment) seg;
        if (s.isSystem() || (s.isCode() && !s.isCodeReadable()))
            throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, s.getSelector() & 0xfffc, true);

        if (s.isDataWritable() || !s.isConforming())
            if ((s.getRPL() > s.getDPL()) || (cs_rpl > s.getDPL()))
                throw new ProcessorException(ProcessorException.Type.TASK_SWITCH, s.getSelector() & 0xfffc, true);

        return touch_segment(s, cpu);
    }

    public static boolean touch_segment(ProtectedModeSegment s, Processor cpu)
    {
        // set accessed bit
        if (!s.isAccessed())
        {
            boolean isSup = cpu.linearMemory.isSupervisor();
            try {
                cpu.linearMemory.setSupervisor(true);
                if ((s.getSelector() & 0x4) != 0)
                    cpu.ldtr.VMsetByte((s.getSelector() & 0xfff8) + 5, (byte) (cpu.ldtr.getByte((s.getSelector() & 0xfff8) + 5) | 1));
                else
                    cpu.gdtr.VMsetByte((s.getSelector() & 0xfff8) + 5, (byte) (cpu.gdtr.getByte((s.getSelector() & 0xfff8) + 5) | 1));
            } finally {
                cpu.linearMemory.setSupervisor(isSup);
            }
            return true;
        }
        return false;
    }
}
