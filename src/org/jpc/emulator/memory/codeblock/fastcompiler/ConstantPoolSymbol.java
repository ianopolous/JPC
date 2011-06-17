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

package org.jpc.emulator.memory.codeblock.fastcompiler;

import java.lang.reflect.*;

/**
 * Immutable wrapper for objects which can be referenced in the constant pool.
 * @author Chris Dennis
 */
class ConstantPoolSymbol
{
    private final Object poolEntity;

    /**
     * Constructs a new <code>ConstantPoolSymbol</code> wrapping
     * </code>o</code>.  This will throw <code>IllegalArgumentException</code>
     * if the supplied object cannot be referenced in a constant pool.  Valid
     * classes are:
     * <ul>
     * <li><code>Class</code></li>
     * <li><code>Method</code></li>
     * <li><code>Field</code></li>
     * <li><code>String</code></li>
     * <li><code>Integer</code></li>
     * <li><code>Long</code></li>
     * <li><code>Float</code></li>
     * <li><code>Double</code></li>
     * </ul>
     * @param o object being wrapped.  
     * @throws IllegalArgumentException if o is not a valid constant pool object
     */
    ConstantPoolSymbol(Object o)
    {
        Class cls = o.getClass();
        
        boolean ok = Class.class.equals(cls)
	    || Method.class.equals(cls)
	    || Field.class.equals(cls)
	    || String.class.equals(cls)
	    || Integer.class.equals(cls)
	    || Long.class.equals(cls)
            || Float.class.equals(cls)
	    || Double.class.equals(cls);

        if (!ok) throw new IllegalArgumentException();

        poolEntity = o;
    }

    /**
     * Returns the object being wrapped by this symbol.
     * @return object being wrapped
     */
    Object poolEntity()
    {
        return poolEntity;
    }

    public String toString()
    {
        return "ConstantPoolSymbol["+poolEntity+"]";
    }
}
