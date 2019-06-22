package com.pnoker.common.bean.wia;

import lombok.Data;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Data
public class HartData {
    private String valueName;
    private float value;
    private long time;

    public HartData(String valueName) {
        this.valueName = valueName;
        this.time = System.currentTimeMillis();
    }

    /**
     * 更新数据，并触发相应操作
     *
     * @param value
     */
    public void update(float value) {
        this.value = value;
        this.time = System.currentTimeMillis();
        //发送消息队列
        //入库
    }
}
