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


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public class ClassVerifier {

    
    public static void main(String[] args) throws IOException {
        if (args.length != 1)
        {
            System.out.println("Usage: java ClassVerifier inputjar.jar");
            System.exit(0);
        }

        JarInputStream in = new JarInputStream(new FileInputStream(args[0]));
        JarOutputStream out = new JarOutputStream(new FileOutputStream(new File("outclasses.jar")));

        JarEntry entry = null;
        while ((entry = in.getNextJarEntry()) != null) {
            if (entry.isDirectory())
                continue;
            String name = entry.getName();
            name = name.replace("org/jpc/dynamic/", "org.jpc.dynamic.");
            name = name.replace(".class", "");
            try {
                Class.forName(name).newInstance();
                out.putNextEntry(entry);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                int data = 0;
                while ((data = in.read()) != -1) {
                    bout.write(data);
                }
                out.write(bout.toByteArray());
                out.flush();
                out.closeEntry();
            } catch (ClassNotFoundException e) { e.printStackTrace();}
            catch (IllegalAccessException e) {e.printStackTrace();}
            catch (InstantiationException e) {e.printStackTrace();}
            catch (VerifyError e) {
                System.out.println("Ignoring class: " + name);
            }
        }
        out.close();
    }
}
