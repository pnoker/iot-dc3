package com.pnoker.common.bean.driver;

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
    private String type;
    private Object value;
    private Long createTime;
    private Long originTime;
    private Long interval;

    public PointValue(Long deviceId, Long pointId, String type, Object value) {
        this.deviceId = deviceId;
        this.pointId = pointId;
        this.type = type;
        this.value = value;
        this.originTime = System.currentTimeMillis();
    }
}
