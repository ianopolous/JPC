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


public class CmdMain {



    public static void main(String[] args) throws Exception
    {

        String usage = "usage: java -jar JPCImg.jar -in <input uri> -infmt <input format> -out <output uri> -outfmt <output format>";
        if(args.length != 8){
            System.out.println(usage);
            System.exit(0);
        }


        URI inputURI = null;
        URI outputURI = null;
        String inputFormat = null;
        String outputFormat = null;

        for(int i=0; i<args.length; i++){
            if(args[i].equals("-in")){
                if(args[i+1] != null){
                    inputURI = new URI(args[i+1].replace("\\", "/"));
                }
            }else if(args[i].equals("-infmt")){
                if(args[i+1] != null){
                    inputFormat = args[i+1];
                }
            }else if(args[i].equals("-out")){
                if(args[i+1] != null){
                    outputURI = new URI(args[i+1].replace("\\", "/"));
                }
            }else if(args[i].equals("-outfmt")){
                if(args[i+1] != null){
                    outputFormat = args[i+1];
                }
            }
        }

        if(inputURI == null || outputURI == null || inputFormat == null || outputFormat == null){
            System.out.println(usage);
            System.exit(1);
        }

        Converter.convert(inputURI, outputURI, inputFormat, outputFormat, 0);
    }
}
