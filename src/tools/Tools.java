package tools;

public class Tools
{
    public static void main(String[] args) throws Exception
    {
        if (args.length == 0)
        {
            Generator.main(args);
        }
        else if (args[0].equals("-bochs"))
        {
            String[] rest = new String[args.length-1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            CompareToBochs.main(rest);
        }
        else if (args[0].equals("-compare"))
        {
            String[] rest = new String[args.length-1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            Comparison.main(rest);
        }
        else if (args[0].equals("-compare-single"))
        {
            String[] rest = new String[args.length-1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            ComparisonToSingleStep.main(rest);
        }
        else if (args[0].equals("-fuzz"))
        {
            String[] rest = new String[args.length-1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            Fuzzer.main(rest);
        }
        else if (args[0].equals("-history"))
        {
            String[] rest = new String[args.length-1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            History.main(rest);
        }
        else if (args[0].equals("-decoder"))
        {
            String[] rest = new String[args.length-1];
            System.arraycopy(args, 1, rest, 0, rest.length);
            DecoderGenerator.generate();
        }
    }
}