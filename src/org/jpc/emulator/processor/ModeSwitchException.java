package org.jpc.emulator.processor;

public class ModeSwitchException extends RuntimeException
{
    public static final ModeSwitchException PROTECTED_MODE_EXCEPTION = new ModeSwitchException();
    public static final ModeSwitchException REAL_MODE_EXCEPTION = new ModeSwitchException();
    public static final ModeSwitchException VIRTUAL8086_MODE_EXCEPTION = new ModeSwitchException();

    private int x86Count=0;

    private ModeSwitchException()
    {
    }

    public void setX86Count(int count)
    {
        this.x86Count = count;
    }

    public int getX86Count()
    {
        return x86Count;
    }

    public String toString()
    {
        if (this == REAL_MODE_EXCEPTION)
            return "Switched to REAL mode";
        else if (this == PROTECTED_MODE_EXCEPTION)
            return "Switched to PROTECTED mode";
        else if (this == VIRTUAL8086_MODE_EXCEPTION)
	    return "Switched to VIRTUAL 8086 mode";
        else
            return "Switched to unknown mode";
    }
}
