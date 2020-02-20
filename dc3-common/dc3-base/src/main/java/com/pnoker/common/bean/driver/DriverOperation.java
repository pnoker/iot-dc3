package com.pnoker.common.bean.driver;

import lombok.Data;

/**
 * @author pnoker
 */
@Data
public class DriverOperation {
    private String command;
    private Long id;
    private Long driverId;
}
