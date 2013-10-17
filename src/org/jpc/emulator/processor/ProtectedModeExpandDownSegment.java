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

package org.jpc.emulator.processor;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpc.emulator.memory.AddressSpace;

/**
 *
 * @author Ian Preston
 */
public abstract class ProtectedModeExpandDownSegment extends ProtectedModeSegment
{
private static final Logger LOGGING = Logger.getLogger(ProtectedModeSegment.class.getName());

    private final int minOffset, maxOffset;
    private final int rawLimit;

    public ProtectedModeExpandDownSegment(AddressSpace memory, int selector, long descriptor)
    {
        super(memory, selector, descriptor);
        rawLimit =  (int) ((descriptor & 0xffffL) | ((descriptor >>> 32) & 0xf0000L));
        if (defaultSize)
        {
            //base = (int) (tmpbase + tmplimit - 0x10000000L);
            //limit = 0xFFFFFFFF - tmplimit;
            minOffset = (int) (base + limit - 1);
            maxOffset = 0xFFFFFFFF;
        } else
        {
            //base = tmpbase + (int) tmplimit - 0x10000;
            //limit = 0xFFFF-tmplimit;
            minOffset = (int) (base + limit - 1);
            maxOffset = 0xFFFF;
        }
    }

    public void checkAddress(int offset)
    {
        if (((offset < 0) && (maxOffset < 0)) | ((offset > 0) && (maxOffset > 0)))
        {
            if (offset >= maxOffset)
            {
                LOGGING.log(Level.INFO, this + "expand down segment: offset not within bounds.");
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION,0,true);
            }
        } else if (offset > 0)
        {
            return;
        } else
        {
            LOGGING.log(Level.INFO, this + "expand down segment: offset not within bounds.");
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION,0,true);
        }
    }

    public int getRawLimit()
    {
        return rawLimit;
    }

    public boolean setSelector(int selector)
    {
        throw new IllegalStateException("Cannot set a selector for a Protected Mode segment");
    }

    static final class ReadWriteDataSegment extends ProtectedModeExpandDownSegment
    {
        public ReadWriteDataSegment(AddressSpace memory, int selector, long descriptor)
        {
            super(memory, selector, descriptor);
        }

        public int getType()
        {
            return DESCRIPTOR_TYPE_CODE_DATA | TYPE_DATA_WRITABLE;
        }
    }
}
