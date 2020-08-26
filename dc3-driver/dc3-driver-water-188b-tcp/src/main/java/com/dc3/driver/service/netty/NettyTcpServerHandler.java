/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.driver.service.netty;

import com.alibaba.fastjson.JSON;
import com.dc3.common.bean.batch.BatchDevice;
import com.dc3.common.bean.batch.BatchDriver;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.sdk.bean.AttributeInfo;
import com.dc3.common.sdk.bean.DriverContext;
import com.dc3.common.sdk.service.rabbit.DriverService;
import com.dc3.common.sdk.util.DriverUtils;
import com.dc3.common.utils.Dc3Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 报文说明
 * FEFEFE
 * 68
 * 10
 * 03221018200673 表号
 * 0081
 * 96 长度
 * 15: 905F 数据标识DI(90 5F)
 * 17: 2B2B3600 正向累计量单位(2B)，反向累计量单位(2B)，瞬时流量单位(36)，水压单位(00)
 * 21: 18690100 当前正向累计水量 92.440
 * 25: 00000000 当前剩余金额
 * 29: 00000000 当前状态
 * 33: 00 充值识别码
 * 34: 00000000 当前反向累计流量
 * 38: 000000 当前瞬时流量
 * 41: 0000 当前水压
 * 43: 0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 48: 0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 53: 0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 58: 0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 63: 0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 68: 0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 73: 0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 78: 0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 83: 0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 88: 0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 93: 0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 98: 0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 103:0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 108:0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 113:0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 118:0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 123:0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 128:0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 133:0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 138:0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 143:0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 148:0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 153:0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 158:0A68010073 正向累计流量(0A680100)累加和(73)  92.17
 * 163:B700 信号强度
 * A9 校验和
 * 16 帧尾
 * <p>
 * ----0--1--2--3--4--5--6--7--8--9--10-11-12-13-14-15-16-17-18-19-20-21-22-23-24-25-26-27-28-29-30-31-32-33-34-35-36-37-38-39-40-41-42-43-44-45-46-47-48-49-50-51
 * lg:[FE FE FE 68 10 03 22 10 18 20 06 73 00 81 96 90 5F 2B 2B 36 00 18 69 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 0A 68 01 00 73 B7 00 A9 16]
 * lg:[FE FE FE 68 10 06 02 01 07 20 06 36 00 81 96 90 5F 2B 2B 35 00 0E 01 00 00 00 00 00 00 00 00 00 24 00 00 00 00 00 00 00 00 00 00 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F 0E 01 00 00 0F A3 00 1B 16]
 *
 * @author pnoker
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyTcpServerHandler extends ChannelInboundHandlerAdapter {
    private static final String START_TAG = "fefefe";

    private static NettyTcpServerHandler nettyTcpServerHandler;

    @PostConstruct
    public void init() {
        nettyTcpServerHandler = this;
    }

    @Resource
    private DriverService driverService;
    @Resource
    private DriverContext driverContext;

    @Override
    public void channelActive(ChannelHandlerContext context) {
        log.debug("Water 188B Driver Listener({}) accept clint({})", context.channel().localAddress(), context.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        decode(context, byteBuf);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) {
        log.error(throwable.getMessage(), throwable);
        context.close();
    }

    /**
     * 自注册设备
     *
     * @param deviceName
     * @throws IOException
     */
    private void autoRegister(String deviceName) throws IOException {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("import/batch-import-188b-tcp-template.json");
        if (null == inputStream) {
            inputStream = this.getClass().getResourceAsStream("import/batch-import-188b-tcp-template.json");
        }
        List<BatchDriver> batchDrivers = JSON.parseArray(
                Dc3Util.inputStreamToString(inputStream),
                BatchDriver.class
        );
        BatchDevice batchDevice = new BatchDevice();
        batchDevice.setName(deviceName);
        batchDevice.setMulti(true);
        batchDrivers.get(0).getProfiles().get(0).getGroups().get(0).getDevices().add(batchDevice);
        nettyTcpServerHandler.driverService.batchImportBatchDriver(batchDrivers);
    }

    public void decode(ChannelHandlerContext context, ByteBuf byteBuf) {
        if (!START_TAG.equals(ByteBufUtil.hexDump(byteBuf, 0, 3))) {
            throw new ServiceException("Start Tag Invalid");
        }
        String deviceName = ByteBufUtil.hexDump(DriverUtils.byteReverse(ByteBufUtil.getBytes(byteBuf, 5, 5)));
        log.info("Receive Device({})[{}] Bytes -> {}", deviceName, context.channel().remoteAddress(), ByteBufUtil.hexDump(byteBuf).toUpperCase());

        List<PointValue> pointValues = new ArrayList<>();
        Long deviceId;
        try {
            deviceId = nettyTcpServerHandler.driverContext.getDeviceIdByName(deviceName);
        } catch (Exception ignored) {
            log.info("Auto register device : {}", deviceName);
            try {
                autoRegister(deviceName);
            } catch (IOException e) {
                log.error("Auto register device({}) error {}", deviceName, e.getMessage(), e);
                return;
            }
            deviceId = nettyTcpServerHandler.driverContext.getDeviceIdByName(deviceName);
        }

        Map<Long, Map<String, AttributeInfo>> pointInfoMap = nettyTcpServerHandler.driverContext.getDevicePointInfoMap().get(deviceId);
        for (Long pointId : pointInfoMap.keySet()) {
            PointValue pointValue = pointValueDecode(deviceId, pointId, pointInfoMap.get(pointId), byteBuf);
            pointValues.add(pointValue);
        }
        PointValue pointValue = new PointValue(deviceId, pointValues, 1, TimeUnit.HOURS);
        nettyTcpServerHandler.driverService.multiPointValueSender(pointValue);
    }

    /**
     * 解析 Point Value 数据
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @param infoMap  Point Info Map
     * @param byteBuf  ByteBuf
     * @return PointValue
     */
    public PointValue pointValueDecode(long deviceId, long pointId, Map<String, AttributeInfo> infoMap, ByteBuf byteBuf) {
        String type = DriverUtils.value(infoMap.get("type").getType(), infoMap.get("type").getValue());
        int start = DriverUtils.value(infoMap.get("start").getType(), infoMap.get("start").getValue());
        int length = DriverUtils.value(infoMap.get("length").getType(), infoMap.get("length").getValue());

        String rawValue, value;
        switch (type.toLowerCase()) {
            case Common.ValueType.HEX:
                rawValue = ByteBufUtil.hexDump(byteBuf.retainedSlice(start, length)).toUpperCase();
                value = nettyTcpServerHandler.driverService.convertValue(deviceId, pointId, rawValue);
                break;
            case Common.ValueType.SHORT:
                rawValue = String.valueOf(byteBuf.getShortLE(start));
                value = nettyTcpServerHandler.driverService.convertValue(deviceId, pointId, rawValue);
                break;
            case Common.ValueType.INT:
                rawValue = String.valueOf(DriverUtils.bytesToIntLE(ByteBufUtil.getBytes(byteBuf, start, length)));
                value = nettyTcpServerHandler.driverService.convertValue(deviceId, pointId, rawValue);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type.toLowerCase());
        }
        value = customDecode(start, value);
        log.info("DecodePointValue: hex({}),rawValue({}),convertValue({})", ByteBufUtil.hexDump(byteBuf, start, length).toUpperCase(), rawValue, value);
        return new PointValue(pointId, rawValue, value);
    }

    /**
     * 自定义报文解析，用于处理特殊位号数据
     *
     * @return PointValue
     */
    public String customDecode(int start, String value) {
        //信号强度额外处理
        if (start == 163) {
            int csq = (int) Double.parseDouble(value);
            if (csq > 0) {
                csq -= 200;
            }
            value = String.valueOf(csq);
        }
        return value;
    }

}