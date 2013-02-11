

package org.jpc.debugger;

import java.lang.reflect.*;
import java.util.*;

import org.jpc.emulator.processor.*;
import org.jpc.emulator.processor.fpu64.FpuState64;

public class FPUAccess
{
    private FpuState64 fpu;
    private Map<String, Field> lookup;

    public FPUAccess(FpuState64 proc)
    {
        fpu = proc;

        try
        {
            lookup = new HashMap<String, Field>();

            addField("statusWord");
        }
        catch (Throwable t) {t.printStackTrace();}
    }

    private void addField(String fieldName) throws Exception
    {
        Field targetField = FpuState64.class.getDeclaredField(fieldName);
        targetField.setAccessible(true);

        lookup.put(fieldName, targetField);   
    }

    public long getLongValue(String name, int defaultValue)
    {
        if (name.startsWith("ST"))
            return Double.doubleToRawLongBits(fpu.ST(Integer.parseInt(name.substring(2))));

        Field f = lookup.get(name);
        if (f == null)
        {
            return defaultValue;
        }
        if (f.getType().isPrimitive())
            try {
                return f.getInt(fpu);
            } catch (IllegalAccessException e) {
                return defaultValue;
            }
        return defaultValue;
    }

    public boolean setLongValue(String name, long value)
    {
        if (name.startsWith("ST"))
        {
            fpu.setST(Integer.parseInt(name.substring(2)), Double.longBitsToDouble(value));
            return true;
        }

        Field f = lookup.get(name);
        if (f == null)
            return false;

        try {
            f.setInt(fpu, (int)value);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }
}
