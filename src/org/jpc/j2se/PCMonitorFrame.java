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

import java.text.DecimalFormat;
import java.awt.Dimension;
import java.awt.event.*;
import java.security.AccessControlException;
import java.util.logging.*;
import java.util.jar.*;
import java.net.URLClassLoader;
import javax.swing.*;
import java.io.*;
import java.net.URL;

import org.jpc.emulator.PC;
import org.jpc.support.ArgProcessor;

/**
 * 
 * @author Mike Moleschi
 */
public class PCMonitorFrame extends JFrame implements Runnable
{
    private static final Logger LOGGING = Logger.getLogger(PCMonitorFrame.class.getName());
    private static final DecimalFormat TWO_DP = new DecimalFormat("0.00");
    private static final DecimalFormat THREE_DP = new DecimalFormat("0.000");
    private static final int COUNTDOWN = 10000000;
    
    protected final PC pc;
    protected final PCMonitor monitor;
    
    private JScrollPane monitorPane;
    private final JProgressBar speedDisplay;
    private JFileChooser configFileChooser;

    private static final boolean updateMHz = true;
    private volatile boolean running;
    private Thread runner;

    public PCMonitorFrame(String title, PC pc, String[] args)
    {
        super(title);
        configFileChooser = new JFileChooser(System.getProperty("user.dir"));

        running = false;
        monitor = new PCMonitor(pc);
        monitorPane = new JScrollPane(monitor);
        getContentPane().add("Center", monitorPane);
        if (monitor != null)
            monitor.setFrame(monitorPane);
        
        this.pc = pc;
   

        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.add("Start").addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    start();
                }
            });
        file.add("Stop").addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    stop();
                }
            });
        file.add("Reset").addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    reset();
                }
            });
        file.addSeparator();
        file.add("Save Configuration").addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    saveConfig();
                }
            });
        file.add("Load Configuration").addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    loadConfig();
                }
            });
        file.addSeparator();
        file.add("Quit").addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    System.exit(0);
                }
            });

        bar.add(file);
            
        speedDisplay = new JProgressBar();
        speedDisplay.setStringPainted(true);
        speedDisplay.setString(" 0.00 Mhz");
        speedDisplay.setPreferredSize(new Dimension(100, 20));

        setJMenuBar(bar);
        
        getContentPane().add("South", speedDisplay);

        try
        {
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
        catch (AccessControlException e)
        {
            LOGGING.log(Level.WARNING, "Not able to add some components to frame.", e);
        }
    }
    
    public JScrollPane getMonitorPane()
    {
        return monitorPane;
    }

    private boolean updateMHz(long time, long count)
    {
        long t2 = System.currentTimeMillis();
        if (t2 - time < 100)
            return false;
        
        //count = COUNTDOWN - count;
        float mhz = count * 1000.0F / (t2 - time) / 1000000;

        speedDisplay.setValue((int)(mhz/1000));
        synchronized (TWO_DP) 
        {
            speedDisplay.setString(TWO_DP.format(mhz) + " MHz or " + THREE_DP.format(mhz/1000) + " GHz Clock");
        }
        return true;
    }

    protected synchronized void stop()
    {
        running = false;
        if ((runner != null) && runner.isAlive()) 
        {
            try 
            {
                runner.join(5000);
            } 
            catch (InterruptedException e) {}

            if (runner.isAlive())
            {
                try 
                {
                    runner.stop();
                } 
                catch (SecurityException e) {}
            }
        }
        
        runner = null;
        monitor.stopUpdateThread();
    }

    protected synchronized void start()
    {
        if (running)
            return;
        monitor.startUpdateThread();

        running = true;
        runner = new Thread(this, "PC Execute");
        runner.start();
    }

    protected synchronized boolean isRunning()
    {
        return running;
    }

    protected void saveConfig()
    {
        if (configFileChooser.showDialog(PCMonitorFrame.this, "Save JPC Configuration") == JFileChooser.APPROVE_OPTION)
        {
            try
            {
                Option.saveConfig(configFileChooser.getSelectedFile());
            }
            catch (IOException e)
            {
                LOGGING.log(Level.WARNING, "Exception saving configuration.", e);
            }
        }
    }

    protected void loadConfig()
    {
        if (configFileChooser.showDialog(PCMonitorFrame.this, "Load JPC Configuration") == JFileChooser.APPROVE_OPTION)
        {
            try
            {
                Option.loadConfig(configFileChooser.getSelectedFile());
            }
            catch (IOException e)
            {
                LOGGING.log(Level.WARNING, "Exception saving configuration.", e);
            }
        }
    }

    protected void reset()
    {
        stop();
        pc.reset();
        start();
    }

    public void run()
    {
        pc.start();
        try 
        {
            long markTime = System.currentTimeMillis();
            long execCount = COUNTDOWN;
            long totalExec = 0;
            while (running) 
            {
                execCount -= pc.execute();
                if (execCount > 0)
                    continue;
                totalExec += (COUNTDOWN - execCount);
                execCount = COUNTDOWN;

                if (updateMHz)
                    if (updateMHz(markTime, totalExec))
                    {
                        markTime = System.currentTimeMillis();
                        totalExec = 0;
                    }
            }
        } 
        finally 
        {
            pc.stop();
            LOGGING.log(Level.INFO, "PC Stopped");
        }
    }

    public static void main(String[] args) throws Exception
    {
        //JPCStatisticsMonitor.install();
        Option.parse(args);

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        args = Option.parse(args);
        if (args.length == 0) {
            ClassLoader cl = JPCApplication.class.getClassLoader();
            if (cl instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader)cl).getURLs()) {
                    InputStream in = url.openStream();
                    try {
                        JarInputStream jar = new JarInputStream(in);
                        Manifest manifest = jar.getManifest();
                        if (manifest == null)
                            continue;
                        
                        String defaultArgs = manifest.getMainAttributes().getValue("Default-Args");
                        if (defaultArgs == null)
                            continue;
                                                
                        args = defaultArgs.split("\\s");
                        break;
                    } catch (IOException e) {
                        System.err.println("Not a JAR file " + url);
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
            
            if (args.length == 0) {
                LOGGING.log(Level.INFO, "No configuration specified, using defaults");
                args = new String[]{"-fda", "mem:resources/images/floppy.img",
                                    "-hda", "mem:resources/images/dosgames.img", "-boot", "fda"
                };
            } else {
                LOGGING.log(Level.INFO, "Using configuration specified in manifest");
            }
        } else {
            LOGGING.log(Level.INFO, "Using configuration specified on command line");
        }

        PC pc = new PC(new VirtualClock(), args);
        
        PCMonitorFrame result = new PCMonitorFrame("JPC Monitor", pc, args);
        result.validate();
        result.setVisible(true);
        result.setBounds(100, 100, 760, 500);
        result.start();
    }
}

