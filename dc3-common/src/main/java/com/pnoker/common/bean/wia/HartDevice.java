package com.pnoker.common.bean.wia;

import lombok.Data;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 设备信息数据
 */
@Data
public class HartDevice {
    private String deviceName;
    private String longAddress;
    private String dataType;
    private int start;
    private int end;
    private HartData hartData;

    public HartDevice(String deviceName, String valueName) {
        this.deviceName = deviceName;
        this.hartData = new HartData(valueName);
    }

    /**
     * 设置Hart设备基本信息：长地址、数据报文解析格式、起始数据报文索引
     *
     * @param longAddress
     * @param dataType
     * @param start
     * @param end
     */
    public void info(String longAddress, String dataType, int start, int end) {
        this.longAddress = longAddress;
        this.dataType = dataType;
        this.start = start;
        this.end = end;
    }
}
