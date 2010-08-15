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

import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpc.emulator.memory.AddressSpace;

/**
 *
 * @author Ian Preston
 */
public abstract class ProtectedModeExpandDownSegment extends Segment
{
private static final Logger LOGGING = Logger.getLogger(ProtectedModeSegment.class.getName());

    public static final int TYPE_ACCESSED = 0x1;
    public static final int TYPE_CODE = 0x8;
    public static final int TYPE_DATA_WRITABLE = 0x2;
    public static final int TYPE_DATA_EXPAND_DOWN = 0x4;
    public static final int TYPE_CODE_READABLE = 0x2;
    public static final int TYPE_CODE_CONFORMING = 0x4;

    public static final int DESCRIPTOR_TYPE_CODE_DATA = 0x10;

    private final boolean defaultSize;
    private final boolean granularity;
    private final boolean present, system;
    private final int selector;
    private final int base;
    private final int minOffset, maxOffset;
    private final int dpl;
    private final long limit;
    private final int rawLimit;
    private final long descriptor;
    private int rpl;

    public ProtectedModeExpandDownSegment(AddressSpace memory, int selector, long descriptor)
    {
        super(memory);
        this.selector = selector;
        this.descriptor = descriptor;

        granularity = (descriptor & 0x80000000000000L) != 0;
        defaultSize = (descriptor & (1L << 54)) != 0;
        base = (int) ((0xffffffL & (descriptor >> 16)) | ((descriptor >> 32) & 0xffffffffff000000L));
        if (granularity)
            limit = ((descriptor << 12) & 0xffff000L) | ((descriptor >>> 20) & 0xf0000000L) | 0xfffL;
        else
            limit = (descriptor & 0xffffL) | ((descriptor >>> 32) & 0xf0000L);

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

        rpl = selector & 0x3;
        dpl = (int) ((descriptor >> 45) & 0x3);

        present = (descriptor & (1L << 47)) != 0;
        system = (descriptor & (1L << 44)) != 0;
    }

    public void saveState(DataOutput output) throws IOException
    {
        output.writeInt(3);
        output.writeInt(selector);
        output.writeLong(descriptor);
        output.writeInt(rpl);
    }

    public boolean isPresent()
    {
        return present;
    }

    public boolean isSystem()
    {
        return !system;
    }

    public int translateAddressRead(int offset)
    {
        checkAddress(offset);
        return base + offset;
    }

    public int translateAddressWrite(int offset)
    {
        checkAddress(offset);
        return base + offset;
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

    public boolean getDefaultSizeFlag()
    {
        return defaultSize;
    }

    public int getLimit()
    {
        return (int) limit;
    }

    public int getRawLimit()
    {
        return rawLimit;
    }

    public int getBase()
    {
        return base;
    }

    public int getSelector()
    {
        return (selector & 0xFFFC) | rpl;
    }

    public int getRPL()
    {
        return rpl;
    }

    public int getDPL()
    {
        return dpl;
    }

    public void setRPL(int cpl)
    {
        rpl = cpl;
    }

    public boolean setSelector(int selector)
    {
        throw new IllegalStateException("Cannot set a selector for a descriptor table segment");
    }

    public void printState()
    {
        System.out.println("PM Expand down segment.");
        System.out.println("selector: " + Integer.toHexString(selector));
        System.out.println("base: " + Integer.toHexString(base));
        System.out.println("dpl: " + Integer.toHexString(dpl));
        System.out.println("rpl: " + Integer.toHexString(rpl));
        System.out.println("limit: " + Long.toHexString(limit));
        System.out.println("descriptor: " + Long.toHexString(descriptor));
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
