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

package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.execution.Executable;
import org.jpc.j2se.Option;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.zip.*;
import java.net.*;
import static org.jpc.emulator.execution.decoder.ZygoteOperand.*;
import static org.jpc.emulator.execution.decoder.Table.*;

public class Disassembler
{
    public static final boolean PRINT_DISAM = Option.log_disam.value();
    public static final boolean DEBUG_BLOCKS = Option.debug_blocks.value();
    static ZygoteInstruction[][] itab = new Table().itab_list;
    public static final int MAX_INSTRUCTIONS_PER_BLOCK = Option.max_instructions_per_block.intValue(10000);
    public static final int vendor = VENDOR_INTEL;
    public static ZygoteInstruction ie_invalid = new ZygoteInstruction("invalid", O_NONE, O_NONE, O_NONE, P_none);
    public static ZygoteInstruction ie_pause = new ZygoteInstruction("pause", O_NONE, O_NONE,    O_NONE, P_none);
    public static ZygoteInstruction ie_nop = new ZygoteInstruction("nop", O_NONE, O_NONE, O_NONE, P_none);
    private static Map<String, Constructor<? extends Executable>> rm_instructions = new HashMap();
    private static Map<String, Constructor<? extends Executable>> pm_instructions = new HashMap();
    private static Map<String, Constructor<? extends Executable>> vm_instructions = new HashMap();

    static {
        loadOpcodes(vm_instructions, "vm");
        loadOpcodes(rm_instructions, "rm");
        loadOpcodes(pm_instructions, "pm");
    }

    private static void loadOpcodes(Map<String, Constructor<? extends Executable>> instructions, String mode)
    {
        // load instruction classes
        ClassLoader cl = Disassembler.class.getClassLoader();
        String path = "org/jpc/emulator/execution/opcodes/"+mode;
        try
        {
            List<String> names=  new ArrayList();
            InputStream nameListIn = null;
            if (nameListIn == null)
            {
                Enumeration<URL> resources = cl.getResources(path);
                while (resources.hasMoreElements())
                {
                    String name = resources.nextElement().getFile();
                    if (!name.contains("!"))
                        continue;
                    URL jar = new URL(name.split("!")[0]);
                    ZipInputStream zip = new ZipInputStream(jar.openStream());
                    ZipEntry entry = null;
                    while ((entry = zip.getNextEntry()) != null)
                    {
                        if (entry.getName().startsWith(path))
                            if (entry.getName().endsWith(".class"))
                                names.add(entry.getName().substring(0, entry.getName().length()-6));
                    }
                }
                if (names.size() == 0)
                    System.out.println("Couldn't load any opcodes for "+mode+"!!!!!");
            }
            else
            {
                String line = new BufferedReader(new InputStreamReader(nameListIn)).readLine();
                names = Arrays.asList(line.split(" "));
                System.out.println(names.size() + " opcodes in "+mode);
            }

            for (String file : names)
            {
                try
                {
                    Class c = Class.forName(file.replaceAll("/", "."));
                    int slash = file.lastIndexOf("/");
                    instructions.put(file.substring(slash+1), c.getConstructor(int.class, int.class, int.class, PeekableInputStream.class));
                    //System.out.println("Loaded: "+mode+"/"+file.substring(slash+1));
                } catch (Exception e)
                {}//e.printStackTrace();}
            }   
        } catch (Exception e)
        {e.printStackTrace();}
    }

    public static Executable getExecutable(int mode, int blockStart, Instruction in)
    {
        try
        {
            String gen = in.getGeneralClassName(false, false);
            Map<String, Constructor<? extends Executable>> instructions = null;
            switch (mode)
            {
                case 1:
                    instructions = rm_instructions;
                    break;
                case 2:
                    instructions = pm_instructions;
                    break;
                case 3:
                    instructions = vm_instructions;
                    break;
                default:
                    throw new IllegalStateException("Unknown mode: " + mode);
            }

            if (instructions.containsKey(gen)
                    || instructions.containsKey(in.getGeneralClassName(true, false))
                    || instructions.containsKey(in.getGeneralClassName(false, true))
                    || instructions.containsKey(in.getGeneralClassName(true, true)))
            {
                //System.out.println("Found general class: " + gen);
                Constructor<? extends Executable> c = instructions.get(gen);
                if (c == null)
                    c = instructions.get(in.getGeneralClassName(true, false));
                if (c == null)
                    c = instructions.get(in.getGeneralClassName(false, true));
                if (c == null)
                    c = instructions.get(in.getGeneralClassName(true, true));
                Executable dis = c.newInstance(blockStart, in);
                if (in.pfx.lock != 0)
                    return new Lock(blockStart, dis, in);
                return dis;
            }
        } catch (InstantiationException e)
        {e.printStackTrace();}
        catch (IllegalAccessException e)
        {e.printStackTrace();}
        catch (InvocationTargetException e) {e.printStackTrace();}
        throw new IllegalStateException("Unimplemented opcode: " + ((mode == 2) ? "PM:": (mode == 1) ? "RM:": "VM:") +in.toString() + ", general pattern: " + in.getGeneralClassName(true, true)+".");
    }

    public static String getExecutableName(int mode, Instruction in)
    {
        Map<String, Constructor<? extends Executable>> instructions;
        String prefix;
        switch (mode)
        {
            case 1:
                instructions = rm_instructions;
                prefix = "org.jpc.emulator.execution.opcodes.rm.";
                break;
            case 2:
                instructions = pm_instructions;
                prefix = "org.jpc.emulator.execution.opcodes.pm.";
                break;
            case 3:
                instructions = vm_instructions;
                prefix = "org.jpc.emulator.execution.opcodes.vm.";
                break;
            default:
                throw new IllegalStateException("Unknown mode: " + mode);
        }
        String gen;
        try {
            gen = in.getGeneralClassName(false, false);
        } catch (IllegalStateException e)
        {
//            e.printStackTrace();
            return prefix + "InvalidOpcode/*(Disassembler.java line 189)*/";
        }

        if (instructions.containsKey(gen))
            return prefix + gen;

        if (instructions.containsKey(in.getGeneralClassName(true, false)))
            return prefix + in.getGeneralClassName(true, false);
        if (instructions.containsKey(in.getGeneralClassName(false, true)))
            return prefix + in.getGeneralClassName(false, true);
        if (instructions.containsKey(in.getGeneralClassName(true, true)))
            return prefix + in.getGeneralClassName(true, true);
        if (gen.equals("invalid"))
            return prefix + "InvalidOpcode";
        return prefix + "UnimplementedOpcode";
    }

    public static Executable getEipUpdate(int mode, int blockStart, Instruction prev)
    {
        Map<String, Constructor<? extends Executable>> instructions = null;
        switch (mode)
        {
            case 1:
                instructions = rm_instructions;
                break;
            case 2:
                instructions = pm_instructions;
                break;
            case 3:
                instructions = vm_instructions;
                break;
            default:
                throw new IllegalStateException("Unknown mode: " + mode);
        }
        try {
            return instructions.get("eip_update").newInstance(blockStart, prev);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Instruction disassemble16(PeekableInputStream input)
    {
        input.resetCounter();
        Instruction in = new Instruction();
        in.eip = input.getAddress();
        int start = input.getCounter();
        try {
            get_prefixes(16, input, in);
            search_table(16, input, in);
            do_mode(16, in);
            disasm_operands(16, input, in);
            resolve_operator(16, input, in);
        } catch (IllegalStateException e)
        {
            System.out.println("Invalid RM opcode for bytes: "+getRawBytes(input, start));
            throw e;
        }
        in.x86Length = input.getCounter();
        if (in.operator.equals("invalid"))
            throw new IllegalStateException("Invalid RM opcode for bytes: "+getRawBytes(input, start));
        return in;
    }

    public static Instruction disassemble32(PeekableInputStream input)
    {
        input.resetCounter();
        Instruction in = new Instruction();
        in.eip = input.getAddress();
        int start=  input.getCounter();
        try {
            get_prefixes(32, input, in);
            search_table(32, input, in);
            do_mode(32, in);
            disasm_operands(32, input, in);
            resolve_operator(32, input, in);
        } catch (IllegalStateException e)
        {
            System.out.println("Invalid PM opcode for bytes: "+getRawBytes(input, start));
            throw e;
        }
        in.x86Length = input.getCounter();
        if (in.operator.equals("invalid"))
            throw new IllegalStateException("Invalid PM opcode for bytes: "+getRawBytes(input, start));
        return in;
    }

    public static String getRawBytes(PeekableInputStream input, int start)
    {
        int length = input.getCounter()-start;
        input.seek(-length);
        StringBuilder b = new StringBuilder();
        for(int i=0; i < length; i++)
            b.append(String.format("%02x ", input.readU8()));
        return b.toString();
    }

    private static long decodeCount = 0;

    public static BasicBlock disassembleBlock(PeekableInputStream input, int operand_size, int mode)
    {
        decodeCount++;
        if (decodeCount % 1000 == 0)
            System.out.println("Decoded "+decodeCount + " blocks...");
        int startAddr = (int)input.getAddress();
        boolean debug = false;
        int beginCount = input.getCounter();
        Instruction startin = (operand_size == 32) ? disassemble32(input) : disassemble16(input);
        Instruction currentInsn = startin;

        Executable start = getExecutable(mode, startAddr, currentInsn);
        if (PRINT_DISAM)
        {
            System.out.printf("%d;%s;%s;", operand_size, currentInsn.getGeneralClassName(false, false), currentInsn);
            System.out.println(getRawBytes(input, beginCount));
        }
        if (debug)
                System.out.printf("Disassembled instruction (%d): %s at %x\n", 0, start, input.getAddress());
        Executable current = start;
        int x86Length = currentInsn.x86Length;
        int count = 1;
        boolean delayInterrupts = false;
        while (!currentInsn.isBranch())
        {
            if (((delayInterrupts) || (count >= MAX_INSTRUCTIONS_PER_BLOCK)) && !delayInterrupts(currentInsn))
            {
                Executable eip = getEipUpdate(mode, startAddr, currentInsn);
                current.next = eip;
                if (!delayInterrupts && (MAX_INSTRUCTIONS_PER_BLOCK > 10))
                    System.out.println((String.format("Exceeded maximum number of instructions in a block at %x", startAddr)));
                return constructBlock(startin, start, x86Length, count);
            }
            beginCount = input.getCounter();
            Instruction nextInsn = (operand_size == 32) ? disassemble32(input) : disassemble16(input);
            if (PRINT_DISAM)
            {
                System.out.printf("%d;%s;%s;", operand_size, nextInsn.getGeneralClassName(false, false), nextInsn);
                System.out.println(getRawBytes(input, beginCount));
            }
            Executable next = getExecutable(mode, startAddr, nextInsn);
            if (debug)
                System.out.printf("Disassembled next instruction (%d): %s at %x\n", count, next, input.getAddress());
            count++;
            if (delayInterrupts(currentInsn))
                delayInterrupts = true;

            currentInsn.next = nextInsn;
            currentInsn = nextInsn;
            current.next = next;
            current = next;
            x86Length += nextInsn.x86Length;
        }

        return constructBlock(startin, start, x86Length, count);
    }

    public static boolean delayInterrupts(Instruction in)
    {
        String name = in.toString();
        if (name.equals("sti")) // to delay checking interrupts until 1 instruction after sti
            return true;
        if (name.startsWith("pop ss"))
            return true;
        if (name.startsWith("mov ss"))
            return true;
        return false;
    }

    private static BasicBlock constructBlock(Instruction startin, Executable start, int x86Length, int x86Count)
    {
        if (DEBUG_BLOCKS)
            return new DebugBasicBlock(startin, start, x86Length, x86Count);
        return new BasicBlock(start, x86Length, x86Count);
    }

    public static Instruction disassemble(PeekableInputStream input, int mode)
    {
        input.resetCounter();
        Instruction in = new Instruction();
        get_prefixes(mode, input, in);
        search_table(mode, input, in);
        do_mode(mode, in);
        disasm_operands(mode, input, in);
        resolve_operator(mode, input, in);
        in.x86Length = input.getCounter();
        return in;
    }

    public static class ByteArrayPeekStream implements PeekableInputStream
    {
        byte[] data;
        int index=0;

        public ByteArrayPeekStream(byte[] data)
        {
            this.data = data;
        }

        public void seek(int delta)
        {
            index += delta;
        }

        public void resetCounter()
        {
            index = 0;
        }

        public long getAddress()
        {
            return getCounter();
        }

        public int getCounter()
        {
            return index;
        }

        public byte read8()
        {
            return data[index++];
        }

        public short read16()
        {
            return (short) readU16();
        }

        public int read32()
        {
            return (int) readU32();
        }

        public long readU(long bits)
        {
            if (bits == 8)
                return data[index++] & 0xFF;
            if (bits == 16)
                return readU16();
            if (bits == 32)
                return readU32();
            if (bits == 64)
                return (0xffffffffL & read32()) | (((long)read32()) << 32);
            throw new IllegalStateException("unimplemented read amount " + bits);
        }

        public int readU8()
        {
            return (data[index++] & 0xFF);
        }

        public int readU16()
        {
            return (data[index++] & 0xFF) | ((data[index++] & 0xFF) << 8);
        }

        public long readU32()
        {
            return (data[index++] &0xFF) | ((data[index++] & 0xFF) << 8) | ((data[index++] & 0xFF) << 16) | ((data[index++] & 0xFF) << 24);
        }

        public int peek()
        {
            return data[index] & 0xFF;
        }

        public void forward()
        {
            index++;
        }
    }

    public static void get_prefixes(int mode, PeekableInputStream input, Instruction inst)
    {
        int curr;
        int i=0;
        while(true)
        {
            curr = input.peek();

            if ((mode == 64) && ((curr & 0xF0) == 0x40))
                inst.pfx.rex = curr;
            else
            {
                if (curr == 0x2E)
                {
                    inst.pfx.seg = "cs";
                    inst.pfx.rex = 0;
                }
                else if (curr == 0x36)
                {
                    inst.pfx.seg = "ss";
                    inst.pfx.rex = 0;
                }
                else if (curr == 0x3E)
                {
                    inst.pfx.seg = "ds";
                    inst.pfx.rex = 0;
                }
                else if (curr == 0x26)
                {
                    inst.pfx.seg = "es";
                    inst.pfx.rex = 0;
                }
                else if (curr == 0x64)
                {
                    inst.pfx.seg = "fs";
                    inst.pfx.rex = 0;
                }
                else if (curr == 0x65) 
                {
                    inst.pfx.seg = "gs";
                    inst.pfx.rex = 0;
                }
                else if (curr == 0x67) //adress-size override prefix
                { 
                    inst.pfx.adr = 0x67;
                    inst.pfx.rex = 0;
                }
                else if (curr == 0xF0)
                {
                    inst.pfx.lock = 0xF0;
                    inst.pfx.rex  = 0;
                }
                else if (curr == 0x66)
                {
                    // the 0x66 sse prefix is only effective if no other sse prefix
                    // has already been specified.
                    if (inst.pfx.insn == 0)
                        inst.pfx.insn = 0x66;
                    inst.pfx.opr = 0x66;         
                    inst.pfx.rex = 0;
                }
                else if (curr == 0xF2)
                {
                    inst.pfx.insn  = 0xF2;
                    inst.pfx.repne = 0xF2;
                    inst.pfx.rex   = 0;
                }
                else if (curr == 0xF3)
                {
                    inst.pfx.insn = 0xF3;
                    inst.pfx.rep  = 0xF3;
                    inst.pfx.repe = 0xF3;
                    inst.pfx.rex  = 0;
                }
                else 
                    //No more prefixes
                    break;
                input.forward();
                i++;
            }
        }
        if (i >= MAX_INSTRUCTION_LENGTH)
            throw new IllegalStateException("Max instruction length exceeded");
        
        // speculatively determine the effective operand mode,
        // based on the prefixes and the current disassembly
        // mode. This may be inaccurate, but useful for mode
        // dependent decoding.
        if (mode == 64)
        {
            if (REX_W(inst.pfx.rex) != 0)
                inst.opr_mode = 64;
            else if (inst.pfx.opr != 0)
                inst.opr_mode = 16;
            else if (P_DEF64(inst.zygote.prefix) != 0)
                inst.opr_mode = 64;
            else
                inst.opr_mode = 32;
            if (inst.pfx.adr != 0)
                inst.adr_mode = 32;
            else
                inst.adr_mode = 64;
        }
        else if (mode == 32)
        {
            if (inst.pfx.opr != 0)
                inst.opr_mode = 16;
            else
                inst.opr_mode = 32;
            if (inst.pfx.adr != 0)
                inst.adr_mode = 16;
            else
                inst.adr_mode = 32;
        }
        else if (mode == 16)
        {
            if (inst.pfx.opr != 0)
                inst.opr_mode = 32;
            else
                inst.opr_mode = 16;
            if (inst.pfx.adr != 0)
                inst.adr_mode = 32;
            else
                inst.adr_mode = 16;
        }
    }

    public static void search_table(int mode, PeekableInputStream input, Instruction inst)
    {
        boolean did_peek = false;
        int peek;
        int curr = input.peek();
        input.forward();
        
        int table=0;
        ZygoteInstruction e;

        // resolve xchg, nop, pause crazyness
        if (0x90 == curr)
        {
            if (!((mode == 64) && (REX_B(inst.pfx.rex) != 0)))
            {
                if (inst.pfx.rep != 0)
                {
                    inst.pfx.rep = 0;
                    e = ie_pause;
                }
                else
                    e = ie_nop;
                inst.zygote = e;
                inst.operator = inst.zygote.operator;
                return;
            }
        }
        else if (curr == 0x0F)
        {
            table = ITAB__0F;
            curr = input.peek();
            input.forward();

            // 2byte opcodes can be modified by 0x66, F3, and F2 prefixes
            if (0x66 == inst.pfx.insn)
            {
                if (!itab[ITAB__PFX_SSE66__0F][curr].operator.equals("invalid"))
                {
                    table = ITAB__PFX_SSE66__0F;
                    //inst.pfx.opr = 0;
                }
            }
            else if (0xF2 == inst.pfx.insn)
            {
                if (!itab[ITAB__PFX_SSEF2__0F][curr].operator.equals("invalid"))
                {
                    table = ITAB__PFX_SSEF2__0F;
                    inst.pfx.repne = 0;
                }
            }
            else if (0xF3 == inst.pfx.insn)
            {
                if (!itab[ITAB__PFX_SSEF3__0F][curr].operator.equals("invalid"))
                {
                    table = ITAB__PFX_SSEF3__0F;
                    inst.pfx.repe = 0;
                    inst.pfx.rep  = 0;
                }
            }
        }
        else
            table = ITAB__1BYTE;
        
        int index = curr;

        while (true)
        {
            e = itab[table][index];
            // if operator constant is a standard instruction constant
            // our search is over.
            if (operator.contains(e.operator))
            {
                if (e.operator.equals("invalid"))
                    if (did_peek)
                        input.forward();
                inst.zygote = e;
                inst.operator = e.operator;
                return;
            }

            table = e.prefix;

            if (e.operator.equals("grp_reg"))
            {
                peek     = input.peek();
                did_peek = true;
                index    = MODRM_REG(peek);
            }
            else if (e.operator.equals("grp_mod"))
            {
                peek     = input.peek();
                did_peek = true;
                index    = MODRM_MOD(peek);
                if (index == 3)
                    index = ITAB__MOD_INDX__11;
                else
                    index = ITAB__MOD_INDX__NOT_11;
            }
            else if (e.operator.equals("grp_rm"))
            {
                curr = input.peek();
                input.forward();
                did_peek = false;
                index = MODRM_RM(curr);
            }
            else if (e.operator.equals("grp_x87"))
            {
                curr = input.peek();
                input.forward();
                did_peek = false;
                index    = curr - 0xC0;
            }
            else if (e.operator.equals("grp_osize"))
            {
                if (inst.opr_mode == 64)
                    index = ITAB__MODE_INDX__64;
                else if (inst.opr_mode == 32) 
                    index = ITAB__MODE_INDX__32;
                else
                    index = ITAB__MODE_INDX__16;
            }
            else if (e.operator.equals("grp_asize"))
            {
                if (inst.adr_mode == 64)
                    index = ITAB__MODE_INDX__64;
                else if (inst.adr_mode == 32) 
                    index = ITAB__MODE_INDX__32;
                else
                    index = ITAB__MODE_INDX__16;
            }
            else if (e.operator.equals("grp_mode"))
            {
                if (mode == 64)
                    index = ITAB__MODE_INDX__64;
                else if (mode == 32)
                    index = ITAB__MODE_INDX__32;
                else
                    index = ITAB__MODE_INDX__16;
            }
            else if (e.operator.equals("grp_vendor"))
            {
                if (vendor == VENDOR_INTEL) 
                    index = ITAB__VENDOR_INDX__INTEL;
                else if (vendor == VENDOR_AMD)
                    index = ITAB__VENDOR_INDX__AMD;
                else
                    throw new RuntimeException("unrecognized vendor id");
            }
            else if (e.operator.equals("d3vil"))
                throw new RuntimeException("invalid instruction operator constant Id3vil");
            else
                throw new RuntimeException("invalid instruction operator constant");
        }
            //inst.zygote = e;
            //inst.operator = e.operator;
            //return;
    }

    public static void do_mode(int mode, Instruction inst)
    {
        // propagate prefix effects 
        if (mode == 64)  // set 64bit-mode flags
        {
            // Check validity of  instruction m64 
            if ((P_INV64(inst.zygote.prefix) != 0))
                throw new IllegalStateException("Invalid instruction");

            // effective rex prefix is the  effective mask for the 
            // instruction hard-coded in the opcode map.
            inst.pfx.rex = ((inst.pfx.rex & 0x40) 
                            |(inst.pfx.rex & REX_PFX_MASK(inst.zygote.prefix)));

            // calculate effective operand size 
            if ((REX_W(inst.pfx.rex) != 0) || (P_DEF64(inst.zygote.prefix) != 0))
                inst.opr_mode = 64;
            else if (inst.pfx.opr != 0)
                inst.opr_mode = 16;
            else
                inst.opr_mode = 32;

            // calculate effective address size
            if (inst.pfx.adr != 0)
                inst.adr_mode = 32; 
            else
                inst.adr_mode = 64;
        }
        else if (mode == 32) // set 32bit-mode flags
        { 
            if (inst.pfx.opr != 0)
                inst.opr_mode = 16;
            else
                inst.opr_mode = 32;
            if (inst.pfx.adr != 0)
                inst.adr_mode = 16;
            else 
                inst.adr_mode = 32;
        }
        else if (mode == 16) // set 16bit-mode flags
        {
            if (inst.pfx.opr != 0)
                inst.opr_mode = 32;
            else 
                inst.opr_mode = 16;
            if (inst.pfx.adr != 0)
                inst.adr_mode = 32;
            else 
                inst.adr_mode = 16;
        }
    }

    public static void resolve_operator(int mode, PeekableInputStream input, Instruction inst)
    {
        // far/near flags 
        inst.branch_dist = null;
        // readjust operand sizes for call/jmp instructions
        if (inst.operator.equals("call") || inst.operator.equals("jmp"))
        {
            if (inst.operand[0].size == SZ_WP)
            {
                // WP: 16bit pointer 
                inst.operand[0].size = 16;
                inst.branch_dist = "far";
            }
            else if (inst.operand[0].size == SZ_DP)
            {
                // DP: 32bit pointer
                inst.operand[0].size = 32;
                inst.branch_dist = "far";
            }
            else if (inst.operand[0].size == 8)
                inst.branch_dist = "near";
        }
        else if (inst.operator.equals("3dnow"))
        {
            // resolve 3dnow weirdness 
            inst.operator = itab[ITAB__3DNOW][input.peek()].operator;
        }
        // SWAPGS is only valid in 64bits mode
        if ((inst.operator.equals("swapgs")) && (mode != 64))
            throw new IllegalStateException("SWAPGS only valid in 64 bit mode");
    }
    
    public static void disasm_operands(int mode, PeekableInputStream input, Instruction inst)
    {
        // get type
        int[] mopt = new int[inst.zygote.operand.length];
        for (int i=0; i < mopt.length; i++)
            mopt[i] = inst.zygote.operand[i].type;
        // get size
        int[] mops = new int[inst.zygote.operand.length];
        for (int i=0; i < mops.length; i++)
            mops[i] = inst.zygote.operand[i].size;
        
        if (mopt[2] != OP_NONE)
            inst.operand = new Instruction.Operand[]{new Instruction.Operand(), new Instruction.Operand(), new Instruction.Operand()};
        else if (mopt[1] != OP_NONE)
            inst.operand = new Instruction.Operand[]{new Instruction.Operand(), new Instruction.Operand()};
        else if (mopt[0] != OP_NONE)
            inst.operand = new Instruction.Operand[]{new Instruction.Operand()};
    
        // These flags determine which operand to apply the operand size
        // cast to.
        if (inst.operand.length > 0)
            inst.operand[0].cast = P_C0(inst.zygote.prefix);
        if (inst.operand.length > 1)
            inst.operand[1].cast = P_C1(inst.zygote.prefix);
        if (inst.operand.length > 2)
            inst.operand[2].cast = P_C2(inst.zygote.prefix);

        // iop = instruction operand 
        //iop = inst.operand
        
        if (mopt[0] == OP_A)
            decode_a(mode, inst, input, inst.operand[0]);
        // M[b] ... 
        // E, G/P/V/I/CL/1/S 
        else if ((mopt[0] == OP_M) || (mopt[0] == OP_E))
        {
            if ((mopt[0] == OP_M) && (MODRM_MOD(input.peek()) == 3))
                throw new IllegalStateException("");
            if (mopt[1] == OP_G)
            {
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", inst.operand[1], mops[1], "T_GPR");
                if (mopt[2] == OP_I)
                    decode_imm(mode, inst, input, mops[2], inst.operand[2]);
                else if (mopt[2] == OP_CL)
                {
                    inst.operand[2].type = "OP_REG";
                    inst.operand[2].base = "cl";
                    inst.operand[2].size = 8;
                }
            }
            else if (mopt[1] == OP_P)
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", inst.operand[1], mops[1], "T_MMX");
            else if (mopt[1] == OP_V)
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", inst.operand[1], mops[1], "T_XMM");
            else if (mopt[1] == OP_S)
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", inst.operand[1], mops[1], "T_SEG");
            else
            {
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", null, 0, "T_NONE");
                if (mopt[1] == OP_CL)
                {
                    inst.operand[1].type = "OP_REG";
                    inst.operand[1].base = "cl";
                    inst.operand[1].size = 8;
                }
                else if (mopt[1] == OP_I1)
                {
                    inst.operand[1].type = "OP_IMM";
                    inst.operand[1].lval = 1;
                }
                else if (mopt[1] == OP_I)
                    decode_imm(mode, inst, input, mops[1], inst.operand[1]);
            }
        }
        // G, E/PR[,I]/VR 
        else if (mopt[0] == OP_G)
        {
            if (mopt[1] == OP_M)
            {
                if (MODRM_MOD(input.peek()) == 3)
                    throw new IllegalStateException("invalid");
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_GPR");
            }
            else if (mopt[1] == OP_E)
            {
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_GPR");
                if (mopt[2] == OP_I)
                    decode_imm(mode, inst, input, mops[2], inst.operand[2]);
            }
            else if (mopt[1] == OP_PR)
            {
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_MMX", inst.operand[0], mops[0], "T_GPR");
                if (mopt[2] == OP_I)
                    decode_imm(mode, inst, input, mops[2], inst.operand[2]);
            }
            else if (mopt[1] == OP_VR)
            {
                if (MODRM_MOD(input.peek()) != 3)
                    throw new IllegalStateException("Invalid instruction");
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_XMM", inst.operand[0], mops[0], "T_GPR");
            }
            else if (mopt[1] == OP_W)
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_XMM", inst.operand[0], mops[0], "T_GPR");
        }
        // AL..BH, I/O/DX 
        else if (ops8.contains(mopt[0]))
        {
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = GPR.get("8").get(mopt[0] - OP_AL);
            inst.operand[0].size = 8;
            
            if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
            else if (mopt[1] == OP_DX)
            {
                inst.operand[1].type = "OP_REG";
                inst.operand[1].base = "dx";
                inst.operand[1].size = 16;
            }
            else if (mopt[1] == OP_O)
                decode_o(mode, inst, input, mops[1], inst.operand[1]);
        }
        // rAX[r8]..rDI[r15], I/rAX..rDI/O
        else if (ops2.contains(mopt[0]))
        {
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = resolve_gpr64(mode, inst, mopt[0]);
            
            if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
            else if (ops64.contains(mopt[1]))
            {
                inst.operand[1].type = "OP_REG";
                inst.operand[1].base = resolve_gpr64(mode, inst, mopt[1]);
            }
            else if (mopt[1] == OP_O)
            {
                decode_o(mode, inst, input, mops[1], inst.operand[1]);
                inst.operand[0].size = resolve_operand_size(mode, inst, mops[1]);
            }
        }
        else if (ops3.contains(mopt[0]))
        {
            int gpr = (mopt[0] - OP_ALr8b +(REX_B(inst.pfx.rex) << 3));
            /*if ((gpr in ["ah",	"ch",	"dh",	"bh",
              "spl",	"bpl",	"sil",	"dil",
              "r8b",	"r9b",	"r10b",	"r11b",
              "r12b",	"r13b",	"r14b",	"r15b",
                         ]) && (inst.pfx.rex != 0)) 
                         gpr = gpr + 4;*/
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = GPR.get("8").get(gpr);
            if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
        }
        // eAX..eDX, DX/I 
        else if (ops32.contains(mopt[0]))
        {
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = resolve_gpr32(inst, mopt[0]);
            if (mopt[1] == OP_DX)
            {
                inst.operand[1].type = "OP_REG";
                inst.operand[1].base = "dx";
                inst.operand[1].size = 16;
            }
            else if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
        }
        // ES..GS 
        else if (ops_segs.contains(mopt[0]))
        {
            // in 64bits mode, only fs and gs are allowed 
            if (mode == 64)
                if ((mopt[0] != OP_FS) && (mopt[0] != OP_GS))
                    throw new IllegalStateException("only fs and gs allowed in 64 bit mode");
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = GPR.get("T_SEG").get(mopt[0] - OP_ES);
            inst.operand[0].size = 16;
        }
        // J 
        else if (mopt[0] == OP_J)
        {
            decode_imm(mode, inst, input, mops[0], inst.operand[0]);
            // MK take care of signs
            long bound = 1L << (inst.operand[0].size - 1);
            if (inst.operand[0].lval > bound)
                inst.operand[0].lval = -(((2 * bound) - inst.operand[0].lval) % bound);
            inst.operand[0].type = "OP_JIMM";
        }
        // PR, I 
        else if (mopt[0] == OP_PR)
        {
            if (MODRM_MOD(input.peek()) != 3)
                throw new IllegalStateException("Invalid instruction");
            decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_MMX", null, 0, "T_NONE");
            if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
        }
        // VR, I 
        else if (mopt[0] == OP_VR)
        {
            if (MODRM_MOD(input.peek()) != 3)
                throw new IllegalStateException("Invalid instruction");
            decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_XMM", null, 0, "T_NONE");
            if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
        }
        // P, Q[,I]/W/E[,I],VR 
        else if (mopt[0] == OP_P)
        {
            if (mopt[1] == OP_Q)
            {
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_MMX", inst.operand[0], mops[0], "T_MMX");
                if (mopt[2] == OP_I)
                    decode_imm(mode, inst, input, mops[2], inst.operand[2]);
            }
            else if (mopt[1] == OP_W)
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_XMM", inst.operand[0], mops[0], "T_MMX");
            else if (mopt[1] == OP_VR)
            {
                if (MODRM_MOD(input.peek()) != 3)
                    throw new IllegalStateException("Invalid instruction");
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_XMM", inst.operand[0], mops[0], "T_MMX");
            }
            else if (mopt[1] == OP_E)
            {
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_MMX");
                if (mopt[2] == OP_I)
                    decode_imm(mode, inst, input, mops[2], inst.operand[2]);
            }
        }
        // R, C/D 
        else if (mopt[0] == OP_R)
        {
            if (mopt[1] == OP_C)
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", inst.operand[1], mops[1], "T_CRG");
            else if (mopt[1] == OP_D)
                decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_GPR", inst.operand[1], mops[1], "T_DBG");
        }
        // C, R 
        else if (mopt[0] == OP_C)
            decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_CRG");
        // D, R 
        else if (mopt[0] == OP_D)
            decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_DBG");
        // Q, P 
        else if (mopt[0] == OP_Q)
            decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_MMX", inst.operand[1], mops[1], "T_MMX");
        // S, E 
        else if (mopt[0] == OP_S)
            decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_SEG");
        // W, V 
        else if (mopt[0] == OP_W)
            decode_modrm(mode, inst, input, inst.operand[0], mops[0], "T_XMM", inst.operand[1], mops[1], "T_XMM");
        // V, W[,I]/Q/M/E 
        else if (mopt[0] == OP_V)
        {
            if (mopt[1] == OP_W)
            {
                // special cases for movlps and movhps 
                if (MODRM_MOD(input.peek()) == 3)
                {
                    if (inst.operator.equals("movlps"))
                        inst.operator = "movhlps";
                    else if (inst.operator.equals("movhps"))
                        inst.operator = "movlhps";
                }
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_XMM", inst.operand[0], mops[0], "T_XMM");
                if (mopt[2] == OP_I)
                    decode_imm(mode, inst, input, mops[2], inst.operand[2]);
            }
            else if (mopt[1] == OP_Q)
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_MMX", inst.operand[0], mops[0], "T_XMM");
            else if (mopt[1] == OP_M)
            {
                if (MODRM_MOD(input.peek()) == 3)
                    throw new IllegalStateException("Invalid instruction");
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_XMM");
            }
            else if (mopt[1] == OP_E)
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_GPR", inst.operand[0], mops[0], "T_XMM");
            else if (mopt[1] == OP_PR)
                decode_modrm(mode, inst, input, inst.operand[1], mops[1], "T_MMX", inst.operand[0], mops[0], "T_XMM");
        }
        // DX, eAX/AL
        else if (mopt[0] == OP_DX)
        {
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = "dx";
            inst.operand[0].size = 16;

            if (mopt[1] == OP_eAX)
            {
                inst.operand[1].type = "OP_REG";   
                inst.operand[1].base = resolve_gpr32(inst, mopt[1]);
            }
            else if (mopt[1] == OP_AL)
            {
                inst.operand[1].type = "OP_REG";
                inst.operand[1].base = "al";
                inst.operand[1].size = 8;
            }
        }
        // I, I/AL/eAX
        else if (mopt[0] == OP_I)
        {
            decode_imm(mode, inst, input, mops[0], inst.operand[0]);
            if (mopt[1] == OP_I)
                decode_imm(mode, inst, input, mops[1], inst.operand[1]);
            else if (mopt[1] == OP_AL)
            {
                inst.operand[1].type = "OP_REG";
                inst.operand[1].base = "al";
                inst.operand[1].size = 8;
            }
            else if (mopt[1] == OP_eAX)
            {
                inst.operand[1].type = "OP_REG";  
                inst.operand[1].base = resolve_gpr32(inst, mopt[1]);
            }
        }
        // O, AL/eAX
        else if (mopt[0] == OP_O)
        {
            decode_o(mode, inst, input, mops[0], inst.operand[0]);
            inst.operand[1].type = "OP_REG";
            inst.operand[1].size = resolve_operand_size(mode, inst, mops[0]);
            if (mopt[1] == OP_AL)
                inst.operand[1].base = "al";
            else if (mopt[1] == OP_eAX)
                inst.operand[1].base = resolve_gpr32(inst, mopt[1]);
            else if (mopt[1] == OP_rAX)
                inst.operand[1].base = resolve_gpr64(mode, inst, mopt[1]);     
        }
        // 3
        else if (mopt[0] == OP_I3)
        {
            inst.operand[0].type = "OP_IMM";
            inst.operand[0].lval = 3;
        }
        // ST(n), ST(n) 
        else if (ops_st.contains(mopt[0]))
        {
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = GPR.get("T_ST").get(mopt[0] - OP_ST0);
            inst.operand[0].size = 0;

            if (ops_st.contains(mopt[1]))
            {
                inst.operand[1].type = "OP_REG";
                inst.operand[1].base = GPR.get("T_ST").get(mopt[1] - OP_ST0);
                inst.operand[1].size = 0;
            }
        }
        // AX 
        else if (mopt[0] == OP_AX)
        {
            inst.operand[0].type = "OP_REG";
            inst.operand[0].base = "ax";
            inst.operand[0].size = 16;
        }
        // none 
        else
            for (int i=0; i < inst.operand.length; i++)
                inst.operand[i].type = null;
    }

    private static void decode_a(int mode, Instruction inst, PeekableInputStream input, Instruction.Operand op)
    {
        //Decodes operands of the type seg:offset.
        if (inst.opr_mode == 16)
        {
            // seg16:off16 
            op.type = "OP_PTR";
            op.size = 32;
            op.dis_start = input.getCounter();
            op.ptr = new Instruction.Ptr(input.readU16(), input.readU16());
        }
        else
        {
            // seg16:off32 
            op.type = "OP_PTR";
            op.size = 48;
            op.dis_start = input.getCounter();
            op.ptr = new Instruction.Ptr(input.read32(), input.readU16());
        }
    }

    private static void decode_modrm(int mode, Instruction inst, PeekableInputStream input, Instruction.Operand op, int s, String rm_type, Instruction.Operand opreg, int reg_size, String reg_type)
    {
        // get mod, r/m and reg fields
        int mod = MODRM_MOD(input.peek());
        int rm  = (REX_B(inst.pfx.rex) << 3) | MODRM_RM(input.peek());
        int reg = (REX_R(inst.pfx.rex) << 3) | MODRM_REG(input.peek());

        op.size = resolve_operand_size(mode, inst, s);
        if (reg_type.equals("T_DBG") || reg_type.equals("T_CRG"))
            mod = 3; // force to a register if mov R,D or mov R, C

        // if mod is 11b, then the m specifies a gpr/mmx/sse/control/debug 
        if (mod == 3)
        {
            op.type = "OP_REG";
            if (rm_type ==  "T_GPR")
                op.base = decode_gpr(mode, inst, op.size, rm);
            else   
                op.base = resolve_reg(rm_type, (REX_B(inst.pfx.rex) << 3) |(rm&7));
        }
        // else its memory addressing 
        else
        {
            op.type = "OP_MEM";
            op.seg = inst.pfx.seg;
            // 64bit addressing 
            if (inst.adr_mode == 64)
            {
                op.base = GPR.get("64").get(rm);
            
                // get offset type
                if (mod == 1)
                    op.offset = 8;
                else if (mod == 2)
                    op.offset = 32;
                else if ((mod == 0) &&((rm & 7) == 5))
                {     
                    op.base = "rip";
                    op.offset = 32;
                }
                else
                    op.offset = 0;

                // Scale-Index-Base(SIB)
                if ((rm & 7) == 4)
                {
                    input.forward();
                
                    op.scale = (1 << SIB_S(input.peek())) & ~1;
                    op.index = GPR.get("64").get((SIB_I(input.peek()) |(REX_X(inst.pfx.rex) << 3)));
                    op.base  = GPR.get("64").get((SIB_B(input.peek()) |(REX_B(inst.pfx.rex) << 3)));

                    // special conditions for base reference
                    if (op.index.equals("rsp"))
                    {
                        op.index = null;
                        op.scale = 0;
                    }

                    if ((op.base.equals("rbp")) || (op.base.equals("r13")))
                    {
                        if (mod == 0) 
                            op.base = null;
                        if (mod == 1)
                            op.offset = 8;
                        else
                            op.offset = 32;
                    }
                }
            }
            // 32-Bit addressing mode 
            else if (inst.adr_mode == 32)
            {
                // get base 
                op.base = GPR.get("32").get(rm);

                // get offset type 
                if (mod == 1)
                    op.offset = 8;
                else if (mod == 2)
                    op.offset = 32;
                else if ((mod == 0) && (rm == 5))
                {
                    op.base = null;
                    op.offset = 32;
                }
                else
                    op.offset = 0;

                // Scale-Index-Base(SIB)
                if ((rm & 7) == 4)
                {
                    input.forward();

                    op.scale = (1 << SIB_S(input.peek())) & ~1;
                    op.index = GPR.get("32").get(SIB_I(input.peek()) |(REX_X(inst.pfx.rex) << 3));
                    op.base  = GPR.get("32").get(SIB_B(input.peek()) |(REX_B(inst.pfx.rex) << 3));

                    if (op.index.equals("esp"))
                    {
                        op.index = null;
                        op.scale = 0;
                    }

                    // special condition for base reference 
                    if (op.base.equals("ebp"))
                    {
                        if (mod == 0)
                            op.base = null;
                        if (mod == 1)
                            op.offset = 8;
                        else
                            op.offset = 32;
                    }
                }
            }
            // 16bit addressing mode 
            else
            {
                if (rm == 0)
                {
                    op.base = "bx";
                    op.index = "si";
                }
                else if (rm == 1)
                {
                    op.base = "bx";
                    op.index = "di";
                }
                else if (rm == 2)
                {
                    op.base = "bp";
                    op.index = "si";
                }
                else if (rm == 3) 
                {
                    op.base = "bp";
                    op.index = "di";
                }
                else if (rm == 4) 
                    op.base = "si";
                else if (rm == 5) 
                    op.base = "di";
                else if (rm == 6) 
                    op.base = "bp";
                else if (rm == 7) 
                    op.base = "bx";
                
                if ((mod == 0) && (rm == 6))
                {
                    op.offset = 16;
                    op.base = null;
                }
                else if (mod == 1)
                    op.offset = 8;
                else if (mod == 2) 
                    op.offset = 16;
            }
        }
        input.forward();
        // extract offset, if any 
        if ((op.offset==8) || (op.offset==16) ||(op.offset==32) || (op.offset==64))
        {
            op.dis_start = input.getCounter();
            op.lval  = input.readU(op.offset);
            long bound = 1L << (op.offset - 1);
            if (op.lval > bound)
                op.lval = -(((2 * bound) - op.lval) % bound);
        }

        // resolve register encoded in reg field
        try {
        if (opreg != null)
        {
            opreg.type = "OP_REG";
            opreg.size = resolve_operand_size(mode, inst, reg_size);
            if (reg_type.equals("T_GPR"))
                opreg.base = decode_gpr(mode, inst, opreg.size, reg);
            else
                opreg.base = resolve_reg(reg_type, reg);
        }
        } catch (RuntimeException r)
        {
            throw new IllegalStateException(r);
        }
    }

    private static void decode_imm(int mode, Instruction inst, PeekableInputStream input, int s, Instruction.Operand op)
    {
        op.size = resolve_operand_size(mode, inst, s);
        op.type = "OP_IMM";
        op.imm_start = input.getCounter();
        op.lval = input.readU(op.size);
    }

    private static void decode_o(int mode, Instruction inst, PeekableInputStream input, int s, Instruction.Operand op)
    {
        // offset
        op.seg = inst.pfx.seg;
        op.offset = inst.adr_mode;
        op.dis_start = input.getCounter();
        op.lval = input.readU(inst.adr_mode);
        op.type = "OP_MEM";
        op.size = resolve_operand_size(mode, inst, s);
    }

    private static String resolve_gpr32(Instruction inst, int gpr_op)
    {
        int index = gpr_op - OP_eAX;
        if(inst.opr_mode == 16)
            return GPR.get("16").get(index);
        return GPR.get("32").get(index);
    }

    private static String resolve_gpr64(int mode, Instruction inst, int gpr_op)
    {
        int index = 0;
        if ((OP_rAXr8 <= gpr_op) && (OP_rDIr15 >= gpr_op))
            index = (gpr_op - OP_rAXr8) |(REX_B(inst.pfx.rex) << 3);
        else
            index = gpr_op - OP_rAX;
        if (inst.opr_mode == 16)
            return GPR.get("16").get(index);
        else if ((mode == 32) || !((inst.opr_mode == 32) && (REX_W(inst.pfx.rex) == 0)))
            return GPR.get("32").get(index);
        return GPR.get("64").get(index);
    }

    private static int resolve_operand_size(int mode, Instruction inst, int s)
    {
        if (s ==  SZ_V)
            return inst.opr_mode;
        else if (s ==  SZ_Z)  
            if (inst.opr_mode == 16)
                return 16;
            else
                return 32;
        else if (s ==  SZ_P)  
            if (inst.opr_mode == 16)
                return SZ_WP;
            else
                return SZ_DP;
        else if (s ==  SZ_MDQ)
            if (inst.opr_mode == 16)
                return 32;
            else
                return inst.opr_mode;
        else if (s ==  SZ_RDQ)
            if (mode == 64)
                return 64;
            else
                return 32;
        else
            return s;
    }

    private static String decode_gpr(int mode, Instruction inst, int s, int rm)
    {
        s = resolve_operand_size(mode, inst, s);
          
        if (s == 64)
            return GPR.get("64").get(rm);
        else if ((s == SZ_DP) || (s == 32))
            return GPR.get("32").get(rm);
        else if ((s == SZ_WP) || (s == 16))
            return GPR.get("16").get(rm);
        else if (s == 8)
        {
            if ((mode == 64) && (inst.pfx.rex != 0))
            {
                if (rm >= 4)
                    return GPR.get("8").get(rm+4);
                return GPR.get("8").get(rm);
            }
            else
                return GPR.get("8").get(rm);
        }
        else
            return null;
    }

    private static String resolve_reg(String regtype, int i)
    {
        return GPR.get(regtype).get(i);
    }
}