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

package org.jpc.rcs;

import java.util.*;
import java.io.*;
import java.net.*;

import org.jpc.classfile.*;
import org.jpc.emulator.memory.*;
import org.jpc.emulator.memory.codeblock.*;
import org.jpc.emulator.memory.codeblock.optimised.*;
import org.jpc.emulator.memory.codeblock.fastcompiler.*;

import org.nereus.net.*;
import org.nereus.net.http.*;
import org.nereus.util.*;

public class RemoteCompilerServer extends NIONetworkHandler implements HTTPMessageFilter
{
    private Object PENDING;
    private byte[] WRONG_BYTES;
    private byte[] OK_BYTES;
    private byte[] NO_BYTES;

    private long nameCounter;
    private HashMap compiledClassMap;
    private HashMap compiledClassIndex;
    private FASTCompiler compiler;

    public RemoteCompilerServer() throws Exception
    {
        super(new BufferPool(), "RemoteCompilerServer");
 
        WRONG_BYTES = new byte[20];
        OK_BYTES = new byte[1];
        NO_BYTES = new byte[0];
        PENDING = new Object();
        nameCounter = 0;
        
        compiler = new FASTCompiler();
        compiledClassMap = new HashMap();
        compiledClassIndex = new HashMap();
    }

    private String getClassName(HTTPHeaders headers)
    {
        String reqClass = HTTPTools.getRequestFile(headers);
        int index = reqClass.indexOf(RemoteCompiler.JPC_DYNAMIC_URI_STRING);
        if (index < 0)
            return null;
        reqClass = reqClass.substring(index+RemoteCompiler.JPC_DYNAMIC_URI_STRING.length());
        reqClass = reqClass.replace("/", ".");
        if (reqClass.endsWith(".class"))
            reqClass = reqClass.substring(0, reqClass.length() - 6);

        return reqClass;
    }

    public HTTPMessageHandler findHandler(HTTPHeaders headers, boolean isHTTP11, boolean isSSL, InetAddress address) throws IOException
    {
        if (HTTPTools.isHTTPGet(headers))
        {
            String reqClass = getClassName(headers);
            if (reqClass != null)
            {
                synchronized (compiledClassMap)
                {
                    CompiledClass cls = (CompiledClass) compiledClassIndex.get(reqClass);
                    if ((cls != null) && (cls.compileError == null))
                        return MemoryContentHandler.createHandlerForContent(isHTTP11, cls.compiledBytes);
                }
            }
            return MemoryContentHandler.createHandlerForContent(isHTTP11, WRONG_BYTES);
        }
        else if (HTTPTools.isHTTPPost(headers))
            return new CodeBlockCompilerProtocol(headers, isHTTP11);
        else
            throw new IOException("Unsupported HTTP Method");
    }

    class CompiledClass 
    {
        int hash;
        boolean isRealMode;
        Throwable compileError;

        byte[] classNameBytes;
        byte[] compiledBytes;
        byte[] rawBytes;

        CompiledClass(String name, byte[] compiledBytes, byte[] rawBytes)
        {
            isRealMode = name.startsWith("org.jpc.dynamic.R");
            classNameBytes = name.getBytes();
            this.rawBytes = rawBytes;
            this.compiledBytes = compiledBytes;

            hash = -1;
            
            for (int i=0; i<rawBytes.length; i++)
            {
                hash ^= rawBytes[i];
                hash *= 31;
            } 
        }

        boolean isRealModeBlock()
        {
            return isRealMode;
        }

        public boolean equals(Object another)
        {
            if (another == this)
                return true;
            if (!(another instanceof CompiledClass))
                return false;
            CompiledClass cls = (CompiledClass) another;
            if ((rawBytes.length != cls.rawBytes.length) || (isRealMode != cls.isRealMode))
                return false;
            for (int i=0; i<rawBytes.length; i++)
                if (rawBytes[i] != cls.rawBytes[i])
                    return false;

            return true;
        }

        public int hashCode()
        {
            return hash;
        }
    }

    class CodeBlockCompilerProtocol extends HTTPPostMessageHandler
    {
        private String className;
        private byte[] rawBytes;

        CodeBlockCompilerProtocol(HTTPHeaders headers, boolean isHTTP11)
        {
            super((int) HTTPTools.getContentLength(headers), MemoryContentHandler.createHandlerForContent(isHTTP11, NO_BYTES));
            className = getClassName(headers);
            className += ""+(nameCounter++);
            rawBytes = new byte[getContentLength()];
        }

        protected int readPostData(Connection conn, int currentPosition, int contentLength) throws IOException
        {
            int remaining = contentLength - currentPosition;
            if (remaining <= 0)
                return 0;
            
            BufferAdapter buffer = conn.getInputBuffer();
            int toRead = Math.min(buffer.availableToRead(), remaining);
            if (toRead < 0)
                throw new IOException("Failed to read class bytes from JPC client");
            buffer.read(rawBytes, currentPosition, toRead);

            if (remaining - toRead > 0)
                return toRead;

            CompiledClass result = new CompiledClass(className, null, rawBytes);
            try
            {
                synchronized (compiledClassMap)
                {
                    CompiledClass existing = (CompiledClass) compiledClassMap.get(result);
                    if (existing != null) 
                    {
                        if (existing.compileError != null)
                            return toRead;
                        setResultHandler(MemoryContentHandler.createHandlerForContent(isHTTP11, existing.classNameBytes));
                        return toRead;
                    }
                }
                
                CompileJob job = new CompileJob(className, rawBytes);
                ClassFile classFile = null;
                if (job.isRealMode())
                    classFile = compiler.compileClassForRealModeBlock(job.getClassName(), job);
                else
                    classFile = compiler.compileClassForProtectedModeBlock(job.getClassName(), job);
                
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(bout);
                classFile.write(dout);
                
                result.compiledBytes = bout.toByteArray();
                result.classNameBytes = className.getBytes();
                synchronized (compiledClassMap)
                {
                    compiledClassIndex.put(job.getClassName(), result);
                    compiledClassMap.put(result, result);
                    setResultHandler(MemoryContentHandler.createHandlerForContent(isHTTP11, result.classNameBytes));
                }
            }
            catch (Throwable t)
            {
                result.compileError = t;

                synchronized (compiledClassMap)
                {
                    compiledClassIndex.put(className, result);
                    compiledClassMap.put(result, result);
                }
            }
            return toRead;
        }
    }

    protected ProtocolHandler createServerProtocolHandler(InetAddress clientAddress, int serverPort)
    {
        return new HTTPServerProtocol(this);
    }
    
    class CompileJob implements InstructionSource
    {
        private String className;
        private int x86Pos, uCodePos;
        private ArrayList uCodes, x86Lengths;

        CompileJob(String className, byte[] rawBytes)
        {
            this.className = className;
            x86Pos = -1;
            uCodePos = -1;

            try
            {
                int pos = 0;
                DataInputStream din = new DataInputStream(new ByteArrayInputStream(rawBytes));
                uCodes = new ArrayList();
                x86Lengths = new ArrayList();
                
                while (true)
                {
                    int len = din.readInt();
                    int[] buf = new int[len];
                    uCodes.add(buf);
                    x86Lengths.add(Integer.valueOf(din.readInt()));
                    
                    for (int i=0; i<len; i++)
                        buf[i] = din.readInt();

                    pos += 4*(2 + len);
                    if (pos >= rawBytes.length)
                        break;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            rewind();
        }

        boolean isRealMode()
        {
            return className.startsWith("org.jpc.dynamic.R");
        }

        public String getClassName()
        {
            return className;
        }

        public void rewind() 
        {
            x86Pos = -1;
            uCodePos = -1;
        }

        public boolean getNext() 
        {
            if (x86Pos >= uCodes.size()-1)
                return false;
            x86Pos++;
            uCodePos = 0;
            return true;
        }

        public int getMicrocode() 
        {
            return ((int[]) uCodes.get(x86Pos))[uCodePos++];
        }

        public int getLength() 
        {
            return ((int[]) uCodes.get(x86Pos)).length;
        }

        public int getX86Length()
        {
            return ((Integer) x86Lengths.get(x86Pos)).intValue();
        }

        public String toString()
        {
            return className;
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        RemoteCompilerServer server = new RemoteCompilerServer();
        server.listenOn(ArgProcessor.extractIntArg(args, "port", 86));
    }
}
