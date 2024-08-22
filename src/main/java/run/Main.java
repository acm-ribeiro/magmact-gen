package run;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import magmact_gen.MagmaCtGen;
import oas_custom_parser.OASCustomParser;

import org.openapi4j.core.exception.EncodeException;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;

import run.exceptions.CreateDirectoryException;
import serialization.SpecificationWrapper;
import oas_custom_parser.domain.Specification;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class Main {

    public static final String DIRECTORY = "magmact-contracts";


    public static void main(String[] args) throws EncodeException, IOException {
        // Checking arguments
        if(args.length < 1) {
            System.err.println("Please, provide the Open API Specification JSON file path.");
            System.exit(1);
        }

        String filepath = args[0];
        File specFile = new File(filepath);

        try {
            // Creating magmact-contracts directory if it does not exist
            createDirectory();

            // Parsing OAS
            Specification spec = OASCustomParser.parse(specFile);

            // Generating MAGMACt contracts
            MagmaCtGen.generate(spec);

            // Serialization - required is wrong here
            SpecificationWrapper specWrapper = new SpecificationWrapper(spec);
            serialize(specWrapper, createJsonFile(specFile.getName()));

            // Print contracts
            MagmaCtGen.print_contracts(spec);

        } catch(ValidationException e) {
            e.printStackTrace();
            System.err.println("OpenAPI 3 validation failure. Please, provide a specification in version 3.");
        } catch (ResolutionException | CreateDirectoryException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void createDirectory() throws CreateDirectoryException {
        File directory = new File(DIRECTORY);
        boolean created = false;

        if (!directory.exists())
            created = directory.mkdir();

        if (created)
            System.out.println("Created new directory: /" + DIRECTORY);

        if (!directory.exists())
            throw new CreateDirectoryException(DIRECTORY);
    }

    private static FileWriter createJsonFile(String filename) throws IOException {
        String magmact_filename = filename.replace(".json", "") + "-magmact.json";
        return new FileWriter(DIRECTORY + "/" + magmact_filename);
    }

    private static void serialize(SpecificationWrapper wrapper, FileWriter writer) throws IOException {
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        gson.toJson(wrapper, writer);
        writer.flush();
        writer.close();
    }

}
