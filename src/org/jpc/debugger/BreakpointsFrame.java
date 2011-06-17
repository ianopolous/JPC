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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import org.jpc.debugger.util.*;
import org.jpc.emulator.processor.Processor;

public class BreakpointsFrame extends UtilityFrame implements PCListener
{
    public static final String BREAKPOINT_FILE = "breakpoints.jpc";
    public static final long BREAKPOINT_MAGIC = 0x81057FAB7272F10l;

    private boolean edited;
    private List<Breakpoint> breakpoints;
    private BPModel model;
    private JTable bpTable;
    private String breakpointFileName;

    private JCheckBoxMenuItem ignoreBP, breakAtPrimary;

    public BreakpointsFrame()
    {
        super("Breakpoints");

        breakpointFileName = BREAKPOINT_FILE;
        breakpoints = new Vector();
        model = new BPModel();
        edited = false;

        bpTable = new JTable(model);
        model.setupColumnWidths(bpTable);

        String delBP = "Del BP";
        InputMap in = new InputMap();
        in.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), delBP);
        ActionMap ac = new ActionMap();
        ac.setParent(bpTable.getActionMap());
        ac.put(delBP, new Deleter());

        bpTable.setInputMap(JComponent.WHEN_FOCUSED, in);
        bpTable.setActionMap(ac);

        add("Center", new JScrollPane(bpTable));

        JMenu options = new JMenu("Options");
        options.add("Set Breakpoint").addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt)
            {
                try {
                    String input = JOptionPane.showInputDialog(BreakpointsFrame.this, "Enter the address (in Hex) for the breakpoint: ", "Breakpoint", JOptionPane.QUESTION_MESSAGE);
                    int address = (int) Long.parseLong(input.toLowerCase(), 16);
                    setAddressBreakpoint(address);
                } catch (Exception e) {
                }
            }
        });
        options.addSeparator();
        options.add("Remove All Breakpoints").addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt)
            {
                removeAllBreakpoints();
            }
        });
        
        options.addSeparator();
        ignoreBP = new JCheckBoxMenuItem("Ignore Breakpoints");
        options.add(ignoreBP);
        breakAtPrimary = new JCheckBoxMenuItem("Break at 'Primary' breakpoints only");
        options.add(breakAtPrimary);

        JMenuBar bar = new JMenuBar();
        bar.add(new BPFileMenu());
        bar.add(options);
        setJMenuBar(bar);

        setPreferredSize(new Dimension(450, 300));
        JPC.getInstance().objects().addObject(this);
        loadBreakpoints();
    }

    public boolean breakAtPrimaryOnly()
    {
        return breakAtPrimary.getState();
    }

    public boolean ignoreBreakpoints()
    {
        return ignoreBP.getState();
    }

    public boolean isEdited()
    {
        return edited;
    }

    public void frameClosed()
    {
        if (edited)
        {
            if (JOptionPane.showConfirmDialog(this, "Do you want to save the changes to the breakpoints?", "Save Breakpoints", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
                saveBreakpoints();
            edited = false;
        }
        
        JPC.getInstance().objects().removeObject(this);
    }

    class BPFileMenu extends JMenu implements ActionListener
    {
        private JMenuItem load, save, saveAs, importBP;

        BPFileMenu()
        {
            super("File");
            
            load = add("Load Breakpoints");
            load.addActionListener(this);
            save = add("Save Breakpoints");
            save.addActionListener(this);
            saveAs = add("Save Breakpoints As");
            saveAs.addActionListener(this);
            addSeparator();
            importBP = add("Import Breakpoints");
            importBP.addActionListener(this);
        }
        
        private String deriveBPFileName(String name)
        {
            String nm = name.toLowerCase();
            if (nm.endsWith(".jpc"))
                return name;

            int dot = nm.indexOf('.');
            if (dot < 0)
                dot = nm.length();

            return name.substring(0, dot)+".jpc";
        }
        
        public void actionPerformed(ActionEvent evt)
        {
            JFileChooser chooser = (JFileChooser) JPC.getObject(JFileChooser.class);
            if (evt.getSource() == load)
            {
                if (chooser.showOpenDialog(JPC.getInstance()) != JFileChooser.APPROVE_OPTION)
                    return;
                
                breakpointFileName = chooser.getSelectedFile().getAbsolutePath();
                removeAllBreakpoints();
                loadBreakpoints();
            }
            else if (evt.getSource() == save)
            {
                saveBreakpoints();
            }
            else if (evt.getSource() == importBP)
            {
                if (chooser.showOpenDialog(JPC.getInstance()) != JFileChooser.APPROVE_OPTION)
                    return;
                
                removeAllBreakpoints();
                String fileName = chooser.getSelectedFile().getAbsolutePath();
                importBreakpoints(fileName, false);
            }
            else if (evt.getSource() == saveAs)
            {
                if (chooser.showSaveDialog(JPC.getInstance()) != JFileChooser.APPROVE_OPTION)
                    return;
                
                breakpointFileName = chooser.getSelectedFile().getAbsolutePath();
                saveBreakpoints();
            }
        }
    }
    
    class Deleter extends AbstractAction
    {
        public void actionPerformed(ActionEvent evt)
        {
            deleteBreakpoint(bpTable.getSelectedRow());
        }
    }
    
    public boolean isBreakpoint(int address)
    {
        AddressBreakpoint bp = new AddressBreakpoint(address);
        return breakpoints.contains(bp);
    }

    public void setAddressBreakpoint(int address)
    {
        setAddressBreakpoint(address, false);
    }

    public void setAddressBreakpoint(int address, boolean isPrimary)
    {
        Breakpoint bp = new AddressBreakpoint(address);
        int idx = breakpoints.indexOf(bp);
        if (idx < 0)
            breakpoints.add(bp);
        else
            bp = breakpoints.get(idx);

        if (isPrimary)
            bp.setPrimary(isPrimary);

        edited = true; 
        JPC.getInstance().refresh();
    }

    public void removeAllBreakpoints()
    {
        breakpoints.clear();
        edited = true;
        JPC.getInstance().refresh();
    }

    public void removeBreakpoint(int address)
    {
        AddressBreakpoint bp = new AddressBreakpoint(address);
        int idx = breakpoints.indexOf(bp);
        if (idx < 0)
            return;

        deleteBreakpoint(idx);
    }

    public Breakpoint checkForBreak(int start, int end)
    {
        return checkForBreak(start, end, breakAtPrimary.getState());
    }

    public Breakpoint checkForBreak(int start, int end, boolean isPrimary)
    {
        if (ignoreBP.getState())
            return null;


        for (Breakpoint bp : breakpoints) {
            if ((bp.getAddress() == start) || ((bp.getAddress() >= start) && (bp.getAddress() < end))) 
            {
                if (isPrimary && !bp.isPrimary())
                    continue;

                return bp;
            }
        }

        return null;
    }

    private void deleteBreakpoint(int index)
    {
        try {
            breakpoints.remove(index);
        } catch (IndexOutOfBoundsException e) {
        }
        edited = true;

        JPC.getInstance().refresh();
    }

//    public class BreakCondition extends Breakpoint
//    {
//        BreakCondition()
//        {
//            
//        }
//    }
    
    public class AddressBreakpoint extends Breakpoint
    {

        AddressBreakpoint(int addr)
        {
            this(addr, false);
        }

        public AddressBreakpoint(String name, int addr)
        {
            super(name, addr, false);
        }
        
        AddressBreakpoint(int addr, boolean primary)
        {
            super("", addr, primary);
        }

        public boolean satisfied(Processor cpu)
        {
            return false;
        }
    }

    class BPModel extends BasicTableModel
    {
        BPModel()
        {
            super(new String[]{"Address", "Name", "Primary"}, new int[]{100, 250, 100});
        }
        
        public int getRowCount()
        {
            return breakpoints.size();
        }

        public boolean isCellEditable(int row, int column)
        {
            return true;
        }

        public Class getColumnClass(int col)
        {
            if (col == 2)
                return Boolean.class;
            return String.class;
        }

        public void setValueAt(Object obj, int row, int column)
        {
            Breakpoint bp = breakpoints.get(row);

            if (column == 0)
            {
                try
                {
                    int addr = (int) Long.parseLong(obj.toString().toLowerCase(), 16);
                    bp.setAddress(addr);
                }
                catch (Exception e) {}
            }
            else if (column == 2)
                bp.setPrimary(((Boolean) obj).booleanValue());
            else if (column == 1)
                bp.setName(obj.toString());

            int selected = sortBreakpoints(row);
            JPC.getInstance().refresh();

            if (selected >= 0)
            {
                bpTable.setRowSelectionInterval(selected, selected);
                Rectangle rect = bpTable.getCellRect(selected, 0, true);
                bpTable.scrollRectToVisible(rect);
            }
            edited = true;
        }

        public Object getValueAt(int row, int column)
        {
            Breakpoint bp = breakpoints.get(row);

            switch (column)
            {
            case 0:
                return MemoryViewPanel.zeroPadHex(bp.getAddress(), 8);
            case 1:
                return bp.getName();
            case 2:
                return new Boolean(bp.isPrimary());
            default:
                return "";
            }
        }
    }

    private int sortBreakpoints(int selectedRow)
    {
        Breakpoint selected = null;
        if (selectedRow >= 0)
            selected = breakpoints.get(selectedRow);

        Collections.sort(breakpoints);

        if (selected == null)
            return 0;
        
        for (int i = 0; i < breakpoints.size(); i++) {
            if (breakpoints.get(i) == selected)
                return i;
        }
        
        return 0;
    }

    public boolean importBreakpoints(String fileName, boolean ignoreDots)
    {
        List<Breakpoint> loaded = new ArrayList<Breakpoint>();

        File f = new File(fileName);
        if (!f.exists())
            return false;

        try {
            FileReader fin = new FileReader(f);
            try {
                BufferedReader in = new BufferedReader(fin);

                while (true) {

                    String line = in.readLine();
                    if (line == null)
                        break;

                    String[] elements = line.split("\\s", 2);

                    String name = elements[1];
                    if (name.startsWith(".") && ignoreDots)
                        continue;
                    
                    int addr = Integer.parseInt(elements[0], 16);

                    loaded.add(new AddressBreakpoint(name, addr));
                }
            } catch (IOException e) {
                return false;
            } finally {
                try {
                    fin.close();
                } catch (IOException e) {
                }
            }
        } catch (FileNotFoundException e) {
            return false;
        }

        breakpoints.clear();
        breakpoints.addAll(loaded);
        sortBreakpoints(-1);
        edited = true;
        return true;
    }

    public void loadBreakpoints()
    {
        FileInputStream fin = null;
        breakpoints.clear();

        try
        {
            File f = new File(breakpointFileName);
            if (!f.exists())
                return;

            fin = new FileInputStream(f);
            DataInputStream din = new DataInputStream(fin);
            
            if (din.readLong() != BREAKPOINT_MAGIC)
                throw new IOException("Magic number mismatch");

            while (true)
            {
                int addr = din.readInt();
                boolean primary = din.readBoolean();
                String name = din.readUTF();

                Breakpoint bp = new AddressBreakpoint(addr, primary);
                bp.setName(name);
                breakpoints.add(bp);
            }
        }
        catch (EOFException e) 
        {
            setTitle("Breakpoints: "+breakpointFileName);
        }
        catch (Exception e)
        {
            System.out.println("Warning: failed to load breakpoints");
            e.printStackTrace();
            setTitle("Breakpoints: "+breakpointFileName+" ERROR");
            alert("Error loading breakpoints: "+e, JOptionPane.ERROR_MESSAGE);
        }
        finally
        {
            try
            {
                fin.close();
            }
            catch (Exception e) {}
            sortBreakpoints(-1);
            edited = false;
        }       
    }

    public void saveBreakpoints()
    {
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(breakpointFileName);
            DataOutputStream dout = new DataOutputStream(out);
            dout.writeLong(BREAKPOINT_MAGIC);

            for (Breakpoint bp : breakpoints) {
                dout.writeInt(bp.getAddress());
                dout.writeBoolean(bp.isPrimary());
                dout.writeUTF(bp.getName());
            }
            
            setTitle("Breakpoints: "+breakpointFileName);
        }
        catch (Exception e)
        {
            System.out.println("Warning: failed to save breakpoints");
            e.printStackTrace();
            setTitle("Breakpoints: "+breakpointFileName+" ERROR");
            alert("Error saving breakpoints: "+e, JOptionPane.ERROR_MESSAGE);
        }
        finally
        {
            try
            {
                out.close();
            }
            catch (Exception e) {}
            edited = false;
        } 
    }

    public void pcCreated() {}

    public void pcDisposed() {}
    
    public void executionStarted() {}

    public void executionStopped() {}

    public void refreshDetails()
    {
        model.fireTableDataChanged();
    }
}
