package org.jpc.emulator.execution.codeblock;

public class SpanningDecodeException extends RuntimeException
{
    private SpanningCodeBlock spanning;

    public SpanningDecodeException(SpanningCodeBlock spanning)
    {
        super("Spanning Decode Exception");
        this.spanning = spanning;
    }

    public SpanningCodeBlock getBlock()
    {
        return spanning;
    }
}
