package org.jpc.emulator.memory.codeblock;

import org.jpc.emulator.processor.Processor;
import org.jpc.emulator.memory.AddressSpace;

class SpanningRealModeCodeBlock extends SpanningCodeBlock implements RealModeCodeBlock
{
    private PeekableMemoryStream byteSourceStream = new PeekableMemoryStream();

    private CodeBlockFactory[] factories;
    private int decodes;

    public SpanningRealModeCodeBlock(CodeBlockFactory[] factories)
    {
        this.factories = factories;
    }

    public CodeBlock decode(Processor cpu)
    {
        decodes++;
        if (decodes % 1000 == 0)
            System.out.printf("RM Spanning block at %08x decoded %d times.\n", cpu.getInstructionPointer(), decodes);
        RealModeCodeBlock block = null;
        AddressSpace memory = cpu.physicalMemory;
        int address = cpu.getInstructionPointer();

        for (int i = 0; (i < factories.length) && (block == null); i++) {
            try {
                byteSourceStream.set(memory, address);
                block = factories[i].getRealModeCodeBlock(byteSourceStream);
            } catch (IllegalStateException e) {e.printStackTrace();}
        }

        byteSourceStream.set(null, 0);
        return block;
    }

    public String toString()
    {
        return "Spanning Real Mode CodeBlock";
    }
}
