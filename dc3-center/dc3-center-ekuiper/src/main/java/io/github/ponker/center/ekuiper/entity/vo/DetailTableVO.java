package io.github.ponker.center.ekuiper.entity.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * @author : Zhen
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetailTableVO {
    @JsonProperty("Name")
    private String Name;

    @JsonProperty("StreamFields")
    private List<StreamField> StreamFields;

    @JsonProperty("Options")
    private Options Options;

    @JsonProperty("StreamType")
    private int StreamType;

    @JsonProperty("Statement")
    private String Statement;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreamField {

        @JsonProperty("Name")
        private String Name;

        @JsonProperty("FieldType")
        private String FieldType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldType {

        private int type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Options {

        private String datasource;

        private String format;

        private String confKey;

        private String type;

        private String key;

        private String kind;
    }
}
