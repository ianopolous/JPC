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

package org.jpc.nereus;

import java.net.URI;
import java.util.Locale;

/**
 *
 * @author Chris Dennis
 */
public final class ProtocolConstants
{
    public static final String TILE_X_POSITION = "Tile-X";
    public static final String TILE_Y_POSITION = "Tile-Y";
    public static final String SCREEN_WIDTH = "Size-X";
    public static final String SCREEN_HEIGHT = "Size-Y";
    public static final String KEYBOARD_DATA = "Keyboard-Data";
    public static final String UPDATE_TIMESTAMP = "Update-Timestamp";
    public static final URI GRAPHICS_UPDATE = URI.create("graphics_update");
    public static final String IMAGE_FORMAT = "PNG";
    public static final String IMAGE_MIME_TYPE = "image/" + IMAGE_FORMAT.toLowerCase(Locale.ROOT);

    private ProtocolConstants()
    {
    }
}
