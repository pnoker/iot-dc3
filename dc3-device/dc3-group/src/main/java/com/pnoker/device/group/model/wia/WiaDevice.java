package com.pnoker.device.group.model.wia;

import lombok.Data;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Wia 数采设备信息
 */
@Data
public class WiaDevice {
    private long id;
    private long gatewayId;
    private String name;
    private String longAddress;
    private int status;
    private long time;

}
