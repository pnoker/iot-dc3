package com.pnoker.common.bean.driver;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author pnoker
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class DriverOperation {
    private String command;
    private Long id;
    private Long parentId;
    private Long attributeId;
}
