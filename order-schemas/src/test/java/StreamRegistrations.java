import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class StreamRegistrations {
    @JsonProperty("schema-registry")
    private String schemaRegistry;

    @JsonProperty("bootstrap-servers")
    private String bootstrapServers;

    @JsonProperty("streams")
    private List<StreamRegistration> streams;
}

