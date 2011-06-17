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


package org.jpc.debugger;

import java.lang.reflect.*;
import java.util.*;

import org.jpc.emulator.processor.*;

public class ProcessorAccess
{
    private Processor processor;
    private Map<String, Field> lookup;

    public ProcessorAccess(Processor proc)
    {
        processor = proc;

        try
        {
            lookup = new HashMap<String, Field>();

            addField("eax");
            addField("ecx");
            addField("edx");
            addField("ebx");
            addField("esp");
            addField("ebp");
            addField("esi");
            addField("edi");

            addField("eip");

            addField("cr0");
            addField("cr1");
            addField("cr2");
            addField("cr3");
            addField("cr4");
            
            addField("cs");
            addField("ds");
            addField("ss");
            addField("es");
            addField("fs");
            addField("gs");

            addField("idtr");
            addField("gdtr");
            addField("ldtr");
        }
        catch (Throwable t) {t.printStackTrace();}
    }

    private void addField(String fieldName) throws Exception
    {
        Field targetField = Processor.class.getDeclaredField(fieldName);
        targetField.setAccessible(true);

        lookup.put(fieldName, targetField);   
    }

    public int getValue(String name, int defaultValue)
    {
        if (name.equals("eflags"))
            return processor.getEFlags();

        Field f = lookup.get(name);
        if (f == null)
        {
            return defaultValue;
        }
        if (f.getType().isPrimitive())
            try {
                return f.getInt(processor);
            } catch (IllegalAccessException e) {
                return defaultValue;
            }

        try {
            Segment sel = (Segment) f.get(processor);
            return sel.getBase();
        } catch (IllegalAccessException e) {
            return defaultValue;            
        } catch (IllegalStateException g) {
            return defaultValue;
        }
    }

    public boolean setValue(String name, int value)
    {
        Field f = lookup.get(name);
        if (f == null)
            return false;

        try {
            f.setInt(processor, value);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }
}
