package io.github.pnoker.center.manager.entity.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * @version 1.0
 * @Author 嘉平十二
 * @Date 2024/7/22 13:05
 * @注释
 */
@Data
public class MQTTtopicVO {
    private String topic;
    private JsonNode message;
}
