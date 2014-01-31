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

package tools;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Calendar;

public class JPCControl extends EmulatorControl
{
    private final Object pc;
    private final Method execute;
    private final Method setState;
    private final Method state;
    private final Method ints;
    private final Method instructionInfo;
    private final Method setPhysicalMemory;
    private final Method disam;
    private final Method reset;
    private final URLClassLoader cl1;

    public JPCControl(String jar, String pcName, String[] extraArgs) throws IOException
    {
        this(jar, concat(CompareToBochs.possibleArgs.get(pcName), extraArgs));
    }

    public JPCControl(String jar, String[] args) throws IOException
    {
        URL[] urls1 = new URL[]{new File(jar).toURL()};
        cl1 = new URLClassLoader(urls1, Comparison.class.getClassLoader());

        try {
            Class opts = cl1.loadClass("org.jpc.j2se.Option");
            Method parse = opts.getMethod("parse", String[].class);
            String[] pcargs = concat(new String[]{"-bochs"}, args);
            parse.invoke(opts, (Object)pcargs);

            Calendar start1 = Calendar.getInstance();
            start1.setTimeInMillis(1370072774000L); // hard coded into bochssrc

            Class c1 = cl1.loadClass("org.jpc.emulator.PC");
            Constructor ctor = c1.getConstructor(String[].class, Calendar.class);
            pc = ctor.newInstance((Object)pcargs, start1);

            ints = c1.getMethod("checkInterrupts", Integer.class, Boolean.class);
            state = c1.getMethod("getState");
            setState = c1.getMethod("setState", int[].class);
            execute = c1.getMethod("executeBlock");
            instructionInfo = c1.getMethod("getInstructionInfo", Integer.class);
            setPhysicalMemory = c1.getMethod("setPhysicalMemory", Integer.class, byte[].class);
            disam = c1.getMethod("disam", byte[].class, Integer.class, Integer.class);
            reset = c1.getMethod("reset");
            Method save = c1.getMethod("savePage", Integer.class, byte[].class, Boolean.class);
            Method load = c1.getMethod("loadPage", Integer.class, byte[].class, Boolean.class);
        } catch (ClassNotFoundException e) {throw new RuntimeException(e.getMessage());}
        catch (NoSuchMethodException e) {throw new RuntimeException(e.getMessage());}
        catch (IllegalAccessException e) {throw new RuntimeException(e.getMessage());}
        catch (InvocationTargetException e) {throw new RuntimeException(e.getMessage());}
        catch (InstantiationException e) {throw new RuntimeException(e.getMessage());}
    }

    public String disam(byte[] code, Integer ops, Integer mode)
    {
        try {
            return (String) disam.invoke(pc, code, ops, mode);
        } catch (InvocationTargetException e)
        {
            if (e.getCause().getMessage().contains("Invalid"))
                return "invalid";
            return "Error during disam: " + e.getMessage();
        }
        catch (IllegalAccessException e) {throw new RuntimeException(e.getMessage());}
    }

    public String executeInstruction() throws IOException
    {
        try {
            ints.invoke(pc, new Integer(1), new Boolean(false));
            int blockLength = (Integer)execute.invoke(pc);
            return (String) instructionInfo.invoke(pc, new Integer(1));
        } catch (InvocationTargetException e)
        {
            Throwable c = e.getCause();
            if (c instanceof IllegalStateException)
                throw (RuntimeException) c;
            throw new RuntimeException(e.getMessage());
        }
        catch (IllegalAccessException e) {throw new RuntimeException(e.getMessage());}
    }

    public int[] getState() throws IOException
    {
        try {
            return (int[])state.invoke(pc);
        } catch (InvocationTargetException e) {throw new RuntimeException(e.getMessage());}
        catch (IllegalAccessException e) {throw new RuntimeException(e.getMessage());}
    }

    public void setPhysicalMemory(int addr, byte[] data) throws IOException
    {
        try {
            setPhysicalMemory.invoke(pc, new Integer(addr), data);
        } catch (InvocationTargetException e) {throw new RuntimeException(e.getMessage());}
        catch (IllegalAccessException e) {throw new RuntimeException(e.getMessage());}
    }

    //public void setState(int[] state, int currentCSEIP) throws IOException
    //{
    //    try {
    //    setState.invoke(pc, (int[])state);
    //    } catch (InvocationTargetException e) {throw new RuntimeException(e.getMessage());}
    //    catch (IllegalAccessException e) {throw new RuntimeException(e.getMessage());}
    //}

    public void destroy()
    {
        try {
            reset.invoke(pc);
        } catch (InvocationTargetException e) {throw new RuntimeException(e.getMessage());}
        catch (IllegalAccessException e) {throw new RuntimeException(e.getMessage());}
    }

    public Integer savePage(Integer page, byte[] data, Boolean linear) throws IOException
    {
        throw new IllegalStateException("Unimplemented!");
    }

    public byte[] getCMOS() throws IOException
    {
        throw new IllegalStateException("Unimplemented!");
    }
    public int[] getPit() throws IOException
    {
        throw new IllegalStateException("Unimplemented!");
    }
    public int getPITIntTargetEIP() throws IOException
    {
        throw new IllegalStateException("Unimplemented!");
    }
    public void keysDown(String keys)
    {
        throw new IllegalStateException("Unimplemented!");
    }
    public void keysUp(String keys)
    {
        throw new IllegalStateException("Unimplemented!");
    }
    public void sendMouse(Integer dx, Integer dy, Integer dz, Integer buttons)
    {
        throw new IllegalStateException("Unimplemented!");
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
}
}
