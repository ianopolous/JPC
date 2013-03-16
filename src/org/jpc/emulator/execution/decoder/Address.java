package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.processor.Processor;

public abstract class Address
{
    public abstract int getBase(Processor cpu);

    public abstract int get(Processor cpu);
}