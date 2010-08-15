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

package org.jpc.support;

import java.io.*;
import java.net.*;
import java.util.logging.*;

/**
 * 
 * @author Ian Preston
 */
public class RemoteBlockDeviceServer 
{
    private static final Logger LOGGING = Logger.getLogger(RemoteBlockDeviceServer.class.getName());
    
    public static void main(String[] args) throws Exception
    {
        DriveSet set = DriveSet.buildFromArgs(args);
        
        int port = 6666;
        try
        {
            port = Integer.parseInt(ArgProcessor.findVariable(args, "port", "6666"));
        }
        catch (NumberFormatException e) {}

        ServerSocket inputsock = new ServerSocket(port);
        Socket ss = inputsock.accept();
        InputStream in = ss.getInputStream();

        OutputStream out = ss.getOutputStream();
        
        RemoteBlockDeviceImpl impl = new RemoteBlockDeviceImpl(in, out, set.getBootDevice());
        
        LOGGING.log(Level.INFO, "Server accepted connection to {0} on port {1,number,integer}", new Object[]{set.getBootDevice(), Integer.valueOf(port)});
    }
    private RemoteBlockDeviceServer()
    {
    }
}
