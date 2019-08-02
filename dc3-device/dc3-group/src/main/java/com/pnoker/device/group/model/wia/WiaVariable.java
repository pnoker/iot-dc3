package com.pnoker.device.group.model.wia;

import lombok.Data;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Data
public class WiaVariable {
    private long id;
    private long deviceId;
    private long unitId;
    private String name;
    private float ratio;
}
