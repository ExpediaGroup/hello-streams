import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class StreamRegistration {
    @JsonProperty("stream-name")
    private String streamName;

    @JsonProperty("multi-type")
    private boolean multiType;

    @JsonProperty("partitions")
    private int partitions;

    @JsonProperty("replication")
    private short replication;

    @JsonProperty("compatibility")
    private String compatibility;

    @JsonProperty("types")
    private List<String> types;
}
