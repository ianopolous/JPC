package tools;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * User: Ian Preston
 */
public class TestGenerator
{

    public static void main(String[] args) throws Exception
    {
        
        // for each opcode definition find the list of used examples, and generate test set for each varying inputs
        // get used encodings
        Map<String, Map<String, Map<String, List<String>>>> res = parseEncodings("disam-uniq.txt");
        Tests trm = new Tests(res.get("rm"), "rm");
        Tests tpm = new Tests(res.get("pm"), "pm");
        // get opcode definitions
        Generator.opcodeParse(Generator.parseXML("RM"), "rm", trm);
        trm.close();
        Generator.opcodeParse(Generator.parseXML("PM"), "pm", tpm);
        tpm.close();
    }

    public static class Test
    {
        byte[] code;
        int[] input;
        String mode;
        String classname;
        String disam;
    }

    public static class Tests implements Callable
    {
        final Map<String, Map<String,List<String>>> encodings;
        final String mode;
        BufferedWriter w;

        public Tests(Map<String, Map<String,List<String>>> encodings, String mode) throws IOException
        {
            this.encodings = encodings;
            this.mode = mode;
            w = new BufferedWriter(new FileWriter("tests/"+mode+".tests"));
        }

        public void call(Opcode op, String mode)
        {
            if (encodings.containsKey(op.getName()) && this.mode.equals(mode))
            {
                String genname = op.getName();
                Map<String,List<String>> types = encodings.get(genname);
                // for each encoding enumerate input sets
                for (String disam: types.keySet())
                {
                    System.out.println(disam);
                    for (String codeHex: types.get(disam))
                    {
                        System.out.println("   "+codeHex);
                    }
                }
            }
        }

        public void close() throws IOException
        {
            w.flush();
            w.close();
        }
    }

    public static Map<String, Map<String, Map<String, List<String>>>> parseEncodings(String file) throws IOException
    {
        Map<String, Map<String,List<String>>> rm = new HashMap();
        Map<String, Map<String,List<String>>> pm = new HashMap();
        Map<String, Map<String, Map<String,List<String>>>> res = new HashMap();
        res.put("rm", rm);
        res.put("pm", pm);
        BufferedReader r = new BufferedReader(new FileReader(file));
        String line=null;
        while ((line=r.readLine()) != null)
        {
            String[] parts = line.split(";");
            if (parts[0].equals("16"))
            {
                if (!rm.containsKey(parts[1]))
                    rm.put(parts[1], new HashMap());
                if (!rm.get(parts[1]).containsKey(parts[2]))
                    rm.get(parts[1]).put(parts[2], new ArrayList());
                rm.get(parts[1]).get(parts[2]).add(parts[3]);
            }
            else if (parts[0].equals("32"))
            {
                if (!pm.containsKey(parts[1]))
                    pm.put(parts[1], new HashMap());
                if (!pm.get(parts[1]).containsKey(parts[2]))
                    pm.get(parts[1]).put(parts[2], new ArrayList());
                pm.get(parts[1]).get(parts[2]).add(parts[3]);
            }
        }
        return res;
    }
}
