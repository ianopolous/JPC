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

import java.util.*;

public class LimitedLinkedHashMap<K, V> extends LinkedHashMap{
    private int maxCapacity;
    private static final int MAX_ENTRIES = 128;

    public LimitedLinkedHashMap(){
        this(MAX_ENTRIES);
    }

    public LimitedLinkedHashMap(int maxCapacity){
        super();
        this.maxCapacity = maxCapacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest){
        return size() > this.maxCapacity;
    }
}
