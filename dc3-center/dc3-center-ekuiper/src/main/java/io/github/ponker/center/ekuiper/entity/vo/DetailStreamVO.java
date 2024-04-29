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
public class DetailStreamVO {

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
    public static class Options {

        private String datasource;

        private String format;

        private String confKey;

        private String type;

        private Boolean shared;

        public Options(String datasource, String format, String confKey, String type, Boolean shared) {
            this.datasource = datasource;
            this.format = format;
            this.confKey = confKey;
            this.type = type;
            // 如果提供了 shared 属性，使用提供的值，否则默认为 false
            this.shared = shared != null ? shared : false;
        }

        // 添加 getter 和 setter 方法，以便在接收 JSON 数据时进行处理
        public Boolean getShared() {
            return shared != null ? shared : false;
        }

        public void setShared(Boolean shared) {
            this.shared = shared != null ? shared : false;
        }
    }
}