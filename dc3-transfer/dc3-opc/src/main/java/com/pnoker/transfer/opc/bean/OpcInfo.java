package com.pnoker.transfer.opc.bean;

import lombok.Data;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Opc 服务连接信息
 */
@Data
public class OpcInfo {
    private String host;
    private String user;
    private String password;
    private String domain;

    private String clsId;
    private String progId;
}
