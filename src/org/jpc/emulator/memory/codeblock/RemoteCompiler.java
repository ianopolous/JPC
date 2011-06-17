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

package org.jpc.emulator.memory.codeblock;

import java.util.*;
import java.net.*;
import java.io.*;
import java.util.logging.*;

import org.jpc.emulator.memory.codeblock.optimised.*;

/**
 * 
 * @author Rhys Newman
 */
public class RemoteCompiler implements CodeBlockCompiler
{
    public static final String JPC_DYNAMIC_URI_STRING = "/JPCDynamicClasses/";
    public static final URI JPC_DYNAMIC_URI = URI.create(JPC_DYNAMIC_URI_STRING);

    private static final Logger LOGGING = Logger.getLogger(RemoteCompiler.class.getName());

    private volatile boolean running;
    private URI serverURI;
    private CodeBlockCompiler immediate;

    private RemoteLoader remoteLoader;
    private URLClassLoader urlClassLoader;
    private LinkedList remoteClassQueue;
    private HashMap realClassCache, realBlockCache, protectedClassCache, protectedBlockCache;

    public RemoteCompiler(CodeBlockCompiler immediate, String remoteServerURI)
    {
        this.immediate = immediate;
        remoteClassQueue = new LinkedList();
        realClassCache = new HashMap();
        realBlockCache = new HashMap();
        protectedClassCache = new HashMap();
        protectedBlockCache = new HashMap();

        try
        {
            serverURI = new URI(remoteServerURI).resolve(JPC_DYNAMIC_URI);
            urlClassLoader = URLClassLoader.newInstance(new URL[]{serverURI.toURL()});

            running = true;
            remoteLoader = new RemoteLoader();
            Thread t = new Thread(remoteLoader, "Remote Compiler Loader Task");
            try 
            {
                t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.currentThread().getPriority()-1));
            } 
            catch (SecurityException e) {}
            t.start();
        }
        catch (Exception e) 
        {
            LOGGING.log(Level.WARNING, "Error connecting to remote compiler server", e);
        }
    }  

    private class RemoteLoader implements Runnable
    {
        public void run()
        {
            while (true) 
            {
                AbstractCodeBlockWrapper target = null;
                synchronized (remoteClassQueue)
                {
                    while (remoteClassQueue.size() == 0)
                    {
                        if (!running)
                            return;
                        try
                        {
                            remoteClassQueue.wait(200);
                        }
                        catch (Exception e) {}
                    }

                    target = (AbstractCodeBlockWrapper) remoteClassQueue.removeFirst();
                }

                CodeBlock src = target.getTargetBlock();
                if (src instanceof ReplacementBlockTrigger)
                    continue;

                CodeBlock result = null;
                InstructionSource blockCodes = null;
                String classStem = null;
                boolean realMode = false;
                if (src instanceof RealModeUBlock)
                {
                    blockCodes = ((RealModeUBlock) src).getAsInstructionSource();
                    classStem = "org.jpc.dynamic.R";
                    realMode = true;
                }
                else if (src instanceof ProtectedModeUBlock)
                {
                    blockCodes = ((ProtectedModeUBlock) src).getAsInstructionSource();
                    classStem = "org.jpc.dynamic.P";
                }
                
                try
                {
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    DataOutputStream dout = new DataOutputStream(bout);
                    
                    while (blockCodes.getNext())
                    {
                        int length = blockCodes.getLength();
                        dout.writeInt(length);
                        dout.writeInt(blockCodes.getX86Length());
                        
                        for (int i=0; i<length; i++) 
                        {
                            int uCode = blockCodes.getMicrocode();
                            dout.writeInt(uCode);
                        }
                    }
                    byte[] rawCodeBlock = bout.toByteArray();
                    
                    int hash = -1;
                    for (int i=0; i<rawCodeBlock.length; i++)
                    {
                        hash ^= rawCodeBlock[i];
                        hash *= 31;
                    }
                    
                    Integer key = Integer.valueOf(hash);
                    byte[] existing = null;
                    if (realMode)
                        existing = (byte[]) realBlockCache.get(key);
                    else
                        existing = (byte[]) protectedBlockCache.get(key);

                    if (Arrays.equals(rawCodeBlock, existing))
                    {
                        if (realMode)
                            result = (CodeBlock) (((Class) realClassCache.get(key)).newInstance());
                        else
                            result = (CodeBlock) (((Class) protectedClassCache.get(key)).newInstance());
                    }
                    else
                    {
                        URL url = serverURI.resolve(classStem).toURL();
                        URLConnection conn = url.openConnection();
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Length", String.valueOf(rawCodeBlock.length));
                        conn.setUseCaches(false);
                        conn.getOutputStream().write(rawCodeBlock);
                        
                        StringBuffer buf = new StringBuffer();
                        InputStream in = conn.getInputStream();
                        while (true)
                        {
                            int ch = in.read();
                            if (ch < 0)
                                break;
                            buf.append((char) ch);
                        }
                        in.close();
                        
                        String className = buf.toString();
                        if (className.length() == 0)
                            throw new IllegalStateException("No class");
                    
                        Class cls = urlClassLoader.loadClass(className);
                        result = (CodeBlock) cls.newInstance();

                        if (realMode)
                        {
                            realClassCache.put(key, cls);
                            realBlockCache.put(key, rawCodeBlock);
                        }
                        else
                        {
                            protectedClassCache.put(key, cls);
                            protectedBlockCache.put(key, rawCodeBlock);
                        }
                    }
                }
                catch (Throwable e)
                {
                    System.out.println(">>> "+e);
                }

                if (result != null)
                    target.setTargetBlock(new ReplacementBlockTrigger(result));
                else
                    target.setTargetBlock(new ReplacementBlockTrigger(src));
            }
        }
    }

    private class RealModeCodeBlockWrapper extends AbstractCodeBlockWrapper implements RealModeCodeBlock
    {
	RealModeCodeBlockWrapper(RealModeCodeBlock block)
	{
            super(block);
	}
    }

    private class ProtectedModeCodeBlockWrapper extends AbstractCodeBlockWrapper implements ProtectedModeCodeBlock
    {
	ProtectedModeCodeBlockWrapper(ProtectedModeCodeBlock block)
	{
	    super(block);
	}
    }

    public RealModeCodeBlock getRealModeCodeBlock(InstructionSource source)
    {
        RealModeCodeBlock result = new RealModeCodeBlockWrapper(immediate.getRealModeCodeBlock(source));
        synchronized (remoteClassQueue)
        {
            remoteClassQueue.add(result);
            remoteClassQueue.notify();
        }
	return result;
    }

    public ProtectedModeCodeBlock getProtectedModeCodeBlock(InstructionSource source)
    {
	ProtectedModeCodeBlock result = new ProtectedModeCodeBlockWrapper(immediate.getProtectedModeCodeBlock(source));
        synchronized (remoteClassQueue)
        {
            remoteClassQueue.add(result);
            remoteClassQueue.notify();
        }
	return result;
    }

    public Virtual8086ModeCodeBlock getVirtual8086ModeCodeBlock(InstructionSource source)
    {
// 	Virtual8086ModeCodeBlock imm = immediate.getVirtual8086ModeCodeBlock(source);
// 	return new Virtual8086ModeCodeBlockWrapper(imm);

	return immediate.getVirtual8086ModeCodeBlock(source);
    }

    protected void finalize()
    {
        running = false;
        remoteClassQueue.clear();
    }
}
