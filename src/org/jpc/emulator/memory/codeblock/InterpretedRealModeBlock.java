package org.jpc.emulator.memory.codeblock;

import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.execution.*;

public class InterpretedRealModeBlock extends BasicBlock implements RealModeCodeBlock
{
    public InterpretedRealModeBlock(Executable start, int x86Length, int x86Count)
    {
        super(start, x86Length, x86Count);
    }

    public InterpretedRealModeBlock(BasicBlock b)
    {
        this(b.start, b.x86Length, b.x86Count);
    }
}
