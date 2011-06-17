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

package org.jpc.emulator;

/**
 * An object which can form part of a PC.
 * <p>
 * Usually but not always, objects that implement <code>HardwareComponent</code> have
 * a physical counterpart in a real computer.
 * @author Chris Dennis
 */
public interface HardwareComponent extends Hibernatable
{
    /**
     * Returns true when a object need be offered no more 
     * <code>HardwareComponent</code> instances through the
     * <code>acceptComponent</code> method.
     * @return true when this component is fully initialised.
     */
    public boolean initialised();

    /**
     * Offers a <code>Hardware Component</code> as possible configuration
     * information for this object.
     * <p>
     * Implementations of this method may or may not maintain a reference to
     * <code>component</code> depending on its type and value.
     * @param component <code>HardwareComponent</code> being offered.
     */
    public void acceptComponent(HardwareComponent component);

    /**
     * Resets this component to its default initial state
     * <p>
     * Implementations of this method should not erase any configuration
     * information.
     */
    public void reset();

    /**
     * Returns true when all references have been received following a call of 
     * <code>loadState</code>.
     * @return true on a set of complete external references
     */
    public boolean updated();

    /**
     * Offers this object a component as a possible required external reference
     * following a call of <code>loadState</code>.
     * @param component
     */
    public void updateComponent(HardwareComponent component);
}
