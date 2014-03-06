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

package org.jpc.debugger;

import org.jpc.emulator.processor.Processor;
import org.jpc.emulator.processor.Segment;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ReflectionProcessorAccess extends ProcessorAccess
{
    private Processor processor;
    private Map<String, Field> lookup;

    public ReflectionProcessorAccess(Processor cpu)
    {
        super();
        this.processor = cpu;
        try
        {
            lookup = new HashMap<String, Field>();

            addField("r_eax");
            addField("r_ebx");
            addField("r_ecx");
            addField("r_edx");
            addField("r_esp");
            addField("r_ebp");
            addField("r_esi");
            addField("r_edi");

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
            Processor.Reg r = (Processor.Reg) f.get(processor);
            return r.get32();
        } catch (Exception e) {}
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
            Processor.Reg r = (Processor.Reg) f.get(processor);
            r.set32(value);
        } catch (Exception e) {e.printStackTrace();}
        try {
            f.setInt(processor, value);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    public void rowChanged(int row) {}
}
