/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.4

    A project from the Physics Dept, The University of Oxford

    Copyright (C) 2007-2010 The University of Oxford

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

    Conceived and Developed by:
    Rhys Newman, Ian Preston, Chris Dennis

    End of licence header
*/

package org.jpc.emulator.pci;

/**
 * A object that allows a PCI device to raise an interrupt on the processor.
 * <p>
 * Instances of this class are handed out to PCI devices by the PCI-ISA bridge
 * so that interrupt request can be directed straight to the ISA bridge, and
 * therefore removing the indirection of access through the PCI bus itself.
 * @author Chris Dennis
 */
public interface IRQBouncer
{
    /**
     * Raise or lower the given interrupt on the processor.
     * @param device source of the request
     * @param irqNumber interrupt number to adjust
     * @param level 1 to raise, 0 to lower.
     */
    public void setIRQ(PCIDevice device, int irqNumber, int level);
}
