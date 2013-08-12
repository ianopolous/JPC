package tools;


import java.io.*;
import java.util.*;

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

    private static String convert(String in, List<Pair> regex)
    {
        for (Pair p: regex)
            in = in.replaceAll(p.key, p.value);
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
