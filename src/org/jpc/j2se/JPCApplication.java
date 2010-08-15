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

package org.jpc.j2se;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;
import java.util.zip.*;

import javax.swing.*;

import org.jpc.emulator.PC;
import org.jpc.emulator.pci.peripheral.EthernetCard;
import org.jpc.emulator.pci.peripheral.VGACard;
import org.jpc.support.*;

public class JPCApplication extends PCMonitorFrame implements PCControl
{
    private static final Logger LOGGING = Logger.getLogger(JPCApplication.class.getName());
    private static final URI JPC_URI = URI.create("http://jpc.sourceforge.net/");
    private static final String IMAGES_PATH = "resources/images/";
    private static final int MONITOR_WIDTH = 720;
    private static final int MONITOR_HEIGHT = 400 + 100;
    private static final String[] DEFAULT_ARGS =
    {
        "-fda", "mem:resources/images/floppy.img",
        "-hda", "mem:resources/images/dosgames.img",
        "-boot", "fda"
    };
    private static final String ABOUT_US =
            "JPC: Developed since August 2005 in Oxford University's Subdepartment of Particle Physics.\n\n" +
            "For more information visit our website at:\n" + JPC_URI.toASCIIString();
    private static final String LICENCE_HTML =
            "JPC is released under GPL Version 2 and comes with absoutely no warranty<br/><br/>" +
            "See " + JPC_URI.toASCIIString() + " for more details";
    private static JEditorPane LICENCE;
    
    static
    {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        URL licence = context.getResource("resources/licence.html");
        if (licence != null)
        {
            try
            {
                LICENCE = new JEditorPane(licence);
            } catch (IOException e)
            {
                LICENCE = new JEditorPane("text/html", LICENCE_HTML);
            }
        } else
        {
            LICENCE = new JEditorPane("text/html", LICENCE_HTML);
        }
        LICENCE.setEditable(false);
    }
    private KeyTypingPanel keys;
    private JFileChooser diskImageChooser;
    private JFileChooser snapshotFileChooser;

    public JPCApplication(String[] args, PC pc) throws Exception
    {
        super("JPC - " + ArgProcessor.findVariable(args, "hda", null), pc, args);
        diskImageChooser = new JFileChooser(System.getProperty("user.dir"));
        snapshotFileChooser = new JFileChooser(System.getProperty("user.dir"));

        String snapShot = ArgProcessor.findVariable(args, "ss", null);
        if (snapShot != null)
            loadSnapshot(new File(snapShot));
        JMenuBar bar = getJMenuBar();

        JMenu snap = new JMenu("Snapshot");
        snap.add("Save Snapshot").addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ev)
            {
                stop();

                if (snapshotFileChooser.showDialog(JPCApplication.this, "Save JPC Snapshot") == JFileChooser.APPROVE_OPTION)
                {
                    try
                    {
                        saveSnapshot(snapshotFileChooser.getSelectedFile());
                    } 
                    catch (IOException e)
                    {
                        LOGGING.log(Level.WARNING, "Exception saving snapshot.", e);
                    }
                }
                start();
            }
        });
        snap.add("Load Snapshot").addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent ev)
            {
                int cancel = JOptionPane.showOptionDialog(JPCApplication.this, "Selecting a snapshot now will discard the current state of the emulated PC. Are you sure you want to continue?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[]
                        {
                            "Continue", "Cancel"
                        }, "Continue");

                if (cancel == 0)
                {
                    stop();

                    if (snapshotFileChooser.showDialog(JPCApplication.this, "Load Snapshot") == JFileChooser.APPROVE_OPTION)
                    {
                        try
                        {
                            loadSnapshot(snapshotFileChooser.getSelectedFile());
                        } catch (IOException e)
                        {
                            LOGGING.log(Level.SEVERE, "Exception during snapshot load", e);
                        }
                    }

                    monitor.revalidate();
                    monitor.requestFocus();
                }
            }
        });
        snap.add("Start saving compiled classes").addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent ev)
            {
                org.jpc.emulator.memory.codeblock.fastcompiler.ClassFileBuilder.startSavingClasses(new File(System.getProperty("user.dir") + "/classy.jar"));
            }
        });
        snap.add("Finish saving compiled classes").addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent ev)
            {
                org.jpc.emulator.memory.codeblock.fastcompiler.ClassFileBuilder.finishSavingClasses();
            }
        });
        bar.add(snap);

        DriveSet drives = (DriveSet) pc.getComponent(DriveSet.class);

        JMenu disks = new JMenu("Disks");

        disks.add("Create disk").addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent ev)
            {
                createBlankDisk();
            }
        });

        disks.add("Create disk from directory").addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent ev)
            {
                createDiskFromDirectory();
            }
        });

        for (int i = 0; i < 2; i++)
        {
            BlockDevice drive = drives.getFloppyDrive(i);

            JMenu top = new JMenu();
            if (drive == null)
                top.setText("FD" + i + " [none]");
            else
                top.setText("FD" + i + " " + drive.toString());

            JMenu included = new JMenu("Included Images...");
            JMenuItem file = new JMenuItem("Choose Image...");

            ActionListener handler = new FloppyDriveChangeHandler(i, top, included, file);
            Iterator<String> itt = getResources(IMAGES_PATH);
            while (itt.hasNext())
            {
                String path = (String) itt.next();
                if (path.startsWith(IMAGES_PATH))
                    path = path.substring(IMAGES_PATH.length());
                included.add(path).addActionListener(handler);
            }
            top.add(included);
            top.add(file).addActionListener(handler);
            disks.add(top);
        }

        for (int i = 0; i < 4; i++)
        {
            BlockDevice drive = drives.getHardDrive(i);

            JMenu top = new JMenu();
            if (drive == null)
                top.setText("HD" + i + " [none]");
            else
                top.setText("HD" + i + " " + drive.toString());

            JMenu included = new JMenu("Included Images...");
            JMenuItem file = new JMenuItem("Choose Image...");
            JMenuItem directory = new JMenuItem("Choose Directory...");

            ActionListener handler = new HardDriveChangeHandler(i, top, included, file, directory);
            Iterator<String> itt = getResources(IMAGES_PATH);
            while (itt.hasNext())
            {
                String path = (String) itt.next();
                if (path.startsWith(IMAGES_PATH))
                    path = path.substring(IMAGES_PATH.length());
                included.add(path).addActionListener(handler);
            }
            top.add(included);
            top.add(file).addActionListener(handler);
            top.add(directory).addActionListener(handler);
            disks.add(top);
        }
        bar.add(disks);

        JMenu help = new JMenu("Help");
        help.add("Getting Started").addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent evt)
            {
                JFrame help = new JFrame("JPC - Getting Started");
                help.setIconImage(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("resources/icon.png")));
                help.getContentPane().add("Center", new JScrollPane(LICENCE));
                help.setBounds(300, 200, MONITOR_WIDTH + 20, MONITOR_HEIGHT - 70);
                help.setVisible(true);
                getContentPane().validate();
                getContentPane().repaint();
            }
        });
        help.add("About JPC").addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent evt)
            {
                Object[] buttons =
                {
                    "Visit our Website", "Ok"
                };
                if (JOptionPane.showOptionDialog(JPCApplication.this, ABOUT_US, "About JPC", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, buttons, buttons[1]) == 0)
                {
                    if (Desktop.isDesktopSupported())
                    {
                        try
                        {
                            Desktop.getDesktop().browse(JPC_URI);
                        } catch (IOException e)
                        {
                            LOGGING.log(Level.INFO, "Couldn't find or launch the default browser.", e);
                        } catch (UnsupportedOperationException e)
                        {
                            LOGGING.log(Level.INFO, "Browse action not supported.", e);
                        } catch (SecurityException e)
                        {
                            LOGGING.log(Level.INFO, "Browse action not permitted.", e);
                        }
                    }
                }
            }
        });
        bar.add(help);

        keys = new KeyTypingPanel(monitor);
        JPCApplet.PlayPausePanel pp = new JPCApplet.PlayPausePanel(this);

        JPanel p1 = new JPanel(new BorderLayout(10, 10));
        p1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        p1.add("Center", keys);
        p1.add("East", pp);              
        getContentPane().add("South", p1); 

        setSize(monitor.getPreferredSize());
        LICENCE.setPreferredSize(monitor.getPreferredSize());
        getMonitorPane().setViewportView(LICENCE);
        getContentPane().validate();
    }

    public void setSize(Dimension d)
    {
        super.setSize(new Dimension(monitor.getPreferredSize().width, d.height + keys.getPreferredSize().height + 60));
        getMonitorPane().setPreferredSize(new Dimension(monitor.getPreferredSize().width + 2, monitor.getPreferredSize().height + 2));
    }

    public synchronized void start()
    {
        super.start();

        getMonitorPane().setViewportView(monitor);
        monitor.validate();
        monitor.requestFocus();
    }

    public synchronized void stop()
    {
        super.stop();
    }

    public synchronized boolean isRunning()
    {
        return super.isRunning();
    }

    private void loadSnapshot(File file) throws IOException
    {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(file));
        zin.getNextEntry();
        pc.loadState(zin);
        zin.closeEntry();
        VGACard card = ((VGACard) pc.getComponent(VGACard.class));
        card.setOriginalDisplaySize();
        zin.getNextEntry();
        monitor.loadState(zin);
        zin.closeEntry();
        zin.close();
    }

    private void saveSnapshot(File file) throws IOException
    {
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file));

        zip.putNextEntry(new ZipEntry("pc"));
        pc.saveState(zip);
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry("monitor"));
        monitor.saveState(zip);
        zip.closeEntry();

        zip.finish();
        zip.close();
    }

    private void createBlankDisk()
    {
        try {
            JFileChooser chooser = diskImageChooser;
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
            JOptionPane.showMessageDialog(rootPane, "Failed to create blank disk " + e, "Create Disk", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createDiskFromDirectory()
    {
        try {
            JFileChooser chooser = diskImageChooser;
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            // select directory to make drive from
            JFileChooser directory = new JFileChooser(System.getProperty("user.dir"));
            directory.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (directory.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File out = chooser.getSelectedFile();
            File root = directory.getSelectedFile();
            if (!out.exists())
                out.createNewFile();
            TreeBlockDevice tbd = new TreeBlockDevice(root, true);
            DataOutput dataout = new DataOutputStream(new FileOutputStream(out));
            tbd.writeImage(dataout);
            System.out.println("Done saving disk image");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(rootPane, "Failed to create disk from directory" + e, "Create Disk", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class FloppyDriveChangeHandler implements ActionListener
    {
        private int driveIndex;
        private JMenu included;
        private JMenuItem file;
        private JMenuItem top;

        public FloppyDriveChangeHandler(int index, JMenuItem top, JMenu included, JMenuItem file)
        {
            driveIndex = index;
            this.included = included;
            this.file = file;
            this.top = top;
        }

        public void actionPerformed(ActionEvent e)
        {
            Component source = (Component) e.getSource();
            if (included.isMenuComponent(source))
                change(IMAGES_PATH + ((JMenuItem) source).getText());
            else if (source == file)
            {
                int result = diskImageChooser.showDialog(JPCApplication.this, "Load FD" + driveIndex + " Image");
                if (result != JFileChooser.APPROVE_OPTION)
                    return;
                change(diskImageChooser.getSelectedFile());
            }
        }

        private void change(File image)
        {
            try
            {
                change(new FileBackedSeekableIODevice(image.getAbsolutePath()));
            } 
            catch (IOException e)
            {
                LOGGING.log(Level.INFO, "Exception changing floppy disk.", e);
            }
        }

        private void change(String resource)
        {
            try
            {
                change(new ArrayBackedSeekableIODevice(resource));
            } 
            catch (IOException e)
            {
                LOGGING.log(Level.INFO, "Exception changing floppy disk.", e);
            }
        }

        private void change(SeekableIODevice device)
        {
            BlockDevice bd = new FloppyBlockDevice(device);
            pc.changeFloppyDisk(bd, driveIndex);
            top.setText("FD" + driveIndex + " " + bd.toString());
        }
    }

    private class HardDriveChangeHandler implements ActionListener
    {
        private int driveIndex;
        private JMenu included;
        private JMenuItem file;
        private JMenuItem directory;
        private JMenu top;

        public HardDriveChangeHandler(int index, JMenu top, JMenu included, JMenuItem file, JMenuItem directory)
        {
            driveIndex = index;
            this.included = included;
            this.file = file;
            this.directory = directory;
            this.top = top;
        }

        public void actionPerformed(ActionEvent e)
        {
            Component source = (Component) e.getSource();
            if (included.isMenuComponent(source))
            {
                stop();
                change(IMAGES_PATH + ((JMenuItem) source).getText());
                reset();
                monitor.revalidate();
                monitor.requestFocus();
            } 
            else if (source == file)
            {
                stop();
                diskImageChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                int result = diskImageChooser.showDialog(JPCApplication.this, "Load HD" + driveIndex + " Image");
                if (result != JFileChooser.APPROVE_OPTION)
                    return;
                changeFile(diskImageChooser.getSelectedFile());
                reset();
                monitor.revalidate();
                monitor.requestFocus();
            } 
            else if (source == directory)
            {
                stop();
                diskImageChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = diskImageChooser.showDialog(JPCApplication.this, "Select HD" + driveIndex + " Directory");
                if (result != JFileChooser.APPROVE_OPTION)
                    return;
                changeDirectory(diskImageChooser.getSelectedFile());
                reset();
                monitor.revalidate();
                monitor.requestFocus();
            }
        }

        private void changeFile(File image)
        {
            try
            {
                SeekableIODevice ioDevice = new FileBackedSeekableIODevice(image.getAbsolutePath());
                change(new HDBlockDevice(ioDevice));
            } 
            catch (IOException e)
            {
                LOGGING.log(Level.INFO, "Exception changing floppy disk.", e);
            }
        }

        private void changeDirectory(File directory)
        {
            try
            {
                change(new TreeBlockDevice(directory, false));
            } 
            catch (IOException e)
            {
                LOGGING.log(Level.INFO, "Exception changing floppy disk.", e);
            }
        }

        private void change(String resource)
        {
            try
            {
                SeekableIODevice ioDevice = new ArrayBackedSeekableIODevice(resource);
                change(new HDBlockDevice(ioDevice));
            } 
            catch (IOException e)
            {
                LOGGING.log(Level.INFO, "Exception changing floppy disk.", e);
            }
        }

        private void change(BlockDevice device)
        {
            DriveSet drives = (DriveSet) pc.getComponent(DriveSet.class);
            drives.setHardDrive(driveIndex, device);
            top.setText("HD" + driveIndex + " " + device.toString());
        }
    }

    private static final Iterator<String> getResources(String directory)
    {
        ClassLoader context = Thread.currentThread().getContextClassLoader();

        List<String> resources = new ArrayList<String>();

        ClassLoader cl = JPCApplication.class.getClassLoader();
        if (!(cl instanceof URLClassLoader))
            throw new IllegalStateException();
        URL[] urls = ((URLClassLoader) cl).getURLs();
        
        int slash = directory.lastIndexOf("/");
        String dir = directory.substring(0, slash + 1);
        for (int i=0; i<urls.length; i++)
        {
            if (!urls[i].toString().endsWith(".jar"))
                continue;
            try
            {
                JarInputStream jarStream = new JarInputStream(urls[i].openStream());
                while (true)
                {
                    ZipEntry entry = jarStream.getNextEntry();
                    if (entry == null)
                        break;
                    if (entry.isDirectory())
                        continue;

                    String name = entry.getName();
                    slash = name.lastIndexOf("/");
                    String thisDir = "";
                    if (slash >= 0)
                        thisDir = name.substring(0, slash + 1);

                    if (!dir.equals(thisDir))
                        continue;
                    resources.add(name);
                }

                jarStream.close();
            }
            catch (IOException e) { e.printStackTrace();}
        }
        InputStream stream = context.getResourceAsStream(directory);
        try
        {
            if (stream != null)
            {
                Reader r = new InputStreamReader(stream);
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                try
                {
                    while (true)
                    {
                        int length = r.read(buffer);
                        if (length < 0)
                        {
                            break;
                        }
                        sb.append(buffer, 0, length);
                    }
                } finally
                {
                    r.close();
                }

                for (String s : sb.toString().split("\n"))
                {
                    if (context.getResource(directory + s) != null)
                    {
                        resources.add(s);
                    }
                }
            }
        }
        catch (IOException e)
        {
            LOGGING.log(Level.INFO, "Exception reading images directory stream", e);
        }

        return resources.iterator();
    }

    public static void main(String[] args) throws Exception
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e)
        {
            LOGGING.log(Level.INFO, "System Look-and-Feel not loaded", e);
        }

        if (args.length == 0)
        {
            ClassLoader cl = JPCApplication.class.getClassLoader();
            if (cl instanceof URLClassLoader)
            {
                for (URL url : ((URLClassLoader) cl).getURLs())
                {
                    InputStream in = url.openStream();
                    try
                    {
                        JarInputStream jar = new JarInputStream(in);
                        Manifest manifest = jar.getManifest();
                        if (manifest == null)
                        {
                            continue;
                        }
                        String defaultArgs = manifest.getMainAttributes().getValue("Default-Args");
                        if (defaultArgs == null)
                        {
                            continue;
                        }
                        args = defaultArgs.split("\\s");
                        break;
                    } 
                    catch (IOException e)
                    {
                        System.err.println("Not a JAR file " + url);
                    } 
                    finally
                    {
                        try
                        {
                            in.close();
                        } catch (IOException e) {}
                    }
                }
            }

            if (args.length == 0)
            {
                LOGGING.log(Level.INFO, "No configuration specified, using defaults");
                args = DEFAULT_ARGS;
            } 
            else
            {
                LOGGING.log(Level.INFO, "Using configuration specified in manifest");
            }
        } 
        else
        {
            LOGGING.log(Level.INFO, "Using configuration specified on command line");
        }

        if (ArgProcessor.findVariable(args, "compile", "yes").equalsIgnoreCase("no"))
            PC.compile = false;

        String memarg = ArgProcessor.findVariable(args, "m", null);

        PC pc;
        if (memarg == null)
            pc = new PC(new VirtualClock(), args);
        else
        {
            int mem;
            if (memarg.endsWith("G") || memarg.endsWith("g"))
                mem = Integer.parseInt(memarg.substring(0, memarg.length()-1))*1024*1024*1024;
            else if (memarg.endsWith("M") || memarg.endsWith("m"))
                mem = Integer.parseInt(memarg.substring(0, memarg.length()-1))*1024*1024;
            else if (memarg.endsWith("K") || memarg.endsWith("k"))
                mem = Integer.parseInt(memarg.substring(0, memarg.length()-1))*1024;
            else
                mem = Integer.parseInt(memarg.substring(0, memarg.length()));
            pc = new PC(new VirtualClock(), args, mem);
        }

        String net = ArgProcessor.findVariable(args, "net", "no");
        if (net.startsWith("hub:"))
        {
            int port = 80;
            String server;
            int index = net.indexOf(":", 5); 
            if (index != -1) {
                port = Integer.parseInt(net.substring(index+1));
                server = net.substring(4, index);
            }
            else
                server = net.substring(4);
            EthernetOutput hub = new EthernetHub(server, port);
            EthernetCard card = (EthernetCard) pc.getComponent(EthernetCard.class);
            card.setOutputDevice(hub);
        }
        final JPCApplication app = new JPCApplication(args, pc);

        app.setBounds(100, 100, MONITOR_WIDTH + 20, MONITOR_HEIGHT + 70);
        try
        {
            app.setIconImage(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("resources/icon.png")));
        } catch (Exception e) {}

        app.validate();
        app.setVisible(true);
    }
}
