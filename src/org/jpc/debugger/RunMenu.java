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

import java.awt.*;
import java.awt.event.*;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

import org.jpc.emulator.PC;
import org.jpc.emulator.processor.Processor;
import org.jpc.support.Clock;
import org.jpc.emulator.memory.codeblock.CodeBlock;

public class RunMenu extends JMenu implements ActionListener {

    private Runner runner;
    private JCheckBoxMenuItem background,  normalBlocks,  compiledBlocks,  cachedUBlocks,  riscBlocks,  pauseTimer;
    private JMenuItem run,  step,  reset,  breakRemote,  multiStep,  startRemote,  continueRemote,  compareRegisters,  adoptRegisters,  skip,  deleteRemoteBreaks,  smart,  smartstepbreak,  printa;
    private FunctionKeys functionKeys;
    private Processor processor;
    private CodeBlockRecord codeBlockRecord;
    private Clock clock;
    private int multiSteps;
    private RemoteGDB remote = null;
    private Processor cpu = null;
    public volatile int breakpointsLeft = 0;
    public BreakpointsFrame bpf;
    public WatchpointsFrame wpf;
    private DataInputStream uniqueIn = null;
    private Thread smartt = null;

    public RunMenu() {
        super("Run");

        run = add("Start");
        run.addActionListener(this);
        run.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, false));

        step = add("Single Step");
        step.addActionListener(this);
        step.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0, false));

        multiSteps = 10;
        multiStep = add("Multiple Steps (" + multiSteps + ")");
        multiStep.addActionListener(this);
        multiStep.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0, false));
        skip = add("Skip multiple breakpoints.");
        skip.addActionListener(this);
        smart = add("Smart continue and compare");
        smart.addActionListener(this);
        printa = add("Print out all addresses");
        printa.addActionListener(this);
        smartstepbreak = add("Smart step, break");
        smartstepbreak.addActionListener(this);

        addSeparator();
        background = new JCheckBoxMenuItem("Background Execution");
        add(background);
        background.setSelected(true);
        background.addActionListener(this);

        ButtonGroup gp = new ButtonGroup();
        gp.add(normalBlocks);
        gp.add(riscBlocks);
        gp.add(cachedUBlocks);
        gp.add(compiledBlocks);

        startRemote = add("Start remote PC and connect GDB");
        startRemote.addActionListener(this);
        breakRemote = add("Set remote breakpoint");
        breakRemote.addActionListener(this);
        continueRemote = add("Continue remote PC");
        continueRemote.addActionListener(this);
        continueRemote.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0, false));
        deleteRemoteBreaks = add("Delete remote breakpoints");
        deleteRemoteBreaks.addActionListener(this);
        compareRegisters = add("Compare remote registers");
        compareRegisters.addActionListener(this);
        compareRegisters.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0, false));
        adoptRegisters = add("Adopt remote registers");
        adoptRegisters.addActionListener(this);

        addSeparator();
        pauseTimer = new JCheckBoxMenuItem("Pause Virtual Clock");
        pauseTimer.setSelected(false);
        add(pauseTimer);

        addSeparator();
        reset = add("Reset - NI");
        reset.addActionListener(this);

        runner = new Runner();
        functionKeys = new FunctionKeys();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(functionKeys);
    }

    public void refresh() {
        processor = (Processor) JPC.getObject(Processor.class);
        codeBlockRecord = (CodeBlockRecord) JPC.getObject(CodeBlockRecord.class);

        PC pc = (PC) JPC.getObject(PC.class);
        if (pc != null) {
            clock = (Clock) pc.getComponent(Clock.class);
        }
        if (codeBlockRecord != null) {
            codeBlockRecord.advanceDecode();
        }
    }

    public void dispose() {
        stop();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(functionKeys);
    }

    class FunctionKeys implements KeyEventDispatcher {

        public boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (e.getKeyCode() == KeyEvent.VK_F8) {
                    executeStep(true);
                    return true;
                } else if (e.getKeyCode() == KeyEvent.VK_F7) {
                    executeSteps(multiSteps, true);
                    return true;
                }

                if (e.getKeyCode() == KeyEvent.VK_F2) {
                    JPC.getInstance().statusReport();
                }
            }

            return false;
        }
    }

    class Runner implements Runnable {

        volatile boolean running;
        Thread runnerThread;

        Runner() {
            running = false;
            runnerThread = null;
        }

        void startRunning() {
            if (running) {
                return;
            }
            if (runnerThread != null) {
                running = false;
                try {
                    runnerThread.join(1000);
                } catch (Exception e) {
                }

                try {
                    runnerThread.stop();
                } catch (Throwable t) {
                }
                runnerThread = null;
            }

            running = true;
            runnerThread = new Thread(this);
            runnerThread.setPriority(Thread.MIN_PRIORITY + 1);
            runnerThread.start();
        }

        void stopRunning() {
            running = false;
        }

        class U1 implements Runnable {

            public void run() {
                JPC.getInstance().refresh();
            }
        }

        class Alerter implements Runnable {

            int type;
            String title, message;

            Alerter(String title, String message, int type) {
                this.title = title;
                this.message = message;
                this.type = type;
            }

            public void show() {
                try {
                    SwingUtilities.invokeAndWait(this);
                } catch (Exception e) {
                }
            }

            public void run() {
                JOptionPane.showMessageDialog(JPC.getInstance(), message, title, type);
            }
        }

        public void run() {

            if (codeBlockRecord == null) {
                return;
            }
            PC pc = (PC) JPC.getObject(PC.class);
            pc.start();

            try {
                int delay = 2000;
                boolean bg = false;
                BreakpointsFrame bps = (BreakpointsFrame) JPC.getObject(BreakpointsFrame.class);
                WatchpointsFrame wps = (WatchpointsFrame) JPC.getObject(WatchpointsFrame.class);
                ExecutionTraceFrame trace = (ExecutionTraceFrame) JPC.getObject(ExecutionTraceFrame.class);
                U1 u = new U1();
                long t1 = System.currentTimeMillis();

                JPC.getInstance().notifyExecutionStarted();
                for (long c = 0; running; c++) {
                    bg = background.getState();

                    try {
                        if (codeBlockRecord == null) {
                            return;
                        }
                        if (pauseTimer.isSelected()) {
                            clock.pause();
                        } else {
                            clock.resume();
                        }
                        if (codeBlockRecord.executeBlock() == null) {
                            throw new Exception("Unimplemented Opcode. IP = " + Integer.toHexString(processor.getInstructionPointer()).toUpperCase());
                        }
                        CodeBlock nextBlock = codeBlockRecord.advanceDecode();

                        if (wps != null) {
                            WatchpointsFrame.Watchpoint wp = wps.checkForWatch();
                            if (wp != null) {
                                SwingUtilities.invokeAndWait(u);

                                String addr = MemoryViewPanel.zeroPadHex(wp.getAddress(), 8);
                                String name = wp.getName();
                                String value = Integer.toHexString(wp.getValue());
                                wp.updateValue();
                                String newvalue = Integer.toHexString(wp.getValue());
                                synchronized (this) {
                                    if (remote != null) {
                                        remote.setRemoteBreak("0x" + addr);
                                        remote.continueRemote();
                                        if (!remote.compareRegisters()) {
                                            throw new IllegalStateException("Different Registers before watchpoint");
                                        }
                                    }
                                    {
                                        new Alerter("Watchpoint", "Watch at " + addr + ": " + name +
                                                "\n old value: " + value + "\tnew value: " + newvalue, JOptionPane.INFORMATION_MESSAGE).show();
                                        break;
                                    }

                                }
                            }
                        }

                        if (bps != null) {
                            int blockStart = processor.getInstructionPointer();
                            int blockEnd = blockStart + 1;
                            if (nextBlock != null) {
                                blockEnd = blockStart + nextBlock.getX86Length();
                            }
                            Breakpoint bp = bps.checkForBreak(blockStart, blockEnd);
                            if (bp != null) {
                                SwingUtilities.invokeAndWait(u);

                                String addr = MemoryViewPanel.zeroPadHex(bp.getAddress(), 8);
                                String name = bp.getName();
                                synchronized (this) {
//                                    if (remote != null) {
//                                        remote.skipBreaks(1);
//                                        if (!remote.compareRegisters()) {
//                                            throw new IllegalStateException("Different Registers");
//                                        }
//                                    }
                                    if (breakpointsLeft > 0) {
                                        breakpointsLeft--;
                                        System.out.println("Break at " + addr + ": " + name + ". " + breakpointsLeft + " breakpoints left.");
                                    } else {
                                        if (remote != null) {
//                                            if (!remote.compareRegisters()) {
//                                                throw new IllegalStateException("Different Registers");
//                                            }
                                        }
                                        //new Alerter("Breakpoint", "Break at " + addr + ": " + name, JOptionPane.INFORMATION_MESSAGE).show();
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {

                        System.err.println("Exception @ 0x" + Integer.toHexString(processor.getInstructionPointer()));
                        e.printStackTrace();
                        new Alerter("Processor Exception", "Exception during execution: " + e, JOptionPane.ERROR_MESSAGE).show();
                        break;
                    }

                    if (bg) {
                        if ((c % 1000) != 0) {
                            continue;
                        }
                        long t2 = System.currentTimeMillis();
                        if (t2 - t1 < delay) {
                            continue;
                        }
                        t1 = t2;
                    }

                    try {
                        SwingUtilities.invokeAndWait(u);
                    } catch (Exception e) {
                    }
                }
            } finally {
                pc.start();
                running = false;
                runnerThread = null;
                run.setText("Start");
                step.setEnabled(true);

                JPC.getInstance().notifyExecutionStopped();
            }
        }
    }

    public void smartStepBreak() {
        while (true) {
            executeStep(false);
            
            int address = cpu.getInstructionPointer();
            System.out.println("Address: " + Integer.toHexString(address));
            remote.setRemoteTbreak("0x" + Integer.toHexString(address));
            remote.continueRemote();

            if (!remote.compareRegisters()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                //try again
                if (!remote.compareRegisters()) {
                    int adopt = JOptionPane.showConfirmDialog(JPC.getInstance(), "Adopt remote registers?", "Registers", JOptionPane.YES_NO_OPTION);
                    if (adopt == 0) {
                        remote.setRegisters();
                    } else {
                        break;
                    }
                }
            }
        }
    }

    public void smartContinueAndCompare() {
        try {
            File f = new File("QemuAddressesUnique.bin");
            if (uniqueIn == null) {
                uniqueIn = new DataInputStream(new FileInputStream(f));
            }
            if (bpf == null) {
                bpf = (BreakpointsFrame) JPC.getObject(BreakpointsFrame.class);
            }

            int previousAddress = -1;
            while (true) {
                int address = uniqueIn.readInt();
                if (address == previousAddress) {
                    while (address == previousAddress) {
                        address = uniqueIn.readInt();
                    }
                }
                System.out.println("Address: " + Integer.toHexString(address));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(RunMenu.class.getName()).log(Level.SEVERE, null, ex);
                }
                remote.setRemoteTbreak("0x" + Integer.toHexString(address));
                remote.continueRemote();
                previousAddress = address;

                if (address == (int) Long.parseLong("c0427ffb", 16)) {
                    int i = 1;
                }
                bpf.removeAllBreakpoints();
                bpf.setAddressBreakpoint(address);
                toggleRun();
                while (runner.running) {
                }
                if (!remote.compareRegisters()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    //try again
                    if (!remote.compareRegisters()) {
                        int adopt = JOptionPane.showConfirmDialog(JPC.getInstance(), "Adopt remote registers?", "Registers", JOptionPane.YES_NO_OPTION);
                        if (adopt == 0) {
                            remote.setRegisters();
                        } else {
                            break;
                        }
                    }
                }
            }
        } catch (EOFException e) {
        } catch (IOException f) {
            f.printStackTrace();
        }
    }

    public void executeStep(boolean doRemote) {
        executeSteps(1, doRemote);
    }

    public void executeSteps(int count, boolean doRemote) {
        if (codeBlockRecord == null) {
            return;
        }
        if (cpu == null) {
            cpu = (Processor) JPC.getObject(Processor.class);
        }
        PC pc = (PC) JPC.getObject(PC.class);
        pc.start();

        JPC.getInstance().notifyExecutionStarted();

        try {
            BreakpointsFrame bps = (BreakpointsFrame) JPC.getObject(BreakpointsFrame.class);
            if (pauseTimer.isSelected()) {
                clock.pause();
            } else {
                clock.resume();
            }
            for (int i = 0; i < count; i++) {
                if (i % 10 == 0) {
                    System.out.println("Done " + i + " blocks.");
                }
                CodeBlock block = codeBlockRecord.executeBlock();
//                while (cpu.eflagsInterruptEnable == false) {
//                    executeStep(false);
//                }
                if ((doRemote) && (remote != null)) {
                    System.out.println("Remote EIP: 0x" + Integer.toHexString(remote.executeRemoteInstructions(block.getX86Count())));
//                    while (remote.getInterruptEnableFlag() == false) {
//                        System.out.println("(Interrupt) Remote EIP: 0x" + Integer.toHexString(remote.executeRemoteInstructions(1)));
//                    }
                    remote.compareRegisters();
                }
                if (block == null) {
                    throw new Exception("Unimplemented Opcode at " + Integer.toHexString(processor.getInstructionPointer()).toUpperCase());
                }
                CodeBlock nextBlock = codeBlockRecord.advanceDecode();

                if (bps != null) {
                    int blockStart = processor.getInstructionPointer();
                    int blockEnd = blockStart + 1;
                    if (nextBlock != null) {
                        blockEnd = blockStart + nextBlock.getX86Length();
                    }
                    Breakpoint bp = bps.checkForBreak(blockStart, blockEnd);
                    if (bp != null) {
                        String addr = MemoryViewPanel.zeroPadHex(bp.getAddress(), 8);
                        String name = bp.getName();
                        JOptionPane.showMessageDialog(JPC.getInstance(), "Break at " + addr + ": " + name, "Breakpoint", JOptionPane.INFORMATION_MESSAGE);
                        break;
                    }
                }

                if ((count % 1000) == 999) {
                    JPC.getInstance().refresh();
                }
            }
        } catch (Exception e) {
            System.err.println("Exception @ 0x" + Integer.toHexString(processor.getInstructionPointer()));
            if (!(e instanceof IllegalStateException)) {
                e.printStackTrace();
            }
            JOptionPane.showMessageDialog(JPC.getInstance(), "Exception During Step:\n" + e, "Execute", JOptionPane.ERROR_MESSAGE);
        } finally {
            pc.stop();
            clock.pause();
            JPC.getInstance().notifyExecutionStopped();
        }
    }

    public void stop() {
        if (runner.running) {
            runner.stopRunning();
            run.setText("Start");
            step.setEnabled(true);
        }
    }

    public void toggleRun() {
        if (!runner.running) {
            runner.startRunning();
            run.setText("Stop");
            step.setEnabled(false);
        } else {
            runner.stopRunning();
            run.setText("Start");
            step.setEnabled(true);
        }

        JPC.getInstance().refresh();
    }

    public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();

        if (src == reset) {
            stop();
            try {
                PC pc = (PC) JPC.getObject(PC.class);
                if (pc != null) {
                    pc.reset();
                }
                JPC.getInstance().loadNewPC(pc);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to load BIOS: " + e, "PC Init Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (src == step) {
            executeStep(true);
        } else if (src == multiStep) {
            try {
                String val = JOptionPane.showInputDialog(JPC.getInstance(), "Enter the new number of steps to take each time", "" + multiSteps);
                multiSteps = Math.max(1, Integer.parseInt(val.trim()));
                multiStep.setText("Multiple Steps (" + multiSteps + ")");
            } catch (Exception e) {
            }
        } else if (src == run) {
            toggleRun();
        } else if (src == breakRemote) {
            stop();
            int size = 0x7c00;

            try {
                String val = JOptionPane.showInputDialog(JPC.getInstance(), "Enter breakpoint for remote machine\n", "JPC Debugger", JOptionPane.QUESTION_MESSAGE);
                if ((val != null) && (!val.isEmpty())) {
                    remote.setRemoteBreak(val);
                }
            } catch (Exception e) {
            }

//            int size = codeBlockRecord.getMaximumBlockSize();
//            try {
//                String val = JOptionPane.showInputDialog(JPC.getInstance(), "Enter the new code block size\n", "JPC Debugger", JOptionPane.QUESTION_MESSAGE);
//                size = Integer.parseInt(val);
//                codeBlockRecord.setMaximumBlockSize(size);
//            } catch (Exception e) {
//            }
//
//            JPC.getInstance().refresh();
        } else if (src == startRemote) {
            cpu = (Processor) JPC.getObject(Processor.class);
            remote = new RemoteGDB();
            remote.startAndConnect();
        } else if (src == continueRemote) {
            remote.continueRemote();
        } else if (src == compareRegisters) {
            if (!remote.compareRegisters()) {
                JOptionPane.showMessageDialog(JPC.getInstance(), "Registers diferent:\n" + remote.getDifferences(), "Execute", JOptionPane.ERROR_MESSAGE);
            }
        } else if (src == adoptRegisters) {
            remote.adoptRegisters();
        } else if (src == skip) {
            int nbps = Integer.parseInt(JOptionPane.showInputDialog(JPC.getInstance(), "Enter the new number of breakpoints to skip", "10"));
            synchronized (this) {
                breakpointsLeft = nbps;
            }
        } else if (src == deleteRemoteBreaks) {
            remote.deleteBreaks();
        } else if (src == smart) {
            //step remote pc along until we find a suitable break point for JPC
            smartt = new Thread(new Runnable() {

                public void run() {
                    smartContinueAndCompare();
                }
            });
            smartt.start();

//            int instr = Integer.parseInt(JOptionPane.showInputDialog(JPC.getInstance(), "Enter the minimum number instructions to skip", "1000"));
//            int breakpoint = remote.smartBreakSearch(instr);
//            if (bpf == null) {
//                bpf = (BreakpointsFrame) JPC.getObject(BreakpointsFrame.class);
//            }
//            bpf.removeAllBreakpoints();
//            bpf.setAddressBreakpoint(breakpoint);
//            toggleRun();
        } else if (src == smartstepbreak) {
            //step remote pc and break JPC at new address
            smartt = new Thread(new Runnable() {

                public void run() {
                    smartStepBreak();
                }
            });
            smartt.start();
        } else if (src == printa) {
            remote.printAddresses();
        }
    }
}
