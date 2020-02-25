package com.pnoker.common.bean.driver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointValue {
    private Long deviceId;
    private Long pointId;
    private String type;
    private Object value;
}
