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

import java.net.URI;
import java.io.IOException;
import java.io.File;

public class IOFactory {

    public static DataIO open(URI uri) throws IOException{
        if(uri.getScheme().equals("file")){
            File file = new File(uri);
            if(file.exists()){
                return new FileIO(uri);
            }else{
                throw new IOException("File cannot be found.");
            }      
        }else{
            throw new IOException("The URI scheme is not supported.");
        }
    }

    public static DataIO create(URI uri) throws IOException{
        if(uri.getScheme().equals("file")){
            return new FileIO(uri);
        }else{
            throw new IOException("The URI scheme is not supported.");
        }
    }
}
