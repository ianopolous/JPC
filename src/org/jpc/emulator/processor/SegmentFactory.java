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

import org.jpc.emulator.memory.*;

import java.io.*;

/**
 * 
 * @author Chris Dennis
 */
public class SegmentFactory
{
    private static final long DESCRIPTOR_TYPE = 0x100000000000L;
    private static final long SEGMENT_TYPE = 0xf0000000000L;
    
    public static final Segment NULL_SEGMENT = new NullSegment();
    
    private SegmentFactory()
    {
    }

    public static Segment createRealModeSegment(AddressSpace memory, int selector)
    {
        if (memory == null)
            throw new NullPointerException("Null reference to memory");

        return new RealModeSegment(memory, selector);
    }

    public static Segment createRealModeSegment(AddressSpace memory, Segment ancestor)
    {
        if (memory == null)
            throw new NullPointerException("Null reference to memory");

        return new RealModeSegment(memory, ancestor);
    }

    public static Segment createVirtual8086ModeSegment(AddressSpace memory, int selector, boolean isCode)
    {
        if (memory == null)
            throw new NullPointerException("Null reference to memory");

        return new Virtual8086ModeSegment(memory, selector, isCode);
    }
    
    public static Segment createDescriptorTableSegment(AddressSpace memory, int base, int limit)
    {
        if (memory == null)
            throw new NullPointerException("Null reference to memory");

        return new DescriptorTableSegment(memory, base, limit);
    }

    public static Segment createProtectedModeSegment(AddressSpace memory, int selector, long descriptor)
    {
        switch ((int) ((descriptor & (DESCRIPTOR_TYPE | SEGMENT_TYPE)) >>> 40)) {

            // System Segments 
            default:
            case 0x00: //Reserved
            case 0x08: //Reserved
            case 0x0a: //Reserved
            case 0x0d: //Reserved
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
            case 0x01: //System Segment: 16-bit TSS (Available)
                return new ProtectedModeSegment.Available16BitTSS(memory, selector, descriptor);
            case 0x02: //System Segment: LDT
                return new ProtectedModeSegment.LDT(memory, selector, descriptor);
            case 0x03: //System Segment: 16-bit TSS (Busy)
                return new ProtectedModeSegment.Busy16BitTSS(memory, selector, descriptor);
            case 0x04: //System Segment: 16-bit Call Gate
                return new ProtectedModeSegment.CallGate16Bit(memory, selector, descriptor);
            case 0x05: //System Segment: Task Gate
                return new ProtectedModeSegment.TaskGate(memory, selector, descriptor);
            case 0x06: //System Segment: 16-bit Interrupt Gate
                return new ProtectedModeSegment.InterruptGate16Bit(memory, selector, descriptor);
            case 0x07: //System Segment: 16-bit Trap Gate
                return new ProtectedModeSegment.TrapGate16Bit(memory, selector, descriptor);
            case 0x09: //System Segment: 32-bit TSS (Available)
                return new ProtectedModeSegment.Available32BitTSS(memory, selector, descriptor);
            case 0x0b: //System Segment: 32-bit TSS (Busy)
                return new ProtectedModeSegment.Busy32BitTSS(memory, selector, descriptor);
            case 0x0c: //System Segment: 32-bit Call Gate
                return new ProtectedModeSegment.CallGate32Bit(memory, selector, descriptor);
            case 0x0e: //System Segment: 32-bit Interrupt Gate
                return new ProtectedModeSegment.InterruptGate32Bit(memory, selector, descriptor);
            case 0x0f: //System Segment: 32-bit Trap Gate
                return new ProtectedModeSegment.TrapGate32Bit(memory, selector, descriptor);

            // Data Segments
            case 0x10: //Data Segment: Read-Only
                return new ProtectedModeSegment.ReadOnlyDataSegment(memory, selector, descriptor);
            case 0x11: //Data Segment: Read-Only, Accessed
                return new ProtectedModeSegment.ReadOnlyAccessedDataSegment(memory, selector, descriptor);
            case 0x12: //Data Segment: Read/Write
                return new ProtectedModeSegment.ReadWriteDataSegment(memory, selector, descriptor);
            case 0x13: //Data Segment: Read/Write, Accessed
                return new ProtectedModeSegment.ReadWriteAccessedDataSegment(memory, selector, descriptor);
            case 0x14: //Data Segment: Read-Only, Expand-Down
                throw new IllegalStateException("Unimplemented Data Segment: Read-Only, Expand-Down");
            case 0x15: //Data Segment: Read-Only, Expand-Down, Accessed
                throw new IllegalStateException("Unimplemented Data Segment: Read-Only, Expand-Down, Accessed");
            case 0x16: //Data Segment: Read/Write, Expand-Down
                return new ProtectedModeExpandDownSegment.ReadWriteDataSegment(memory, selector, descriptor);
            case 0x17: //Data Segment: Read/Write, Expand-Down, Accessed
                throw new IllegalStateException("Unimplemented Data Segment: Read/Write, Expand-Down, Accessed");

            // Code Segments
            case 0x18: //Code, Execute-Only
                return new ProtectedModeSegment.ExecuteOnlyCodeSegment(memory, selector, descriptor);
            case 0x19: //Code, Execute-Only, Accessed
                throw new IllegalStateException("Unimplemented Code Segment: Execute-Only, Accessed");
            case 0x1a: //Code, Execute/Read
                return new ProtectedModeSegment.ExecuteReadCodeSegment(memory, selector, descriptor);
            case 0x1b: //Code, Execute/Read, Accessed
                return new ProtectedModeSegment.ExecuteReadAccessedCodeSegment(memory, selector, descriptor);
            case 0x1c: //Code: Execute-Only, Conforming
                throw new IllegalStateException("Unimplemented Code Segment: Execute-Only, Conforming");
            case 0x1d: //Code: Execute-Only, Conforming, Accessed
                return new ProtectedModeSegment.ExecuteOnlyConformingAccessedCodeSegment(memory, selector, descriptor);
            case 0x1e: //Code: Execute/Read, Conforming
                return new ProtectedModeSegment.ExecuteReadConformingCodeSegment(memory, selector, descriptor);
            case 0x1f: //Code: Execute/Read, Conforming, Accessed
                return new ProtectedModeSegment.ExecuteReadConformingAccessedCodeSegment(memory, selector, descriptor);
        }
    }

    static final class NullSegment extends Segment
    {
        public NullSegment()
        {
            super(null);
        }

        public void printState()
        {
            System.out.println("Null Segment");
        }

        public void saveState(DataOutput output) throws IOException
        {
            output.writeInt(4);
        }

        public int getType()
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        }

        public int getSelector()
        {
            return 0;
        }

        public void checkAddress(int offset)
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        }

        public int translateAddressRead(int offset)
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        }

        public int translateAddressWrite(int offset)
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        }

        public void invalidateAddress(int offset)
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        }

        public int getBase()
        {
            throw new IllegalStateException(getClass().toString());
        }

        public int getLimit()
        {
            throw new IllegalStateException(getClass().toString());
        }
        
        public int getRawLimit()
        {
            throw new IllegalStateException(getClass().toString());
        }

        public boolean setSelector(int selector)
        {
            throw new IllegalStateException(getClass().toString());
        }

        public int getDPL()
        {
            throw new IllegalStateException(getClass().toString());
        }

        public int getRPL()
        {
            throw new IllegalStateException(getClass().toString());
        }

        public void setRPL(int cpl)
        {
            throw new IllegalStateException(getClass().toString());
        }

        public boolean getDefaultSizeFlag()
        {
            throw new IllegalStateException(getClass().toString());
        }

        public boolean isPresent()
        {
            return true;
        }

        public boolean isSystem()
        {
            return false;
        }
    }
}
