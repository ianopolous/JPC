package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.execution.*;
import org.jpc.emulator.memory.codeblock.CodeBlock;
import org.jpc.emulator.processor.*;
import org.jpc.j2se.Option;

import static org.jpc.emulator.execution.Executable.*;

public class DebugBasicBlock extends BasicBlock
{
    public final Instruction start;
    private final String contents;

    public DebugBasicBlock(Instruction startin, Executable start, int x86Length, int x86Count)
    {
        super(start, x86Length, x86Count);
        this.start = startin;
        StringBuilder b = new StringBuilder();
        while (startin != null)
        {
            b.append(startin.toString());
            b.append("\n");
            startin = startin.next;
        }
        contents = b.toString();
    }

    public String getDisplayString()
    {
        return contents;
    }

    public Instruction getInstructions()
    {
        return start;
    }
}