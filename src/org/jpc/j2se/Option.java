package org.jpc.j2se;

import java.lang.reflect.*;
import java.util.*;

public abstract class Option {
    private static final Hashtable<String, Option> names2options = new Hashtable();

    public static final Switch log_disam = createSwitch("log-disam");
    public static final Switch log_disam_addresses = createSwitch("log-disam-addresses");
    public static final Switch log_state = createSwitch("log-state");
    public static final Switch log_blockentry = createSwitch("log-blockentry");
    public static final Opt ram = opt("ram");
    public static final Opt max_instructions_per_block = opt("max-block-size");

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
            } else {
                opt.set = true;
                index = opt.update(source, index);
            }
        }
        if (tmp.size() == source.length) {
            return source;
        } else {
            return tmp.toArray(new String[tmp.size()]);
        }
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

        public String[] arrayValue(String[] defaultValue) {
            if (value != null) {
                return value.split(":");
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

        /**
         * Invoke
         *
         * @param type
         * @return
         */
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
