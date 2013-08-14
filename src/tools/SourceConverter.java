package tools;


import java.io.*;
import java.util.*;
import java.util.regex.*;

public class SourceConverter
{

    public static void main(String[] args) throws IOException
    {
        String outputDir = ".";
        String outputPackage = "org.jpc.emulator.peripheral";
        String inputFile = "/home/ian/jpc/bochs/bochs-2.6.1/iodev/floppy.cc";
        String[] inputHeader = new String[] {"/home/ian/jpc/bochs/bochs-2.6.1/iodev/floppy.h", "floppy_include.txt"};
        for (int i=0; i < args.length; i++)
        {
            if (args[i].equals("-output"))
            {
                outputDir = args[i+1];
                i++;
            }
            else if (args[i].equals("-package"))
            {
                outputPackage = args[i+1];
                i++;
            }
            else if (args[i].equals("-input"))
            {
                inputFile = args[i+1];
                i++;
            }
        }
        File outRoot = new File(outputDir);
        File outDir = new File(outRoot, outputPackage.replaceAll("\\.", "/"));
        File inFile = new File(inputFile);
        String name = inFile.getName().replaceAll("\\.cc", "");
        File outFile = new File(outDir, name + ".java");
        StringBuilder b =new StringBuilder();
        for (String header: inputHeader)
        {
            BufferedReader r = new BufferedReader(new FileReader(header));
            String line;
            while ((line = r.readLine()) != null)
                b.append(line + "\n");
        }

        {
            BufferedReader r = new BufferedReader(new FileReader(inFile));
            String line;
            while ((line = r.readLine()) != null)
                b.append(line + "\n");
        }

        String result = convert(b.toString(), getRegex());
        BufferedWriter w = new BufferedWriter(new FileWriter(outFile));
        writeHeader(w, outputPackage, name);
        w.write(result.toString());
        writeFooter(w);
        w.flush();
        w.close();
    }

    private static void writeHeader(BufferedWriter w, String pack, String name) throws IOException  {
        w.append("package "+ pack+";\n\n");
        w.append("import org.jpc.support.*;\n");
        w.append("public class "+ name + "\n{\n");
        w.append("private static final boolean DEBUG = false;\n");
    }

    private static void writeFooter(BufferedWriter w) throws IOException {
        w.append("}");
    }

    private static List<Pair> getRegex() throws IOException
    {
        List<Pair> reg = new ArrayList();
        BufferedReader r = new BufferedReader(new FileReader("floppy_regex.txt"));
        String line;
        while ((line = r.readLine()) != null)
            reg.add(new Pair(line, r.readLine()));
        return reg;
    }

    private static String[] complex_types = new String[] {"floppy_t", "floppy_type_t"};
    private static String[] primitives = new String[] {"int"};
    private static String[] functionsToDelete = new String[] {"int libfloppy_LTX_plugin_init", "void libfloppy_LTX_plugin_fini", "bx_floppy_ctrl_c", "~bx_floppy_ctrl_c"};
    private static Map<String, Boolean> macros = new HashMap();
    static {
        macros.put("BX_DEBUGGER", false);
        macros.put("BX_USE_FD_SMF", false);
    }

    private static String convert(String in, List<Pair> regex)
    {
        // simple regex
        for (Pair p: regex)
            in = in.replaceAll(p.key, p.value);

        // more complex replacements (single layer structs)
        for (String type: complex_types)
        {
            // definition
            Pattern def = Pattern.compile("typedef struct \\{([\\w\\s;/\\*]+)\\} "+type+";");
            Matcher matcher = def.matcher(in);
            if (matcher.find())
            {
                String body = matcher.group(1);
                String[] lines = body.trim().split("\n");
                for (int i=0; i < lines.length; i++)
                {
                    if (lines[i].length() == 0)
                        continue;
                    lines[i] = lines[i].substring(0, lines[i].indexOf(";")); // ignore comments after ;
                    lines[i] = lines[i].replaceAll("[\\s]+", " "); // contract spaces
                }

                String args = "";
                for (String arg: lines)
                {
                    if (arg.trim().length() == 0)
                        continue;
                    args += arg.trim() + ", ";
                }
                args = args.substring(0, args.length()-2);
                String constructorBody = "";
                for (String arg: lines)
                {
                    if (arg.trim().length() == 0)
                        continue;
                    String name = arg.trim().split(" ")[1];
                    constructorBody += "      this."+name + " = "+name+";\n";
                }
                in = in.replaceAll("typedef struct \\{([\\w\\s;/\\*]+)\\} "+type+";", "static class "+type+"\n{\n$1\n   public "+type+"("+args+")\n   {\n"+constructorBody+"   }\n}");
            }

            // Array uses of type
            while (true)
            {
                String pat = type + "[\\s]+([\\w]+)\\[[\\d]+\\][\\s]+=[\\s]+\\{([\\w\\s,\\{\\}]+)\\};";
                Pattern arr = Pattern.compile(pat);
                Matcher match = arr.matcher(in);
                if (match.find())
                {
                    String name = match.group(1);
                    String body = match.group(2);
                    // find array elements
                    String [] lines = body.trim().split("},");
                    String newbody = "";
                    for (int i=0; i < lines.length; i++)
                    {
                        if (lines[i].length() == 0)
                            continue;
                        lines[i] = lines[i].trim();
                        lines[i] = lines[i].replaceAll("[\\s]+", " "); // contract spaces
                        if (lines[i].startsWith("{"))
                        {
                            lines[i] = "new "+type+"("+lines[i].substring(1, lines[i].length()-1)+")";
                        }
                        newbody += "   "+ lines[i]+", \n";
                    }
                    newbody = newbody.substring(0, newbody.length()-3);
                    String namedPat = type + "[\\s]+" + name + "\\[[\\d]+\\][\\s]+=[\\s]+\\{([\\w\\s,\\{\\}]+)\\};";
                    in = in.replaceAll(namedPat, type + "[] "+name + " = new "+type+"[] {\n"+newbody+"};");
                    continue;
                }
                break;
            }
        }
        // arrays of primitive types
        for (String type: primitives)
            while (true)
            {
                String pat = type + "[\\s]+([\\w]+)\\[[\\d]+\\][\\s]+=[\\s]+\\{([\\w\\s,\\{\\}]+)\\};";
                Pattern arr = Pattern.compile(pat);
                Matcher match = arr.matcher(in);
                if (match.find())
                {
                    String name = match.group(1);
                    String body = match.group(2);
                    // find array elements
                    String [] lines = body.trim().split(",");
                    String newbody = "";
                    for (int i=0; i < lines.length; i++)
                    {
                        if (lines[i].length() == 0)
                            continue;
                        lines[i] = lines[i].trim();
                        lines[i] = lines[i].replaceAll("[\\s]+", " "); // contract spaces
                        newbody += lines[i]+", ";
                    }
                    newbody = newbody.substring(0, newbody.length()-2);
                    String namedPat = type + "[\\s]+" + name + "\\[[\\d]+\\][\\s]+=[\\s]+\\{([\\w\\s,\\{\\}]+)\\};";
                    in = in.replaceAll(namedPat, type + "[] "+name + " = new "+type+"[] {"+newbody+"};");
                    continue;
                }
                break;
            }

        // delete unnecessary functions
        for (String func: functionsToDelete)
        {
            while (in.contains(func))
            {
                int start = in.indexOf(func);
                int end = start+func.length();
                while (in.charAt(end) != ')')
                    end++;
                while (in.charAt(end) != '{')
                    end++;
                // find matching bracket to opening bracket // assume they match and no comments contain {}'s!
                int open = 1;
                end++;
                while (open > 0)
                {
                    if (in.charAt(end) == '{')
                    {open++;System.out.println("Open: "+open + " " + in.substring(start, end+1));}
                    else if (in.charAt(end) == '}')
                    {open--;System.out.println("Open: "+open + " " + in.substring(start, end+1));}
                    end++;
                }
                //System.out.println("Removing: " + in.substring(start, end));
                in = in.substring(0, start) + in.substring(end);
            }
        }

        return in;
    }

    private static class Pair
    {
        String key, value;

        public Pair(String key, String value)
        {
            this.key= key;
            this.value = value;
        }
    }
}
