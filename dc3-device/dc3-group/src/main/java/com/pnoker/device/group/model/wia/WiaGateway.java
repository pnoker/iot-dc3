package com.pnoker.device.group.model.wia;

import lombok.Data;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Wia 网关设备信息
 */
@Data
public class WiaGateway {
    private long id;
    private String ipAddress;
    private int port;
    private int localPort;
    private boolean ping;
    private long time;
}
