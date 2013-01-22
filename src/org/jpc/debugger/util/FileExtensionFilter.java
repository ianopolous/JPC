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


package org.jpc.debugger.util;

import java.io.File;
import javax.swing.filechooser.FileFilter;

public class FileExtensionFilter extends FileFilter
{
    private String extension, description, lowerExtension;
    private boolean acceptDirectories, caseSensitive;

    public FileExtensionFilter(String extension, String description)
    {
        this(extension, description, true, false);
    }

    public FileExtensionFilter(String extension, String description, boolean acceptDirectories, boolean caseSensitive)
    {
        this.extension = extension;
        lowerExtension = extension.toLowerCase();

        this.description = description;
        this.acceptDirectories = acceptDirectories;
        this.caseSensitive = caseSensitive;
    }
    
    public boolean accept(File f) 
    {
        if (f.isDirectory())
            return acceptDirectories;

        String name = f.getName();
        if (!caseSensitive)
            return name.toLowerCase().endsWith(lowerExtension);
        else
            return name.endsWith(extension);
    }

    public String getDescription() 
    {
        return description;
    }
}
