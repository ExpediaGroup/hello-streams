import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.tool.IdlToSchemataTool;

//import exceptions.ConvertIdlException;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Converts an entire directory from Avro IDL (.avdl) to schema (.avsc)
 */
@Slf4j
public class ConvertIdl {
    public static void main(String[] args) throws ConvertIdlException {

        ArgumentParser argumentParser = ArgumentParsers.newFor("util.ConvertIdl")
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

        IdlToSchemataTool tool = new IdlToSchemataTool();

        File inDir = new File(ns.getString("inputDir"));
        File outDir = new File(ns.getString("outputDir"));

        for (File inFile : inDir.listFiles()) {
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
}
