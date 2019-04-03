import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;
import org.apache.commons.io.FilenameUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import lombok.extern.slf4j.Slf4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class AvroSchemaCompatibilityTest {

    private static String schemaRegistryUrl;

    private static String schemaDirectory;

    private static Map<String, Schema> loadedSchemas;

    private static SchemaRegistryClient client;

    private static RestService restService;

    @BeforeClass
    public static void setup() {
        // Variables read from Maven Profiles & surefire plugin
        schemaRegistryUrl = System.getProperty("schema.registry.url");
        schemaDirectory = System.getProperty("avro.schema.directory");

        loadedSchemas = loadSchemas(schemaDirectory);

        restService = new RestService(Arrays.asList(schemaRegistryUrl.split(",")));
        client = new CachedSchemaRegistryClient(restService, 1000);
    }

    @Test
    public void testSchemaDirectoryReadable() {
        assertFalse("Schema directory has been defined", schemaDirectory.isEmpty());

        File schemataDir = new File(schemaDirectory);
        final File[] schemaFiles = schemataDir.listFiles();
        assertNotNull(schemaFiles);
        assertTrue(schemaFiles.length > 0);

        assertEquals("Schemas have been generated and parsed", schemaFiles.length, loadedSchemas.size());
    }

    @Test
    public void generatedSchemaIsCompatibleWithExistingRegistry() {
        assertFalse("Schema registry URL(s) has been defined", schemaRegistryUrl.isEmpty());

        String register = System.getProperty("register");

        Map<String, Schema> incompatibleSchemas = new HashMap<>();

        System.out.println("Schema Registry URL -"+schemaRegistryUrl);

        for (Map.Entry<String, Schema> entry : loadedSchemas.entrySet()) {

            String subject = entry.getKey();
            Schema schema = entry.getValue();
            System.out.println("Subject: "+subject+" ,schema:"+schema);
            if (!safeTestCompatibility(subject, schema)) {
                incompatibleSchemas.put(subject, schema);
            } else {
                // Only register the schemas if this unit test is ran with a -Dregister=true flag
                //if (register != null && (register.isEmpty() || Boolean.valueOf(register))) {
                registerSchema(subject, schema);
                //}
            }
        }
        assertTrue("Generated Schemas are not compatible with Registry", incompatibleSchemas.isEmpty());
    }

    /**
     * Registers a schema with the registry
     *
     * @param subject Subject name to register under
     * @param schema Schema object to register for the subject
     * @return
     */
    public boolean registerSchema(String subject, Schema schema) {
        try {
            int id = client.register(subject, schema);
            return id > 0;
        } catch (IOException e) {
            log.error("Unable to connect to schema registry", e);
        } catch (RestClientException e) {
            log.error("An exception occurred with the RestClient", e);
        }
        return false;
    }

    /**
     * Loops backwards over all versions of a subject to test the compatibility of a schema
     *
     * @param subject The Subject in the schema registry
     * @param schema The schema to test compatibility for
     * @return True if compatible, or subject not existent
     */
    private boolean safeTestCompatibility(String subject, Schema schema) {
        log.debug("Schema under test for Subject {} is {}", subject, schema);
        try {
            SchemaMetadata latestSchemaMetadata = client.getLatestSchemaMetadata(subject);
            for (int version = latestSchemaMetadata.getVersion(); version > 0; version--) {
                SchemaMetadata versionMetadata = client.getSchemaMetadata(subject, version);

                log.info("Schema {}", versionMetadata.getSchema());
                if (!restService.testCompatibility(schema.toString(), subject, Integer.toString(version))) {
                    log.warn("Generated Schema {} is incompatible with Registered Schema {}", schema, versionMetadata.getSchema());
                    return false;
                }
            }
        } catch (RestClientException restClientException) {
            if (restClientException.getStatus() == 404) {
                log.info("The Subject {} does not exist. Any Schema should be compatible when registered.", subject);
                return true;
            }
            log.warn("Something REST-y happened with testing Schema compatibility for Subject {}. Returning 'incompatible': e={}", subject,
                restClientException);
            return false;
        } catch (Exception exception) {
            throw new IllegalStateException(String.format("Could not safely test compatibility of Schema on Subject. Returning 'compatible': %s %s", subject, schema.toString()), exception);
        }
        return true;
    }

    /**
     * Parses the schemas in the given directory
     *
     * @param schemaDirectory The file location containing AVSC files
     * @return (SubjectName => Schema) mapping
     */
    protected static Map<String, Schema> loadSchemas(String schemaDirectory) {
        File[] schemaFiles = new File(schemaDirectory).listFiles();

        int errorCount = 0;
        Map<String, Schema> results = new LinkedHashMap<>();

        for (File schemaFile : schemaFiles) {
            if(!schemaFile.isDirectory()) {
                Schema.Parser parser = new Schema.Parser();
                final String fileName = schemaFile.getName();
                log.info("Loading schema for subject - {} from {}",
                    fileName,
                    schemaFile);

                try (FileInputStream inputStream = new FileInputStream(schemaFile)) {
                    Schema schema = parser.parse(inputStream);
                    results.put(FilenameUtils.getBaseName(fileName), schema);
                } catch (IOException ex) {
                    log.error("Exception thrown while loading " + schemaFile, ex);
                    errorCount++;
                } catch (SchemaParseException ex) {
                    log.error("Exception thrown while parsing " + schemaFile, ex);
                    errorCount++;
                }
            }
        }

        if (errorCount > 0) {
            throw new IllegalStateException("One or more schemas could not be loaded.");
        }

        return results;
    }

}
