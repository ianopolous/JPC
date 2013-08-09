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

package org.jpc.emulator.execution;

public class UCodes
{
    //Lazy flag operations
    public static final int SHR8 = 125;
    public static final int SHR16 = 126;
    public static final int SHR32 = 127;
    public static final int SHRD16 = 128;
    public static final int SHRD32 = 129;
    public static final int ADD8 = 130;
    public static final int ADD16 = 131;
    public static final int ADD32 = 132;
    public static final int ADC8 = 133;
    public static final int ADC16 = 134;
    public static final int ADC32 = 135;
    public static final int SUB8 = 136;
    public static final int SUB16 = 137;
    public static final int SUB32 = 138;
    public static final int SBB8 = 139;
    public static final int SBB16 = 140;
    public static final int SBB32 = 141;
    public static final int INC = 142;
    public static final int DEC = 143;
    public static final int NEG8 = 144;
    public static final int NEG16 = 145;
    public static final int NEG32 = 146;
    public static final int SAR8 = 147;
    public static final int SAR16 = 148;
    public static final int SAR32 = 149;
    public static final int SHL8 = 150;
    public static final int SHL16 = 151;
    public static final int SHL32 = 152;
    public static final int SHLD16 = 153;
    public static final int SHLD32 = 154;
    public static final int IMUL8 = 155;
    public static final int IMUL16 = 156;
    public static final int IMUL32 = 157;
}
