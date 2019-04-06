import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.subject.TopicNameStrategy;
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@Slf4j
public class AvroSchemaCompatibilityTest {
    private static Map<String, Schema> LOADED_SCHEMAS;

    private static SchemaRegistryClient CLIENT;

    private static StreamRegistrations STREAM_REGISTRATIONS;

    @BeforeClass
    public static void setup() throws Exception {
        // Variables read from Maven Profiles & surefire plugin
        String schemaDirectory = System.getProperty("avro.schema.directory");

        // Convert idl
        String avroSrcDir = System.getProperty("avro.source.directory");
        log.info("[ConvertIdl] converting avdl to avsc from " + avroSrcDir + " to " + schemaDirectory);
        File inDir = new File(avroSrcDir);
        File outDir = new File(schemaDirectory);
        ConvertIdl convertIdl = new ConvertIdl();
        convertIdl.convertIdl(inDir, outDir);

        LOADED_SCHEMAS = loadSchemas(schemaDirectory);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        // HACK!! -- should probably do this in a better manner
        File file = new File("src/main/resources/stream-registration.yaml");
        STREAM_REGISTRATIONS = mapper.readValue(file, StreamRegistrations.class);
        log.info("streamRegistrations={}", STREAM_REGISTRATIONS);

        RestService restService = new RestService(Arrays.asList(STREAM_REGISTRATIONS.getSchemaRegistry().split(",")));
        CLIENT = new CachedSchemaRegistryClient(restService, 1000);
    }

    @Test
    public void registerStreams() {
        log.info("Schema Registry URL = {}", STREAM_REGISTRATIONS.getSchemaRegistry());

        STREAM_REGISTRATIONS.getStreams().forEach(this::registerStream);
    }
    
    private void registerStream(StreamRegistration sr) {
        // ensure topic is created
        ensureTopicCreated(sr);

        final SubjectNameStrategy<Schema> subjectNameStrategy = getSubjectNameStrategy(sr);
        String compatibility = sr.getCompatibility();

        // check compatibility of type
        sr.getTypes().forEach(s -> {
            Schema schema = LOADED_SCHEMAS.get(s);
            assertThat(schema, is(notNullValue()));
            checkCompatibilityAndRegister(sr.getStreamName(), schema, subjectNameStrategy, compatibility);
        });
    }

    private void checkCompatibilityAndRegister(String topic, Schema schema, SubjectNameStrategy<Schema> subjectNameStrategy, String compatibility) {
        try {
            // ensure compatibility level
            ensureCompatibilityLevelIsSet(topic, schema, subjectNameStrategy, compatibility);

            // ensure compatibility
            ensureCompatibility(topic, schema, subjectNameStrategy);

            // register schema
            registerSchema(topic, schema, subjectNameStrategy);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not check compatibility.", exception);
        }
    }

    private void registerSchema(String topic, Schema schema, SubjectNameStrategy<Schema> subjectNameStrategy) throws IOException, RestClientException {
        String subject = subjectNameStrategy.subjectName(topic, false, schema);
        log.info("[Register Schema] Registering schema for topic={} subject={}", topic, subject);
        CLIENT.register(subject, schema);
    }

    private void ensureCompatibilityLevelIsSet(String topic, Schema schema, SubjectNameStrategy<Schema> subjectNameStrategy, String compatibility) throws IOException, RestClientException {
        String subject = subjectNameStrategy.subjectName(topic, false, schema);
        String currentCompatibility;
        try {
            currentCompatibility = CLIENT.getCompatibility(subject);
            if(!currentCompatibility.equals(compatibility)) {
                log.info("[EnsureCompatibility] subject={} current compatibility={} ... setting to compatibility={}", subject, currentCompatibility, compatibility);
                CLIENT.updateCompatibility(subject, compatibility);
            }
        } catch (RestClientException restClientException) {
            if (restClientException.getErrorCode() != 40401) {
                throw new IllegalStateException("Could not check compatibility for subject:" + subject, restClientException);
            }
            log.info("[EnsureCompatibility] subject={} does not exist. Setting compatibility={}", subject, compatibility);
            // subject not found this is ok.
            CLIENT.updateCompatibility(subject, compatibility);
        }
    }

    private void ensureCompatibility(String topic, Schema schema, SubjectNameStrategy<Schema> subjectNameStrategy) throws IOException {
        String subject = subjectNameStrategy.subjectName(topic, false, schema);
        try {
            if (!CLIENT.testCompatibility(subject, schema)) {
                throw new IllegalStateException("Schema not valid with subject=" + subject);
            }
            log.info("[EnsureCompatibility] Requested schema for Subject={} is compatible. Proceeding.");
        } catch (RestClientException restClientException) {
            if(restClientException.getErrorCode()!=40401) {
                throw new IllegalStateException("Could not test compatibility with subject=" + subject, restClientException);
            }
            log.info("[EnsureCompatibility] Subject={} does not exist. Assuming schema is compatible.", subject);
        }
    }

    private SubjectNameStrategy<Schema> getSubjectNameStrategy(StreamRegistration sr) {
        if (sr.isMultiType()) {
            return new TopicRecordNameStrategy();
        }
        return new TopicNameStrategy();
    }

    private void ensureTopicCreated(StreamRegistration sr) {
        try {
            log.info("[EnsureTopicCreated] Initializing kafka admin CLIENT");
            Properties props = new Properties();
            props.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, STREAM_REGISTRATIONS.getBootstrapServers());
            props.setProperty(AdminClientConfig.CLIENT_ID_CONFIG, "stream-modeling-pipeline");
            AdminClient adminClient = KafkaAdminClient.create(props);
            log.info("[EnsureTopicCreated] ClusterID = {}", adminClient.describeCluster().clusterId().get());

            ListTopicsResult topicResults = adminClient.listTopics();
            if(topicResults.names().get().contains(sr.getStreamName())) {
                log.info("[EnsureTopicCreated] Topic={} already exists.", sr.getStreamName());
                return;
            }
            log.info("[EnsureTopicCreated] Creating topic={} partitions={} replicationFactor={}", sr.getStreamName(), sr.getPartitions(), sr.getReplication());
            CreateTopicsResult result = adminClient.createTopics(
                    Collections.singletonList(new NewTopic(sr.getStreamName(), sr.getPartitions(), sr.getReplication())));
            // wait for result
            result.values().get(sr.getStreamName()).get();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not ensure topic created for stream=" + sr.getStreamName(), exception);
        }
    }

    /**
     * Parses the schemas in the given directory
     *
     * @param schemaDirectory The file location containing AVSC files
     * @return (SubjectName => Schema) mapping
     */
    private static Map<String, Schema> loadSchemas(String schemaDirectory) {
        File[] schemaFiles = new File(schemaDirectory).listFiles();

        int errorCount = 0;
        Map<String, Schema> results = new LinkedHashMap<>();

        for (File schemaFile : Objects.requireNonNull(schemaFiles, "No files in schemaDirectory:" + schemaDirectory)) {
            if(!schemaFile.isDirectory()) {
                Schema.Parser parser = new Schema.Parser();
                final String fileName = schemaFile.getName();
                log.info("Loading schema for subject - {} from {}",
                    fileName,
                    schemaFile);

                try (FileInputStream inputStream = new FileInputStream(schemaFile)) {
                    Schema schema = parser.parse(inputStream);
                    results.put(FilenameUtils.getBaseName(fileName), schema);
                } catch (IOException | SchemaParseException ex) {
                    log.error("Exception thrown while loading " + schemaFile, ex);
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
