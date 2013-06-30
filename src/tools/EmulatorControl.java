package tools;

import java.io.IOException;

public interface EmulatorControl
{
    // return disam of next instruction
    public String executeInstruction() throws IOException;
    public int[] getState() throws IOException;
    public void setState(int[] state) throws IOException;

    public Integer savePage(Integer page, byte[] data, Boolean linear) throws IOException;

    public void keysDown(String keys);
    public void keysUp(String keys);
    public void sendMouse(Integer dx, Integer dy, Integer dz, Integer buttons);
    public byte[] getCMOS() throws IOException;
    public int[] getPit() throws IOException;
}
