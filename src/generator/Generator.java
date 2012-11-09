import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class Generator
{
    public static void main(String[] cmd)
    {
        Document dom = parseXML();
        NodeList properties = dom.getElementsByTagName("jcc");
        String jcc = null;
        for (int i=0; i < properties.getLength(); i++)
        {
            Node n = properties.item(i);
            String content = n.getTextContent();
            if (content.trim().length() > 0)
                jcc = content;
        }

        NodeList list = dom.getElementsByTagName("opcode");
        for (int i=0; i < list.getLength(); i++)
        {
            Node n = list.item(i);
            String mnemonic = n.getAttributes().getNamedItem("mnemonic").getNodeValue();
            NodeList children = n.getChildNodes();
            String ret=null, snippet=null;
            // get return and snippet
            for (int j=0; j < children.getLength(); j++)
            {
                Node c = children.item(j);
                if (c.getNodeName().equals("return"))
                    ret = c.getTextContent().trim();
                if (c.getNodeName().equals("snippet"))
                    snippet = c.getTextContent();
                if (c.getNodeName().equals("jcc"))
                    snippet += jcc;
            }
            if (ret == null)
                throw new IllegalStateException("No return value for "+mnemonic);
            if (snippet == null)
                throw new IllegalStateException("No snippet for "+mnemonic);

            // get each opcode definition
            for (int j=0; j < children.getLength(); j++)
            {
                Node c = children.item(j);                
                if (!c.getNodeName().equals("args"))
                    continue;
                String argsText = c.getTextContent();
                int size = Integer.parseInt(c.getAttributes().getNamedItem("size").getNodeValue());
                String[] args = argsText.split(";");
                if (argsText.length() == 0)
                    args = new String[0];
                List<Opcode> ops = Opcode.get(mnemonic, args, size, snippet, ret);
                for (Opcode op: ops)
                {
                    System.out.println(op.getName());
                    op.writeToFile();
                }
            }
        }
    }

    public static Document parseXML()
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse("Opcodes.xml");
        }catch(ParserConfigurationException pce) {
            pce.printStackTrace();
        }catch(SAXException se) {
            se.printStackTrace();
        }catch(IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }
}
