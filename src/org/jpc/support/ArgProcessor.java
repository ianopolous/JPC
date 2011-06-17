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

/**
 * Provides simple command line parsing for the various frontends to the
 * emulator.
 * @author Rhys Newman
 */
public class ArgProcessor
{
    /**
     * Finds the value of the variable <code>key</code>.  Searches the given
     * commandline for <code>-key value</code>
     * @param args array of strings to search
     * @param key key to search for
     * @param defaultValue value returned on failure
     * @return result, or <code>defaultValue</code> on failure
     */
    public static String findVariable(String[] args, String key, String defaultValue)
    {           
        int keyIndex = findKey(args, key);
        if (keyIndex < 0)
            return defaultValue;
        
        if ((keyIndex + 1) < args.length)
            return args[keyIndex + 1];
        else
            return defaultValue;
    }

    /**
     * Searches for the presence of the given flag on the command line as 
     * <code>-flag</code>
     * @param args array of strings to search
     * @param flag parameter to search for
     * @return true if flag is found
     */
    public static boolean findFlag(String[] args, String flag)
    {
        return findKey(args, flag) >= 0;
    }

    private static int findKey(String[] args, String key)
    {
        if (key.startsWith("-"))
            key = key.substring(1);
        
        for (int i=0; i<args.length; i++)
        {
            if (!args[i].startsWith("-"))
                continue;
            
            if (args[i].substring(1).equalsIgnoreCase(key))
                return i;
        }
        
        return -1;
    }
    
    private ArgProcessor()
    {
    }
}
