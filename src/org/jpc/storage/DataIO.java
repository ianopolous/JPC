/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.4.1

    A project from the eMediaTrack ltd.

    Copyright (C) 2007-2010 The eMediaTrack ltd.

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

    http://jpc.sourceforge.net/

    End of licence header
*/

/**
 *
 * @author Rhys Newman & Pin Hu
 */

package org.jpc.storage;

import java.io.IOException;

public interface DataIO{

    public int read(long address, byte[] content, int offset, int length) throws IOException;

    public int readFully(long address, byte[] content, int offset, int length) throws IOException;

    public int write(long address, byte[] content, int offset, int length) throws IOException;

    public void writeFully(long address, byte[] content, int offset, int length) throws IOException;

    public void close() throws IOException;

    public long getLength() throws IOException;

    public void setLength(long length) throws IOException;
}
