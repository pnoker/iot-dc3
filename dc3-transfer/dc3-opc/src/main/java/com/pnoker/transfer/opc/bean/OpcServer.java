package com.pnoker.transfer.opc.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Opc 服务基本信息
 */
@Data
@AllArgsConstructor
public class OpcServer {
    private String clsId;
    private String progId;
    private String description;
}
