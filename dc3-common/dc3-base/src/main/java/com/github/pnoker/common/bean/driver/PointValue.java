package com.github.pnoker.common.bean.driver;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class PointValue implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long deviceId;
    private Long pointId;
    private String rawValue;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String value;
    private Long originTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long createTime;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long interval;

    public PointValue(Long deviceId, Long pointId, String rawValue, String value) {
        this.deviceId = deviceId;
        this.pointId = pointId;
        this.rawValue = rawValue;
        this.value = value;
        this.originTime = System.currentTimeMillis();
    }
}
