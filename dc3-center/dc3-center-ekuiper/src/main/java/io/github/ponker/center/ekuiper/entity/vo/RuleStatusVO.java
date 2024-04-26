package io.github.ponker.center.ekuiper.entity.vo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : Zhen
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class RuleStatusVO {

    private String status;
    private Map<String, Object> dynamicProperties = new HashMap<>();

    @JsonAnySetter
    public void setDynamicProperty(String name, Object value) {
        dynamicProperties.put(name, value);
    }
}
