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

package org.jpc.debugger;

import java.util.*;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.logging.*;

import javax.swing.*;

import org.jpc.debugger.util.*;
import org.jpc.emulator.execution.decoder.Instruction;
import org.jpc.emulator.memory.AddressSpace;
import org.jpc.emulator.execution.codeblock.*;

public class OpcodeFrequencyFrame extends UtilityFrame implements PCListener, ActionListener, CodeBlockListener, Comparator
{
    private static final Logger LOGGING = Logger.getLogger(CodeBlockFrequencyFrame.class.getName());
    
    private CodeBlockRecord record;
    private Map<String, OpcodeEntry> frequencies;

    private OpcodeEntry[] frequentCodes;
    private FrequencyModel model;

    public OpcodeFrequencyFrame() 
    {
        super("Opcode Frequencies");
        frequencies = new HashMap<String, OpcodeEntry>();
        record = null;
        frequentCodes = new OpcodeEntry[1000];

        setPreferredSize(new Dimension(600, 400));
        JPC.getInstance().objects().addObject(this);

        model = new FrequencyModel();
        JTable table = new JTable(model);
        model.setupColumnWidths(table);

        add("Center", new JScrollPane(table));
    }

    class OpcodeEntry
    {
        int frequency;
        String opName;

        OpcodeEntry(String name, CodeBlock b)
        { 
            frequency = 0;
            opName = name;
        }
        
        public int hashCode()
        {
            return opName.hashCode();
        }

        public boolean equals(Object obj)
        {
            OpcodeEntry e = (OpcodeEntry) obj;
            if (!e.opName.equals(opName))
                return false;

            return true;
        }
        

        public String toString()
        {
            return opName;
        }
    }

    public void actionPerformed(ActionEvent evt) {}

    public void codeBlockDecoded(int address, AddressSpace memory, CodeBlock block) {}

    public synchronized void codeBlockExecuted(int address, AddressSpace memory, CodeBlock block)
    {
	while (block instanceof AbstractCodeBlockWrapper)
	    block = ((AbstractCodeBlockWrapper)block).getTargetBlock();

        try
        {
            Instruction current = block.getInstructions();
            for (; current != null; current = current.next)
            {
                String name = current.getGeneralClassName(true, true);
                OpcodeEntry e = frequencies.get(name);
                if (e == null)
                {
                    e = new OpcodeEntry(name, block);
                    frequencies.put(name, e);
                }
                e.frequency++;
            }
        }
        catch (Exception e)
        {
            LOGGING.log(Level.WARNING, "Error calculating freuqency of block: " + block,e);
        }
    }

    public synchronized void frameClosed()
    {
        JPC.getInstance().objects().removeObject(this);
        if (record != null)
            record.setCodeBlockListener(null);
    }

    public void pcCreated()
    {
        refreshDetails();
    }

    public void pcDisposed()
    {
        record = null;
        model.fireTableDataChanged();
    }
    
    public void executionStarted() {}

    public void executionStopped() 
    {
        refreshDetails();
    }

    public int compare(Object o1, Object o2)
    {
        if (o1 == null)
        {
            if (o2 == null)
                return 0;
            else 
                return 1;
        }
        else if (o2 == null)
            return -1;

        OpcodeEntry e1 = (OpcodeEntry) o1;
        OpcodeEntry e2 = (OpcodeEntry) o2;
        
        return e2.frequency - e1.frequency;
    }

    public synchronized void refreshDetails()
    {
        CodeBlockRecord r = (CodeBlockRecord) JPC.getObject(CodeBlockRecord.class);
        if (r != record)
        {
            if (record != null)
                record.setCodeBlockListener(null);
            record = r;
            record.setCodeBlockListener(this);
            frequencies = new HashMap();
        }

        Object[] buffer = frequencies.values().toArray();
        Arrays.sort(buffer, this);
        Arrays.fill(frequentCodes, null);

        int limit = Math.min(buffer.length, frequentCodes.length);
        System.arraycopy(buffer, 0, frequentCodes, 0, limit);
        model.fireTableDataChanged();
    }

    class FrequencyModel extends BasicTableModel
    {
        FrequencyModel()
        {
            super(new String[]{"Rank", "Opcode", "Frequency"}, new int[]{80, 200, 80});
        }

        public int getRowCount()
        {
            return frequentCodes.length;
        }

        public Object getValueAt(int row, int column)
        {
            OpcodeEntry e = frequentCodes[row];
            if (e == null)
                return null;

            switch (column)
            {
	    case 0:
		return Integer.valueOf(row);
            case 1:
                return e.toString();
            case 2:
                return Integer.valueOf(e.frequency);
            }

            return null;
        }
    }
}
