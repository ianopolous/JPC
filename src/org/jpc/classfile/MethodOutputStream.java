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

package org.jpc.classfile;
    
import java.io.*;

/**
 * An <code>OutputStream</code> implementation that limits the total number of
 * bytes written to 64K, the size limit of a method.
 * @author Chris Dennis
 */
public class MethodOutputStream extends OutputStream
{
    private final OutputStream backing;
    private int count;
    
    /**
     * Constructs a instance wrapping an already existing <code>OutputStream</code>
     * instance.
     * @param out <code>OutputStream</code> to be wrapped
     */
    public MethodOutputStream(OutputStream out)
    {
	backing = out;
	count = 0;
    }
    
    /**
     * Returns the number of bytes written so far
     * @return bytes written
     */
    public int position()
    {
	return count;
    }
    
    public void close() throws IOException { backing.close(); }
    public void flush() throws IOException { backing.flush(); }

    /**
     * @throws IllegalStateException if stream limit is exceeded
     */
    public void write(byte[] b) throws IOException
    {
	backing.write(b);
        count += b.length;
	if (count >= ClassFile.MAX_METHOD_CODE_SIZE)
	    throw new IllegalStateException("Oversize Method");
    }
 
    /**
     * @throws IllegalStateException if stream limit is exceeded
     */
    public void write(byte[] b, int off, int len) throws IOException
    {
	backing.write(b, off, len);
        count += len;
	if (count >= ClassFile.MAX_METHOD_CODE_SIZE)
	    throw new IllegalStateException("Oversize Method");	    
    }

    /**
     * @throws IllegalStateException if stream limit is exceeded
     */
    public void write(int b) throws IOException
    {
	backing.write(b);
        count++;
	if (count >= ClassFile.MAX_METHOD_CODE_SIZE)
	    throw new IllegalStateException("Oversize Method");	
    }
}
