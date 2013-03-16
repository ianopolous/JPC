package org.jpc.emulator.execution.decoder;

import org.jpc.emulator.execution.Executable;

/**
 * Converts a stream of x86 bytes into an Executable Opcode
 * @author Ian Preston
 */
public interface OpcodeDecoder
{
    // source will be pointing at the modrm byte or the first byte after it
    public Executable decodeOpcode(int blockStart, int eip, int prefices, PeekableInputStream source);
}
