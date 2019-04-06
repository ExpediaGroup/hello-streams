import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.avro.tool.IdlToSchemataTool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Converts an entire directory from Avro IDL (.avdl) to schema (.avsc)
 */
@Slf4j
public class ConvertIdl {
    public void convertIdl(File inDir, File outDir) throws ConvertIdlException {
        IdlToSchemataTool tool = new IdlToSchemataTool();

        for (File inFile : Objects.requireNonNull(inDir.listFiles(), inDir.toString() + " directory is empty. Expected *.avdl files.")) {
            if (inFile.getName().endsWith(".avdl")) {
                List<String> toolArgs = new ArrayList<>();
                toolArgs.add(inFile.getAbsolutePath());
                toolArgs.add(outDir.getAbsolutePath());

                try {
                    tool.run(System.in, System.out, System.err, toolArgs);
                } catch (Exception e) {
                    throw new ConvertIdlException("Exception in converting avdl to avsc" + e);
                }
            }
        }
    }

    public static void main(String[] args) throws ConvertIdlException {

        ArgumentParser argumentParser = ArgumentParsers.newFor("ConvertIdl")
            .build()
            .defaultHelp(true)
            .description("Converts an entire directory from Avro IDL (.avdl) to schema (.avsc)");

        argumentParser.addArgument("-i", "--inputDir")
            .dest("inputDir")
            .help("Input Directory for Avro IDL");
        argumentParser.addArgument("-o", "--outputDir")
            .dest("outputDir")
            .help("Output Directory for Avsc");

        Namespace ns = null;
        try {
            ns = argumentParser.parseArgs(args);
        } catch (ArgumentParserException e) {
            argumentParser.handleError(e);
            System.exit(1);
        }

        File inDir = new File(ns.getString("inputDir"));
        File outDir = new File(ns.getString("outputDir"));
        ConvertIdl instance = new ConvertIdl();
        instance.convertIdl(inDir, outDir);
    }
}
