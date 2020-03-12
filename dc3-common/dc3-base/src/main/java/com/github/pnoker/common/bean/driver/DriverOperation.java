package com.github.pnoker.common.bean.driver;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author pnoker
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class DriverOperation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String command;
    private Long id;
    private Long parentId;
    private Long attributeId;
}
