package com.pnoker.transfer.opc.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Opc 节点信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpcNodes {
    private String name;
    private String type;
    private List<OpcNodes> children;

    public OpcNodes(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
