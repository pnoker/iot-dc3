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
public class DriverOperation {
    private String command;
    private Long id;
    private Long driverId;
}
