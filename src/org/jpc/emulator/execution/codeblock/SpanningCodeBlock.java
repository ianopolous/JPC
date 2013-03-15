package org.jpc.emulator.execution.codeblock;

import org.jpc.emulator.execution.Executable;
import org.jpc.emulator.execution.decoder.Instruction;
import org.jpc.emulator.processor.Processor;

/**
 * Abstract <code>CodeBlock</code> instance that spans a page boundary and thus needs to be careful
 * about caching the decode result.
 * @author Ian Preston
 */
public abstract class SpanningCodeBlock implements CodeBlock
{
    private CodeBlock lastBlock = null;

    public int getX86Length()
    {
        return 0;
    }

    public int getX86Count()
    {
	try {
	    return lastBlock.getX86Count();
	} catch (NullPointerException e) {
	    return 0;
	}
    }
    
    public Executable.Branch execute(Processor cpu)
    {
        if (lastBlock == null)
            lastBlock = decode(cpu);
        return lastBlock.execute(cpu);
    }

    public void invalidate()
    {
        lastBlock = null;
    }

    /**
     * Forces a new decode on the current memory state.
     * @param cpu processor state on which we are about to execute
     * @return fresh <code>CodeBlock</code> instance
     */
    public abstract CodeBlock decode(Processor cpu);

    public boolean handleMemoryRegionChange(int startAddress, int endAddress)
    {
        return false;
    }

    public String getDisplayString()
    {
        if (lastBlock != null)
            return lastBlock.getDisplayString();
        else
            return "Undecoded Spanning Block";
    }

    public Instruction getInstructions()
    {
        if (lastBlock != null)
            return lastBlock.getInstructions();
        return null;
    }
}
