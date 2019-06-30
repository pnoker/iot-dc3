package com.pnoker.device.group.model;

import lombok.Data;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 变量信息
 */
@Data
public class Variable {
    private long id;
    private long deviceId;
    private String unit;
    private String name;
    private float ratio;
}
