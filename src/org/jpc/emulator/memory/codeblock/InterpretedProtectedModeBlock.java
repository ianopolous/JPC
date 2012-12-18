package org.jpc.emulator.memory.codeblock;

import org.jpc.emulator.execution.decoder.*;
import org.jpc.emulator.execution.*;

public class InterpretedProtectedModeBlock extends BasicBlock implements ProtectedModeCodeBlock
{
    public InterpretedProtectedModeBlock(Executable start, int x86Length, int x86Count)
    {
        super(start, x86Length, x86Count);
    }

    public InterpretedProtectedModeBlock(BasicBlock b)
    {
        this(b.start, b.x86Length, b.x86Count);
    }
}
