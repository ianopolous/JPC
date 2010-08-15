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
import java.util.zip.*;
import java.io.*;
import java.text.DecimalFormat;
import java.awt.Color;
import java.awt.event.*;

import javax.swing.*;

import org.jpc.debugger.util.*;
import org.jpc.emulator.PC;
import org.jpc.support.*;
import org.jpc.emulator.memory.*;
import org.jpc.emulator.processor.Processor;
import org.jpc.emulator.peripheral.Keyboard;
import org.jpc.emulator.pci.peripheral.VGACard;
import org.jpc.j2se.VirtualClock;

public class JPC extends ApplicationFrame implements ActionListener {

    private static JPC instance = null;
    private ObjectDatabase objects;
    private RunMenu runMenu;
    private CodeBlockRecord codeBlocks;
    private JDesktopPane desktop;
    private DiskSelector floppyDisk,  hardDisk, cdrom;
    private JMenuItem createPC,  scanForImages,  loadSnapshot,  saveSnapshot;
    private JMenuItem processorFrame,  physicalMemoryViewer,  linearMemoryViewer,  watchpoints,  breakpoints,  opcodeFrame,  traceFrame,  monitor,  codeBlockTreeFrame;
    public static Process p = null;
    public static BufferedReader input = null;
    public static BufferedWriter output = null;

    private JPC(boolean fullScreen) {
        super("JPC Debugger");

        if (fullScreen) {
            setBoundsToMaximum();
        } else {
            setBounds(0, 0, 1024, 900);
        }
        objects = new ObjectDatabase();
        desktop = new JDesktopPane();
        add("Center", desktop);

        JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        objects.addObject(chooser);

        JMenuBar bar = new JMenuBar();
        JMenu actions = new JMenu("Actions");
        createPC = actions.add("Create New PC");
        createPC.setEnabled(false);
        createPC.addActionListener(this);
        scanForImages = actions.add("Scan Directory for Images");
        scanForImages.addActionListener(this);
        actions.addSeparator();
        loadSnapshot = actions.add("Load Snapshot");
        loadSnapshot.addActionListener(this);
        saveSnapshot = actions.add("Save Snapshot");
        saveSnapshot.addActionListener(this);
        actions.addSeparator();
        actions.add("Quit").addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                frameCloseRequested();
            }
        });



        JMenu windows = new JMenu("Windows");
        monitor = windows.add("PC Monitor");
        monitor.addActionListener(this);
        processorFrame = windows.add("Processor Frame");
        processorFrame.addActionListener(this);
        physicalMemoryViewer = windows.add("Physical Memory Viewer");
        physicalMemoryViewer.addActionListener(this);
        linearMemoryViewer = windows.add("Linear Memory Viewer");
        linearMemoryViewer.addActionListener(this);
        breakpoints = windows.add("Breakpoints");
        breakpoints.addActionListener(this);
        watchpoints = windows.add("Watchpoints");
        watchpoints.addActionListener(this);
        opcodeFrame = windows.add("Opcode Frame");
        opcodeFrame.addActionListener(this);
        traceFrame = windows.add("Execution Trace Frame");
        traceFrame.addActionListener(this);
//         frequencies = windows.add("Opcode Frequency Frame");
//         frequencies.addActionListener(this);
        codeBlockTreeFrame = windows.add("Code Block Tree Frame");
        codeBlockTreeFrame.addActionListener(this);

        JMenu tools = new JMenu("Tools");
        tools.add("Create Blank Disk (file)").addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createBlankHardDisk();
            }
        });

        runMenu = new RunMenu();

        floppyDisk = new DiskSelector("FD", Color.red);
        hardDisk = new DiskSelector("HD", Color.blue);
        cdrom = new DiskSelector("CD", Color.blue);

        bar.add(actions);
        bar.add(windows);
        bar.add(runMenu);
        bar.add(tools);
        bar.add(floppyDisk);
        bar.add(hardDisk);
        bar.add(cdrom);

        bar.add(Box.createHorizontalGlue());
        bar.add(new Hz());

        codeBlocks = null;
        setJMenuBar(bar);

        resyncImageSelection(new File(System.getProperty("user.dir")));
    }

    private void resyncImageSelection(File dir) {
        floppyDisk.rescan(dir);
        hardDisk.rescan(dir);
        cdrom.rescan(dir);

        checkBootEnabled();
    }

    private void checkBootEnabled() {
        createPC.setEnabled(floppyDisk.isBootDevice() || hardDisk.isBootDevice() || cdrom.isBootDevice());
    }

    class DiskSelector extends JMenu implements ActionListener {

        String mainTitle;
        ButtonGroup group;
        List<File> diskImages;
        Map<ButtonModel, File> lookup;
        JCheckBoxMenuItem bootFrom;
        JMenuItem openFile;

        public DiskSelector(String mainTitle, Color fg) {
            super(mainTitle);
            this.mainTitle = mainTitle;
            setForeground(fg);

            lookup = new HashMap();
            diskImages = new Vector();
            group = new ButtonGroup();
            bootFrom = new JCheckBoxMenuItem("Set as Boot Device");
            bootFrom.addActionListener(this);
            openFile = new JMenuItem("Select Image File");
            openFile.addActionListener(this);
        }

        public void actionPerformed(ActionEvent evt) {
            if (evt.getSource() == openFile) {
                JFileChooser chooser = (JFileChooser) objects.getObject(JFileChooser.class);
                if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                rescan(chooser.getSelectedFile());
            }

            resetTitle();
        }

        public void setSelectedFile(File f, boolean isBootDevice) {
            rescan(f);
            bootFrom.setState(isBootDevice);
            resetTitle();
        }

        private void resetTitle() {
            String fileName = "";
            File f = getSelectedFile();
            if (f != null) {
                fileName = f.getAbsolutePath();
            }
            if (isBootDevice()) {
                setText(mainTitle + " >" + fileName + "<");
            } else {
                setText(mainTitle + " " + fileName);
            }
            checkBootEnabled();
            if (bootFrom.getState() && (getSelectedFile() == null)) {
                bootFrom.setState(false);
            }
        }

        public File getSelectedFile() {
            ButtonModel selectedModel = group.getSelection();
            if (selectedModel == null) {
                return null;
            }
            return lookup.get(selectedModel);
        }

        public boolean isBootDevice() {
            return bootFrom.getState() && (getSelectedFile() != null);
        }

        void rescan(File f) {
            File selected = getSelectedFile();
            boolean isBoot = isBootDevice();

            for (int i = diskImages.size() - 1; i >= 0; i--) {
                if (!diskImages.get(i).exists()) {
                    diskImages.remove(i);
                }
            }
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getName().toLowerCase().endsWith(".img")) {
                        if (!diskImages.contains(files[i])) {
                            diskImages.add(files[i]);
                        }
                    }
                }
            } else if (f.exists()) {
                boolean found = false;
                for (int i = 0; i < diskImages.size(); i++) {
                    if (diskImages.get(i).getAbsolutePath().equals(f.getAbsolutePath())) {
                        selected = diskImages.get(i);
                        found = true;
                    }
                }
                if (!found) {
                    diskImages.add(f);
                    selected = f;
                }
            }

            removeAll();
            lookup.clear();

            group = new ButtonGroup();
            bootFrom.setState(isBoot);
            add(bootFrom);
            addSeparator();

            for (int i = 0; i < diskImages.size(); i++) {
                File ff = diskImages.get(i);
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(ff.getAbsolutePath());
                item.addActionListener(this);
                lookup.put(item.getModel(), ff);

                group.add(item);
                add(item);

                if (ff.equals(selected)) {
                    group.setSelected(item.getModel(), true);
                }
            }

            addSeparator();
            add(openFile);
        }
    }

    // Hook for F2 - print status report
    public void statusReport() {
        System.out.println("No status to report");
    }

    public Object get(Class cls) {
        return objects.getObject(cls);
    }

    public ObjectDatabase objects() {
        return objects;
    }

    public JDesktopPane getDesktop() {
        return desktop;
    }

    protected void frameCloseRequested() {
        BreakpointsFrame bp = (BreakpointsFrame) objects.getObject(BreakpointsFrame.class);
        if ((bp != null) && bp.isEdited()) {
            bp.dispose();
        }
        WatchpointsFrame wp = (WatchpointsFrame) objects.getObject(WatchpointsFrame.class);
        if ((wp != null) && wp.isEdited()) {
            wp.dispose();
        }
        System.exit(0);
    }

    public void bringToFront(JInternalFrame f) {
        desktop.moveToFront(f);
        desktop.setSelectedFrame(f);
    }

    public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();

        if (src == scanForImages) {
            JFileChooser chooser = (JFileChooser) objects.getObject(JFileChooser.class);
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File dir = chooser.getSelectedFile();
            if (!dir.isDirectory()) {
                dir = dir.getParentFile();
            }
            resyncImageSelection(dir);
        } else if (src == createPC) {
            try {
                File floppyImage = floppyDisk.getSelectedFile();
                File hardImage = hardDisk.getSelectedFile();
                File cdImage = cdrom.getSelectedFile();

                DriveSet.BootType bootType;
                if (floppyDisk.isBootDevice()) {
                    if (!floppyImage.exists()) {
                        alert("Floppy Image: " + floppyImage + " does not exist", "Boot", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    bootType = DriveSet.BootType.FLOPPY;
                } else if (hardDisk.isBootDevice())
                {
                    if (!hardImage.exists()) {
                        alert("Hard disk Image: " + hardImage + " does not exist", "Boot", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    bootType = DriveSet.BootType.HARD_DRIVE;
                } else {
                    if (!cdImage.exists()) {
                        alert("CD Image: " + cdImage + " does not exist", "Boot", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    bootType = DriveSet.BootType.CDROM;
                }

                String[] args;
                int argc = 0;
                if (floppyImage != null) {
                    argc += 2;
                }
                if (hardImage != null) {
                    argc += 2;
                }
                if (cdImage != null) {
                    argc += 2;
                }
                if (argc > 2) {
                    argc += 2;
                }
                args = new String[argc];

                int pos = 0;
                if (floppyImage != null) {
                    args[pos++] = "-fda";
                    args[pos++] = floppyImage.getAbsolutePath();
                }
                if (hardImage != null) {
                    args[pos++] = "-hda";
                    args[pos++] = hardImage.getAbsolutePath();
                }
                if (cdImage != null) {
                    args[pos++] = "-cdrom";
                    args[pos++] = cdImage.getAbsolutePath();
                }
                if (pos <= (argc - 2)) {
                    args[pos++] = "-boot";
                    if (bootType == DriveSet.BootType.HARD_DRIVE) {
                        args[pos++] = "hda";
                    } else if (bootType == DriveSet.BootType.CDROM) {
                        args[pos++] = "cdrom";
                    } else {
                        args[pos++] = "fda";
                    }
                }

                instance.createPC(args);
                resyncImageSelection(new File(System.getProperty("user.dir")));
            } catch (Exception e) {
                alert("Failed to create PC: " + e, "Boot", JOptionPane.ERROR_MESSAGE);
            }
        } else if (src == loadSnapshot) {
            runMenu.stop();
            JFileChooser fc = new JFileChooser();
            try {
                BufferedReader in = new BufferedReader(new FileReader("prefs.txt"));
                String path = in.readLine();
                in.close();
                if (path != null) {
                    File f = new File(path);
                    if (f.isDirectory()) {
                        fc.setCurrentDirectory(f);
                    }
                }
            } catch (Exception e) {
            }

            int returnVal = fc.showDialog(this, "Load JPC Snapshot");
            File file = fc.getSelectedFile();
            try {
                if (file != null) {
                    BufferedWriter out = new BufferedWriter(new FileWriter("prefs.txt"));
                    out.write(file.getPath());
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (returnVal == 0) {
                try {
                    System.out.println("Loading a snapshot of JPC");
                    ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
                    zin.getNextEntry();
                    ((PC) objects.getObject(PC.class)).loadState(zin);
                    zin.closeEntry();
                    ((PCMonitorFrame) objects.getObject(PCMonitorFrame.class)).resizeDisplay();
                    zin.getNextEntry();
                    ((PCMonitorFrame) objects.getObject(PCMonitorFrame.class)).loadMonitorState(zin);
                    zin.closeEntry();
                    zin.close();
                    System.out.println("done");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (src == saveSnapshot) {
            runMenu.stop();
            JFileChooser fc = new JFileChooser();
            try {
                BufferedReader in = new BufferedReader(new FileReader("prefs.txt"));
                String path = in.readLine();
                in.close();
                if (path != null) {
                    File f = new File(path);
                    if (f.isDirectory()) {
                        fc.setCurrentDirectory(f);
                    }
                }
            } catch (Exception e) {
            }

            int returnVal = fc.showDialog(this, "Save JPC Snapshot");
            File file = fc.getSelectedFile();
            try {
                if (file != null) {
                    BufferedWriter out = new BufferedWriter(new FileWriter("prefs.txt"));
                    out.write(file.getPath());
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (returnVal == 0) {
                try {
                    ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file));
                    zip.putNextEntry(new ZipEntry("pc"));
                    ((PC) objects.getObject(PC.class)).saveState(zip);
                    zip.closeEntry();
                    zip.putNextEntry(new ZipEntry("monitor"));
                    ((PCMonitorFrame) objects.getObject(PCMonitorFrame.class)).saveState(zip);
                    zip.closeEntry();
                    zip.finish();
                    zip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (src == processorFrame) {
            ProcessorFrame pf = (ProcessorFrame) objects.getObject(ProcessorFrame.class);
            if (pf != null) {
                bringToFront(pf);
            } else {
                pf = new ProcessorFrame();
                addInternalFrame(desktop, 10, 10, pf);
            }
        } else if (src == physicalMemoryViewer) {
            MemoryViewer mv = (MemoryViewer) objects.getObject(MemoryViewer.class);

            if (mv != null) {
                bringToFront(mv);
            } else {
                mv = new MemoryViewer("Physical Memory");
                addInternalFrame(desktop, 360, 50, mv);
            }
        } else if (src == linearMemoryViewer) {
            LinearMemoryViewer lmv = (LinearMemoryViewer) objects.getObject(LinearMemoryViewer.class);

            if (lmv != null) {
                bringToFront(lmv);
            } else {
                lmv = new LinearMemoryViewer("Linear Memory");
                addInternalFrame(desktop, 360, 50, lmv);
            }
        } else if (src == breakpoints) {
            BreakpointsFrame bp = (BreakpointsFrame) objects.getObject(BreakpointsFrame.class);
            if (bp != null) {
                bringToFront(bp);
            } else {
                bp = new BreakpointsFrame();
                addInternalFrame(desktop, 550, 360, bp);
            }
        } else if (src == watchpoints) {
            WatchpointsFrame wp = (WatchpointsFrame) objects.getObject(WatchpointsFrame.class);
            if (wp != null) {
                bringToFront(wp);
            } else {
                wp = new WatchpointsFrame();
                addInternalFrame(desktop, 550, 360, wp);
            }
        } else if (src == opcodeFrame) {
            OpcodeFrame op = (OpcodeFrame) objects.getObject(OpcodeFrame.class);
            if (op != null) {
                bringToFront(op);
            } else {
                op = new OpcodeFrame();
                addInternalFrame(desktop, 100, 200, op);
            }
        } else if (src == traceFrame) {
            ExecutionTraceFrame tr = (ExecutionTraceFrame) objects.getObject(ExecutionTraceFrame.class);
            if (tr != null) {
                bringToFront(tr);
            } else {
                tr = new ExecutionTraceFrame();
                addInternalFrame(desktop, 30, 100, tr);
            }
        } else if (src == monitor) {
            PCMonitorFrame m = (PCMonitorFrame) objects.getObject(PCMonitorFrame.class);
            if (m != null) {
                bringToFront(m);
            } else {
                m = new PCMonitorFrame();
                addInternalFrame(desktop, 30, 30, m);
            }
        }
//         else if (src == frequencies)
//         {
//             OpcodeFrequencyFrame f = (OpcodeFrequencyFrame) objects.getObject(OpcodeFrequencyFrame.class);
//             if (f != null)
//                 bringToFront(f);
//             else
//             {
//                 f = new OpcodeFrequencyFrame();
//                 addInternalFrame(desktop, 550, 30, f);
//             }
//         }

        refresh();
    }

    public void notifyExecutionStarted() {
        for (Object obj : objects.entries()) {
            if (!(obj instanceof PCListener)) {
                continue;
            }
            try {
                ((PCListener) obj).executionStarted();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void notifyExecutionStopped() {
        for (Object obj : objects.entries()) {
            if (!(obj instanceof PCListener)) {
                continue;
            }
            try {
                ((PCListener) obj).executionStopped();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void notifyPCDisposed() {
        for (Object obj : objects.entries()) {
            if (!(obj instanceof PCListener)) {
                continue;
            }
            try {
                ((PCListener) obj).pcDisposed();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void notifyPCCreated() {
        for (Object obj : objects.entries()) {
            if (!(obj instanceof PCListener)) {
                continue;
            }
            try {
                ((PCListener) obj).pcCreated();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void refresh() {
        for (Object obj : objects.entries()) {
            if (!(obj instanceof PCListener)) {
                continue;
            }
            try {
                ((PCListener) obj).refreshDetails();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public PC loadNewPC(PC pc) {
        PC oldPC = (PC) objects.removeObject(PC.class);
        if (oldPC != null) {
            notifyPCDisposed();
        }
        JInternalFrame[] frames = desktop.getAllFrames();
        for (int i = 0; i < frames.length; i++) {
            frames[i].dispose();
        }
        runMenu.refresh();

        objects.removeObject(Processor.class);
        objects.removeObject(PhysicalAddressSpace.class);
        objects.removeObject(LinearAddressSpace.class);
        objects.removeObject(VGACard.class);
        objects.removeObject(Keyboard.class);
        objects.removeObject(ProcessorAccess.class);
        objects.removeObject(CodeBlockRecord.class);

        for (int i = 0; i < 10; i++) {
            System.gc();
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }

        setTitle("JPC Debugger - Boot Device: " + ((DriveSet) pc.getComponent(DriveSet.class)).getBootDevice());
        objects.addObject(pc);
        objects.addObject(pc.getProcessor());
        objects.addObject(pc.getComponent(LinearAddressSpace.class));
        objects.addObject(pc.getComponent(PhysicalAddressSpace.class));
        objects.addObject(pc.getComponent(VGACard.class));
        objects.addObject(pc.getComponent(Keyboard.class));

        ProcessorAccess pca = new ProcessorAccess(pc.getProcessor());
        codeBlocks = new CodeBlockRecord(pc);

        objects.addObject(pca);
        objects.addObject(codeBlocks);

        runMenu.refresh();
        notifyPCCreated();

        //processorFrame.doClick();
        //breakpoints.doClick();
        monitor.doClick();
        //codeBlockTreeFrame.doClick();
        //opcodeFrame.doClick();

        return pc;
    }

    public PC createPC(String[] args) throws IOException {
        if (ArgProcessor.findVariable(args, "compile", "yes").equalsIgnoreCase("no")) {
            PC.compile = false;
        }
        PC pc = new PC(new VirtualClock(), args);
        loadNewPC(pc);

        String snapShot = ArgProcessor.findVariable(args, "ss", null);
        if (snapShot == null) {
            return pc;
        }
        File file = new File(snapShot);
        ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
        zin.getNextEntry();
        pc.loadState(zin);
        zin.closeEntry();
        ((PCMonitorFrame) objects.getObject(PCMonitorFrame.class)).resizeDisplay();
        zin.getNextEntry();
        ((PCMonitorFrame) objects.getObject(PCMonitorFrame.class)).loadMonitorState(zin);
        zin.closeEntry();
        zin.close();
        return pc;
    }

    class Hz extends JLabel implements ActionListener {

        DecimalFormat fmt;
        long lastCount, lastTime;

        Hz() {
            super("MHz = 0");
            fmt = new DecimalFormat("#.##");
            lastTime = System.currentTimeMillis();
            javax.swing.Timer timer = new javax.swing.Timer(1000, this);
            timer.setRepeats(true);
            timer.start();
        }

        public void actionPerformed(ActionEvent evt) {
            if (codeBlocks == null) {
                return;
            }
            long count = codeBlocks.getInstructionCount();
            long decoded = codeBlocks.getDecodedCount();
            long executed = codeBlocks.getExecutedBlockCount();
            long now = System.currentTimeMillis();

            double mhz = 1000.0 * (count - lastCount) / (now - lastTime) / 1000000;
            setText("Decoded: (" + decoded + " x86 Instr) | Executed: (" + commaSeparate(count) + " x86 Instr) (" + executed + " UBlocks) | " + fmt.format(mhz) + " MHz");
            lastCount = count;
            lastTime = now;
        }

        public String commaSeparate(long n) {
            String s = Long.toString(n);
            if (s.length() < 4) {
                return s;
            }
            StringBuffer buf = new StringBuffer();
            int offset = (s.length() % 3);
            if (offset == 0) {
                buf.append(s.substring(0, 3));
                offset = 3;
                for (int i = 0; i < (int) ((s.length() - 1) / 3); i++) {
                    buf.append(",");
                    buf.append(s.substring(offset + 3 * i, offset + 3 * i + 3));
                }
            } else {
                buf.append(s.substring(0, offset));
                for (int i = 0; i < (int) ((s.length() - 1) / 3); i++) {
                    buf.append(",");
                    buf.append(s.substring(offset + 3 * i, offset + 3 * i + 3));
                }
            }
            return buf.toString();
        }
    }

    public void createBlankHardDisk() {
        try {
            JFileChooser chooser = (JFileChooser) objects.getObject(JFileChooser.class);
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            String sizeString = JOptionPane.showInputDialog(this, "Enter the size in MB for the disk", "Disk Image Creation", JOptionPane.QUESTION_MESSAGE);
            if (sizeString == null) {
                return;
            }
            long size = Long.parseLong(sizeString) * 1024l * 1024l;
            if (size < 0) {
                throw new Exception("Negative file size");
            }
            RandomAccessFile f = new RandomAccessFile(chooser.getSelectedFile(), "rw");
            f.setLength(size);
            f.close();
        } catch (Exception e) {
            alert("Failed to create blank disk " + e, "Create Disk", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static Object getObject(Class cls) {
        return instance.get(cls);
    }

    public static JPC getInstance() {
        return instance;
    }

    public static void main(String[] args) throws IOException {
        initialise();

        boolean fullScreen = true;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("full")) {
                fullScreen = true;
                break;
            }
        }
        instance = new JPC(fullScreen);
        instance.validate();
        instance.setVisible(true);

        if (args.length > 0) {
            instance.createPC(args);
        }
    }
}
