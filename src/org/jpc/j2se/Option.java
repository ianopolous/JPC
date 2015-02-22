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

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public abstract class Option {
    private static final Hashtable<String, Option> names2options = new Hashtable();

    public static final Opt config = opt("config"); // This one is special...

    public static final Switch track_writes = createSwitch("track-writes"); // needed to use -mem with compare
    public static final Switch debug_blocks = createSwitch("debug-blocks");
    public static final Switch log_disam = createSwitch("log-disam");
    public static final Switch log_disam_addresses = createSwitch("log-disam-addresses");
    public static final Switch log_state = createSwitch("log-state");
    public static final Switch log_blockentry = createSwitch("log-block-entry");
    public static final Switch log_memory_maps = createSwitch("log-memory-maps");
    public static final Switch compile = createSwitch("compile");
    public static final Switch fullscreen = createSwitch("fullscreen");
    public static final Switch history = createSwitch("history");
    public static final Switch useBochs = createSwitch("bochs");

    public static final Switch printCHS = createSwitch("printCHS");
    public static final Switch help = createSwitch("help");
    public static final Opt min_addr_watch = opt("min-addr-watch");
    public static final Opt max_addr_watch = opt("max-addr-watch");

    // required for deterministic execution
    public static final Switch deterministic = createSwitch("deterministic");
    public static final Opt startTime = opt("start-time");
    public static final Switch noScreen = createSwitch("no-screen");

    public static final Opt ss = opt("ss");
    public static final Opt ram = opt("ram");
    public static final Opt ips = opt("ips");
    public static final Opt cpulevel = opt("cpulevel");
    public static final Opt timeslowdown = opt("time-slowdown");
    public static final Switch singlesteptime = createSwitch("single-step-time");
    public static final Opt max_instructions_per_block = opt("max-block-size");
    public static final Opt boot = opt("boot");
    public static final Opt fda = opt("fda");
    public static final Opt fdb = opt("fdb");
    public static final Opt hda = opt("hda");
    public static final Opt hdb = opt("hdb");
    public static final Opt hdc = opt("hdc");
    public static final Opt hdd = opt("hdd");
    public static final Opt cdrom = opt("cdrom");
    public static final Opt bios = opt("bios");
    public static final Switch ethernet = createSwitch("ethernet");

    public static final Switch sound = createSwitch("sound");
    public static final Opt sounddevice = opt("sounddevice");
    public static final Opt mixer_rate = opt("mixer_rate");
    public static final Opt mixer_javabuffer = opt("mixer_java-buffer");
    public static final Opt mixer_blocksize = opt("mixer_block-size");
    public static final Switch mixer_nosound = createSwitch("mixer_no-sound");
    public static final Opt mixer_prebuffer = opt("mixer_prebuffer");

    public static final Opt mpu401 = opt("mpu401");
    public static final Opt mididevice = opt("midi-device");
    public static final Opt midiconfig = opt("midi-config");

    public static final Opt sbbase = opt("sbbase");
    public static final Opt sb_irq = opt("sb_irq");
    public static final Opt sb_dma = opt("sb_dma");
    public static final Opt sb_hdma = opt("sb_hdma");
    public static final Switch sbmixer = createSwitch("sbmixer");
    public static final Opt sbtype = opt("sbtype");
    public static final Opt oplemu = opt("oplemu");
    public static final Opt oplrate = opt("oplrate");

    public static void printHelp() {
        System.out.println("JPC Help");
        System.out.println("Parameters may be specified on the command line or in a file. ");
        System.out.println();
        System.out.println("-help - display this help");
        System.out.println("-config $file - read parameters from $file, any subsequent commandline parameters override parameters in the file");
        System.out.println("-boot $device - the device to boot from out of fda (floppy), hda (hard drive 1), cdrom (CDROM drive)");
        System.out.println("-fda $file - floppy image file");
        System.out.println("-hda $file - hard disk image file");
        System.out.println("-hda dir:$dir - directory to mount as a FAT32 hard disk");
        System.out.println("-ss $file - snapshot file to load");
        System.out.println("-ram $megabytes - the amount RAM the virtual machine should have");
        System.out.println("-ips $number - number of emulated instructions per emulated second - a larger value will cause a slower apparent time in the VM");
        System.out.println("-cpulevel $number - 4 = 486, 5 = Pentium, 6 = Pentium Pro");
        System.out.println();
        System.out.println("-sound - enable sound");
        System.out.println();
        System.out.println("Advanced Options:");
        System.out.println("-bios - specify an alternate bios image");
        System.out.println("-max-block-size $num - maximum number of instructions per basic block (A value of 1 will still have some blocks of length 2 due to mov ss,X, pop ss and sti)");
    }

    public static String[] parse(String[] source) {
        ArrayList<String> tmp = new ArrayList<String>();
        for (Iterator<Option> iterator = names2options.values().iterator(); iterator.hasNext(); ) {
            Option next = iterator.next();
            next.set = false;
        }
        int index = 0;
        for (; index < source.length; index++) {
            String arg = source[index];
            if (arg.startsWith("-")) {
                arg = arg.substring(1);
            }
            Option opt = names2options.get(arg);
            if (opt == null) {
                tmp.add(source[index]);
            } else if ((opt == config) && (config.value() != null))
            {
                // exit recursion
                opt.set = false;
                index = opt.update(source, index);
            }
            else {
                opt.set = true;
                index = opt.update(source, index);
            }
        }
        if (config.isSet())
            return loadConfig(config.value());
        if (config.value() != null)
            ((Option) config).set = true;
        if (tmp.size() == source.length) {
            return source;
        } else {
            return tmp.toArray(new String[tmp.size()]);
        }
    }

    public static void saveConfig(File f) throws IOException
    {
        String conf = saveConfig();
        BufferedWriter w = new BufferedWriter(new FileWriter(f));
        w.write(conf);
        w.flush();
        w.close();
    }

    public static String[] loadConfig(String file)
    {
        try {
            return loadConfig(new File(file));
        } catch (IOException e)
        {
            System.out.println("Error loading config from file "+file);
        }
        return null;
    }

    public static String[] loadConfig(File f) throws IOException
    {
        StringBuilder b = new StringBuilder();
        BufferedReader r = new BufferedReader(new FileReader(f));
        String line;
        while ((line = r.readLine()) != null)
        {
            b.append(line+" ");
        }
        String[] current = saveConfig().split("\n");
        for (String s: current)
            b.append(s + " ");
        return parse(b.toString().split(" "));
    }

    public static String saveConfig()
    {
        StringBuilder b = new StringBuilder();
        for (Option opt : names2options.values())
        {
            if (opt instanceof Switch)
            {
                if (opt.isSet())
                    b.append("-"+opt.getName()+"\n");
            }
            else if (opt instanceof Opt)
            {
                if (opt.isSet())
                    b.append("-"+opt.getName()+" "+((Opt) opt).value()+"\n");
            }
        }
        return b.toString();
    }

    public static Option getParameter(String name)
    {
        return names2options.get(name);
    }

    public static Switch createSwitch(String name) {
        Switch sw = (Switch) names2options.get(name);
        if (sw == null) {
            sw = new Switch(name);
        }
        return sw;
    }

    public static Opt opt(String name) {
        Opt opt = (Opt) names2options.get(name);
        if (opt == null) {
            opt = new Opt(name);
        }
        return opt;
    }
    public static OptSet optSet(String name) {
        OptSet opt = (OptSet) names2options.get(name);
        if (opt == null) {
            opt = new OptSet(name);
        }
        return opt;
    }
    public static Select select(String name) {
        return select(name,"default");
    }
    public static Select select(String name,String defaultKey) {
        Select opt = (Select) names2options.get(name);
        if (opt == null) {
            opt = new Select(name,defaultKey);
        }
        return opt;
    }


    private final String name;
    private boolean set;

    protected Option(String name) {
        this.name = name;
        names2options.put(name, this);
    }

    public String getName() {
        return name;
    }

    public boolean isSet() {
        return set;
    }

    public abstract Object getValue();

    protected abstract int update(String[] args, int index);

    public Object getInstance() {
        return getInstance(null);
    }
    public Object getInstance(String defaultClass) {
        Object o = getValue();
        Class clazz = null;
        try {
            if (o instanceof Class) {
                clazz = (Class) o;
            } else if (o instanceof String) {
                clazz = Class.forName(o.toString());
            } else if (defaultClass!=null) {
                clazz=Class.forName(defaultClass);
            } else {
                return null;
            }
            return clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class OptSet extends Option {
        private Collection<String> values = new LinkedHashSet<String>();

        public OptSet(String name, String... defaults) {
            super(name);
            for (int i = 0; i < defaults.length; i++) {
                String s = defaults[i];
                values.add(s);
            }
        }

        @Override
        public Object getValue() {
            return values.toArray(new String[values.size()]);
        }

        public String[] values() {
            return (String[]) getValue();
        }

        @Override
        protected int update(String[] args, int index) {
            String value = args[++index];
            values.add(value);
            return index;
        }

        public void remove(String value) {
            values.remove(value);
        }
    }

    public static class Opt extends Option {
        private String value;

        public Opt(String name) {
            super(name);
        }

        @Override
        public int update(String[] args, int index) {
            this.value = args[++index];
            return index;
        }

        @Override
        public Object getValue() {
            return value;
        }

        public int intValue(int defaultValue) {
            if (value != null) {
                return Integer.parseInt(value.trim());
            } else {
                return defaultValue;
            }
        }

        public int intValue(int defaultValue, int radix) {
            if (value != null) {
                return (int) Long.parseLong(value.trim(), radix);
            } else {
                return defaultValue;
            }
        }


        public long longValue(long defaultValue, int radix) {
            if (value != null) {
                return Long.parseLong(value.trim(), radix);
            } else {
                return defaultValue;
            }
        }

        public double doubleValue(double defaultValue) {
            if (value != null) {
                return Double.parseDouble(value.trim());
            } else {
                return defaultValue;
            }
        }

        public String value(String defaultValue) {
            if (value != null) {
                return value;
            } else {
                return defaultValue;
            }
        }

        public String value() {
            return value;
        }

        public Object valueOf(Class type, Object defaultValue) {
            if (value == null) return defaultValue;
            Throwable t = null;
            try {
                try {
                    Method valueOf = type.getMethod("valueOf", String.class);
                    return valueOf.invoke(type, value);
                } catch (NoSuchMethodException e) {
                    System.err.println(type + " :No suitable method");
                }
                try {
                    Constructor constructor = type.getConstructor(String.class);
                    return constructor.newInstance(value);
                } catch (NoSuchMethodException e) {
                    System.err.println(type + " :No suitable Constructor");
                }
            } catch (Exception e) {
                throw new RuntimeException("Nested Exception" + type, e);
            }
            throw new RuntimeException("Unable obtain value of " + type);
        }


        public Object instance(String defaultClassName) {
            if (value == null) {
                value = defaultClassName;
            }
            try {
                return Class.forName(value.trim()).newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException("Nested Exception", e);

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Nested Exception", e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Nested Exception", e);
            }
        }
    }

    public static class Switch extends Option {
        private boolean value;

        public Switch(String name) {
            super(name);
        }

        public boolean value() {
            return value;
        }

        @Override
        public Object getValue() {
            return Boolean.valueOf(value);
        }

        @Override
        public int update(String[] args, int index) {
            value = true;
            return index; // No Arguments
        }
    }

    public static class Select extends Option {

        private Map<String, Object> values = new LinkedHashMap<String, Object>();
        private String defaultValue;
        private String key;

        public Select(String name, String defaultValue) {
            super(name);
            this.key = defaultValue;
            this.defaultValue = defaultValue;
        }

        @Override
        public Object getValue() {
            Object o = values.get(key);
            if (o == null) return values.get(defaultValue);
            return o;
        }

        public Select entry(String key,Object value) {
            values.put(key,value);
            return this;
        }
        public Object getInstance() {
            Object o = getValue();
            Class clazz = null;
            try {
                if (o instanceof Class) {
                    clazz = (Class) o;
                } else if (o instanceof String) {
                    clazz = Class.forName(o.toString());
                } else {
                    return null;
                }
                return clazz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public int update(String[] args, int index) {
            this.key = args[++index];
            return index;
        }
    }

}
