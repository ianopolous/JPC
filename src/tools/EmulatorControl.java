/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine

    Copyright (C) 2012-2013 Ian Preston

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

    Details (including contact information) can be found at:

    jpc.sourceforge.net
    or the developer website
    sourceforge.net/projects/jpc/

    End of licence header
*/

package tools;

import java.io.IOException;

public interface EmulatorControl
{
    // return disam of next instruction
    public String executeInstruction() throws IOException;
    public int[] getState() throws IOException;
    public byte[] getCMOS() throws IOException;
    public int[] getPit() throws IOException;
    public int getPITIntTargetEIP() throws IOException;
    public Integer savePage(Integer page, byte[] data, Boolean linear) throws IOException;

    public void setState(int[] state) throws IOException;
    public void keysDown(String keys);
    public void keysUp(String keys);
    public void sendMouse(Integer dx, Integer dy, Integer dz, Integer buttons);

}
