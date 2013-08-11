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
        File outFile = new File(outDir, inFile.getName().replaceAll("\\.cc", "\\.java"));
        StringBuilder b =new StringBuilder();
        BufferedReader r = new BufferedReader(new FileReader(inFile));
        String line;
        while ((line = r.readLine()) != null)
            b.append(line + "\n");

        String result = convert(b.toString(), getRegex());
        BufferedWriter w = new BufferedWriter(new FileWriter(outFile));
        w.write(result.toString());
        w.flush();
        w.close();
    }

    private static Map<String, String> getRegex() throws IOException
    {
        Map reg = new TreeMap();
        BufferedReader r = new BufferedReader(new FileReader("regex.txt"));
        String line;
        while ((line = r.readLine()) != null)
            reg.put(line, r.readLine());
        return reg;
    }

    private static String convert(String in, Map<String, String> regex)
    {
        for (String key: regex.keySet())
            in = in.replaceAll(key, regex.get(key));
        return in;
    }
}
