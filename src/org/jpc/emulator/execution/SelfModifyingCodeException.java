package org.jpc.emulator.execution;

public class SelfModifyingCodeException extends RuntimeException
{
    public SelfModifyingCodeException(String message)
    {
        super(message);
    }
}
