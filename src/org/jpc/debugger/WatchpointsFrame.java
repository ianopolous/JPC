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
import org.jpc.emulator.memory.AddressSpace;

import javax.swing.*;

import org.jpc.debugger.util.*;
import org.jpc.emulator.memory.PhysicalAddressSpace;

public class WatchpointsFrame extends UtilityFrame implements PCListener
{
    public static final String WATCHPOINT_FILE = "watchpoints.jpc";
    public static final long WATCHPOINT_MAGIC = 0x81057FAB7272F11l;

    private boolean edited;
    private List<Watchpoint> watchpoints;
    private WPModel model;
    private JTable wpTable;
    private String watchpointFileName;
    private AddressSpace addressSpace;

    private JCheckBoxMenuItem ignoreWP, watchPrimary;

    public WatchpointsFrame()
    {
        super("Watchpoints");

        watchpointFileName = WATCHPOINT_FILE;
        watchpoints = new Vector();
        model = new WPModel();
        edited = false;
        
        addressSpace=(AddressSpace) JPC.getObject(PhysicalAddressSpace.class);

        wpTable = new JTable(model);
        model.setupColumnWidths(wpTable);

        String delWP = "Del WP";
        InputMap in = new InputMap();
        in.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), delWP);
        ActionMap ac = new ActionMap();
        ac.setParent(wpTable.getActionMap());
        ac.put(delWP, new Deleter());

        wpTable.setInputMap(JComponent.WHEN_FOCUSED, in);
        wpTable.setActionMap(ac);

        add("Center", new JScrollPane(wpTable));

        JMenu options = new JMenu("Options");
        options.add("Set Watchpoint").addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt)
            {
                try {
                    String input = JOptionPane.showInputDialog(WatchpointsFrame.this, "Enter the address (in Hex) for the watchpoint: ", "Watchpoint", JOptionPane.QUESTION_MESSAGE);
                    int address = (int) Long.parseLong(input.toLowerCase(), 16);
                    setWatchpoint(address);
                } catch (Exception e) {
                }
            }
        });
        options.addSeparator();
        options.add("Remove All Watchpoints").addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt)
            {
                removeAllWatchpoints();
            }
        });
        
        options.addSeparator();
        ignoreWP = new JCheckBoxMenuItem("Ignore Watchpoints");
        options.add(ignoreWP);
        watchPrimary = new JCheckBoxMenuItem("Watch 'Primary' watchpoints only");
        options.add(watchPrimary);

        JMenuBar bar = new JMenuBar();
        bar.add(new WPFileMenu());
        bar.add(options);
        setJMenuBar(bar);

        setPreferredSize(new Dimension(450, 300));
        JPC.getInstance().objects().addObject(this);
        loadWatchpoints();
    }

    public boolean WatchPrimaryOnly()
    {
        return watchPrimary.getState();
    }

    public boolean ignoreWatchpoints()
    {
        return ignoreWP.getState();
    }

    public boolean isEdited()
    {
        return edited;
    }

    public void frameClosed()
    {
        if (edited)
        {
            if (JOptionPane.showConfirmDialog(this, "Do you want to save the changes to the Watchpoints?", "Save Watchpoints", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
                saveWatchpoints();
            edited = false;
        }
        
        JPC.getInstance().objects().removeObject(this);
    }

    class WPFileMenu extends JMenu implements ActionListener
    {
        private JMenuItem load, save, saveAs, importWP;

        WPFileMenu()
        {
            super("File");
            
            load = add("Load Watchpoints");
            load.addActionListener(this);
            save = add("Save Watchpoints");
            save.addActionListener(this);
            saveAs = add("Save Watchpoints As");
            saveAs.addActionListener(this);
            addSeparator();
            importWP = add("Import Watchpoints");
            importWP.addActionListener(this);
        }
        
        private String deriveWPFileName(String name)
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
                
                watchpointFileName = chooser.getSelectedFile().getAbsolutePath();
                removeAllWatchpoints();
                loadWatchpoints();
            }
            else if (evt.getSource() == save)
            {
                saveWatchpoints();
            }
            else if (evt.getSource() == importWP)
            {
                if (chooser.showOpenDialog(JPC.getInstance()) != JFileChooser.APPROVE_OPTION)
                    return;
                
                removeAllWatchpoints();
                String fileName = chooser.getSelectedFile().getAbsolutePath();
                importWatchpoints(fileName, false);
            }
            else if (evt.getSource() == saveAs)
            {
                if (chooser.showSaveDialog(JPC.getInstance()) != JFileChooser.APPROVE_OPTION)
                    return;
                
                watchpointFileName = chooser.getSelectedFile().getAbsolutePath();
                saveWatchpoints();
            }
        }
    }
    
    class Deleter extends AbstractAction
    {
        public void actionPerformed(ActionEvent evt)
        {
            deleteWatchpoint(wpTable.getSelectedRow());
        }
    }
    
    public boolean isWatchpoint(int address)
    {
        Watchpoint wp = new Watchpoint(address);
        return watchpoints.contains(wp);
    }

    public void setWatchpoint(int address)
    {
        setWatchpoint(address, false);
    }

    public void setWatchpoint(int address, boolean isPrimary)
    {
        Watchpoint wp = new Watchpoint(address);
        int idx = watchpoints.indexOf(wp);
        if (idx < 0)
            watchpoints.add(wp);
        else
            wp = watchpoints.get(idx);

        if (isPrimary)
            wp.isPrimary = isPrimary;

        edited = true; 
        JPC.getInstance().refresh();
    }

    public void removeAllWatchpoints()
    {
        watchpoints.clear();
        edited = true;
        JPC.getInstance().refresh();
    }

    public void removeWatchpoint(int address)
    {
        Watchpoint wp = new Watchpoint(address);
        int idx = watchpoints.indexOf(wp);
        if (idx < 0)
            return;

        deleteWatchpoint(idx);
    }

    public Watchpoint checkForWatch()
    {
        return checkForWatch(watchPrimary.getState());
    }

    public Watchpoint checkForWatch(boolean isPrimary)
    {
        if (ignoreWP.getState())
            return null;


        for (Watchpoint wp : watchpoints) {
            byte b = addressSpace.getByte(wp.address);
            //if ((b != (byte) 0xff) && (b != 0) && (wp.value!=b))
            if (wp.value!=b)
            {
                if (isPrimary && !wp.isPrimary)
                    continue;

                return wp;
            }
        }

        return null;
    }

    private void deleteWatchpoint(int index)
    {
        try {
            watchpoints.remove(index);
        } catch (IndexOutOfBoundsException e) {
        }
        edited = true;

        JPC.getInstance().refresh();
    }

    public class Watchpoint implements Comparable<Watchpoint>
    {
        private int address;
        private int value;
        private boolean isPrimary;
        private String name;

        Watchpoint(int addr)
        {
            this(addr, false);
        }

        public Watchpoint(String name, int addr)
        {
            this(addr);
            this.name = name;
        }
        
        Watchpoint(int addr, boolean primary)
        {
            address = addr;
            isPrimary = primary;
            name = "";
            value=addressSpace.getByte(addr);
        }

        
        public boolean equals(Object another)
        {
            if (!(another instanceof Watchpoint))
                return false;

            return address == ((Watchpoint) another).address;
        }

        public int compareTo(Watchpoint wp)
        {
            return address - wp.address;
        }

        public String getName()
        {
            return name;
        }
        
        public int getAddress()
        {
            return address;
        }

        public int getValue()
        {
            return value;
        }
 
        public void updateValue()
        {
            value=addressSpace.getByte(address);
        }
        
        public boolean isPrimary()
        {
            return isPrimary;
        }
    }

    class WPModel extends BasicTableModel
    {
        WPModel()
        {
            super(new String[]{"Address", "Name", "Primary"}, new int[]{100, 250, 100});
        }
        
        public int getRowCount()
        {
            return watchpoints.size();
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
            Watchpoint wp = watchpoints.get(row);

            if (column == 0)
            {
                try
                {
                    int addr = (int) Long.parseLong(obj.toString().toLowerCase(), 16);
                    wp.address = addr;
                }
                catch (Exception e) {}
            }
            else if (column == 2)
                wp.isPrimary = ((Boolean) obj).booleanValue();
            else if (column == 1)
                wp.name = obj.toString();

            int selected = sortWatchpoints(row);
            JPC.getInstance().refresh();

            if (selected >= 0)
            {
                wpTable.setRowSelectionInterval(selected, selected);
                Rectangle rect = wpTable.getCellRect(selected, 0, true);
                wpTable.scrollRectToVisible(rect);
            }
            edited = true;
        }

        public Object getValueAt(int row, int column)
        {
            Watchpoint wp = watchpoints.get(row);

            switch (column)
            {
            case 0:
                return MemoryViewPanel.zeroPadHex(wp.address, 8);
            case 1:
                return wp.name;
            case 2:
                return new Boolean(wp.isPrimary);
            default:
                return "";
            }
        }
    }

    private int sortWatchpoints(int selectedRow)
    {
        Watchpoint selected = null;
        if (selectedRow >= 0)
            selected = watchpoints.get(selectedRow);

        Collections.sort(watchpoints);

        if (selected == null)
            return 0;
        
        for (int i = 0; i < watchpoints.size(); i++) {
            if (watchpoints.get(i) == selected)
                return i;
        }
        
        return 0;
    }

    public boolean importWatchpoints(String fileName, boolean ignoreDots)
    {
        List<Watchpoint> loaded = new ArrayList<Watchpoint>();

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

                    loaded.add(new Watchpoint(name, addr));
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

        watchpoints.clear();
        watchpoints.addAll(loaded);
        sortWatchpoints(-1);
        edited = true;
        return true;
    }

    public void loadWatchpoints()
    {
        FileInputStream fin = null;
        watchpoints.clear();

        try
        {
            File f = new File(watchpointFileName);
            if (!f.exists())
                return;

            fin = new FileInputStream(f);
            DataInputStream din = new DataInputStream(fin);
            
            if (din.readLong() != WATCHPOINT_MAGIC)
                throw new IOException("Magic number mismatch");

            while (true)
            {
                int addr = din.readInt();
                boolean primary = din.readBoolean();
                String name = din.readUTF();

                Watchpoint wp = new Watchpoint(addr, primary);
                wp.name = name;
                watchpoints.add(wp);
            }
        }
        catch (EOFException e) 
        {
            setTitle("Watchpoints: "+watchpointFileName);
        }
        catch (Exception e)
        {
            System.out.println("Warning: failed to load watchpoints");
            e.printStackTrace();
            setTitle("Watchpoints: "+watchpointFileName+" ERROR");
            alert("Error loading watchpoints: "+e, JOptionPane.ERROR_MESSAGE);
        }
        finally
        {
            try
            {
                fin.close();
            }
            catch (Exception e) {}
            sortWatchpoints(-1);
            edited = false;
        }       
    }

    public void saveWatchpoints()
    {
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(watchpointFileName);
            DataOutputStream dout = new DataOutputStream(out);
            dout.writeLong(WATCHPOINT_MAGIC);

            for (Watchpoint wp : watchpoints) {
                dout.writeInt(wp.address);
                dout.writeBoolean(wp.isPrimary);
                dout.writeUTF(wp.name);
            }
            
            setTitle("Watchpoints: "+watchpointFileName);
        }
        catch (Exception e)
        {
            System.out.println("Warning: failed to save watchpoints");
            e.printStackTrace();
            setTitle("Watchpoints: "+watchpointFileName+" ERROR");
            alert("Error saving watchpoints: "+e, JOptionPane.ERROR_MESSAGE);
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
