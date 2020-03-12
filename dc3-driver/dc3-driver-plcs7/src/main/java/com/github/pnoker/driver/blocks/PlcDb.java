package com.github.pnoker.driver.blocks;

import com.github.s7connector.api.S7Type;
import com.github.s7connector.api.annotation.S7Variable;
import lombok.Data;

/**
 * 用于演示，具体情况需要自定义数据块
 *
 * @author pnoker
 */
@Data
public class PlcDb {
    @S7Variable(type = S7Type.BOOL, byteOffset = 0)
    public boolean 设备运行状态;

    @S7Variable(type = S7Type.DWORD, byteOffset = 2)
    public long 机器人状态;

    @S7Variable(type = S7Type.DWORD, byteOffset = 6)
    public long 压机状态;

    @S7Variable(type = S7Type.DWORD, byteOffset = 18)
    public long 当前计划生产数;

    @S7Variable(type = S7Type.DWORD, byteOffset = 22)
    public long 当前已经生产次数;

    @S7Variable(type = S7Type.DWORD, byteOffset = 26)
    public long 合模温度;

    @S7Variable(type = S7Type.REAL, byteOffset = 34)
    public float 大滑块位置;

    @S7Variable(type = S7Type.REAL, byteOffset = 38)
    public float 大滑块速度;

    @S7Variable(type = S7Type.REAL, byteOffset = 42)
    public float 实时吨位值;

    @S7Variable(type = S7Type.REAL, byteOffset = 46)
    public float 油液反馈压力;

    @S7Variable(type = S7Type.REAL, byteOffset = 50)
    public float 伺服电机转速;

    @S7Variable(type = S7Type.TIME, byteOffset = 66)
    public long 设备运行时长;

    @S7Variable(type = S7Type.TIME, byteOffset = 70)
    public long 设备生产时长;

}