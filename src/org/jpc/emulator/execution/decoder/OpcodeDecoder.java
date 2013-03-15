package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.execution.Executable;

/**
 * Converts a stream of x86 bytes into an Executable Opcode
 * @author Ian Preston
 */
public interface OpcodeDecoder
{
    public Executable decodeOpcode(int blockStart, int prefices, PeekableInputStream source);
}
