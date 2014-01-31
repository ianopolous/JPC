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
    public static int CRO_INDEX = 36;
    public static int GDT_BASE_INDEX = 24;
    public static int GDT_LIMIT_INDEX = 25;
    public static int ES_LIMIT_INDEX = 17;
    public static int CS_LIMIT_INDEX = 18;
    public static int SS_LIMIT_INDEX = 19;
    public static int DS_LIMIT_INDEX = 20;
    public static int FS_LIMIT_INDEX = 21;
    public static int GS_LIMIT_INDEX = 22;
    public static int ES_BASE_INDEX = 30;
    public static int CS_BASE_INDEX = 31;
    public static int SS_BASE_INDEX = 31;
    public static int DS_BASE_INDEX = 31;
    public static int FS_BASE_INDEX = 31;
    public static int GS_BASE_INDEX = 31;
    public static String[] names = new String[]
        {
            "eax", "ecx", "edx", "ebx", "esp", "ebp", "esi", "edi","eip", "flags",
            /*10*/"es", "cs", "ss", "ds", "fs", "gs", "ticks",
            /*17*/"es-lim", "cs-lim", "ss-lim", "ds-lim", "fs-lim", "gs-lim", "cs-prop",
            /*24*/"gdtrbase", "gdtr-lim", "idtrbase", "idtr-lim", "ldtrbase", "ldtr-lim",
            /*30*/"es-base", "cs-base", "ss-base", "ds-base", "fs-base", "gs-base",
            /*36*/"cr0",
            /*37*/"ST0H", "ST0L","ST1H", "ST1L","ST2H", "ST2L","ST3H", "ST3L",
            /*45*/"ST4H", "ST4L","ST5H", "ST5L","ST6H", "ST6L","ST7H", "ST7L",
            //"expiry"
        };

    public String disam(byte[] code, Integer ops, Integer mode);
    // return disam of next instruction
    public String executeInstruction() throws IOException;
    public int[] getState() throws IOException;
    public byte[] getCMOS() throws IOException;
    public int[] getPit() throws IOException;
    public int getPITIntTargetEIP() throws IOException;
    public Integer savePage(Integer page, byte[] data, Boolean linear) throws IOException;

    public void setPhysicalMemory(int addr, byte[] data) throws IOException;
    public void setState(int[] state, int currentCSEIP) throws IOException;
    public void keysDown(String keys);
    public void keysUp(String keys);
    public void sendMouse(Integer dx, Integer dy, Integer dz, Integer buttons);

    public void destroy();
}
