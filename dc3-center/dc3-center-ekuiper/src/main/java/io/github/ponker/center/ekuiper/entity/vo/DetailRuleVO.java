package io.github.ponker.center.ekuiper.entity.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author : Zhen
 */
@Data
//@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailRuleVO {

    private String id;

    private String name;

    private String sql;

    private List<Map<String, Object>> actions;

    private Map<String, Object> options;

    //private Map<String, Object> dynamicProperties = new HashMap<>();

//    @JsonAnySetter
//    public void setDynamicProperty(String name, Object value) {
//        dynamicProperties.put(name, value);
//    }
}
