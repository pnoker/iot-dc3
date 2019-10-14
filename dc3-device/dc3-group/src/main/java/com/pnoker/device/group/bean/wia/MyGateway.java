/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.pnoker.device.group.bean.wia;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Wia Gateway信息
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Data
@Slf4j
public class MyGateway {
    /* 网关基本信息 */
    private long id;
    private String ipAddress;
    private int localPort;
    private int port;

    /* 报文信息 */
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramReceive;
    private DatagramPacket datagramSend;
    private byte[] buff = new byte[1024];
    private byte[] sendCode = {(byte) 0x01, (byte) 0x0B, (byte) 0xFF, (byte) 0xFF, (byte) 0x4A, (byte) 0x9B};

    /* 其他信息 */
    private Map<String, MyHartDevice> hartDeviceMap = new HashMap<>(32);
    private Map<String, String> addressMap = new HashMap<>(32);

    public MyGateway(long id, String ipAddress, int localPort, int port, List<MyHartDevice> myHartDeviceList) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.localPort = localPort;
        this.port = port;
        myHartDeviceList.forEach(myHartDevice -> hartDeviceMap.put(myHartDevice.getLongAddress(), myHartDevice));
    }

    public void initialized() {
        log.info("Initializing myGateway [id:{},ipAddress:{},port:{},localPort:{}]", id, ipAddress, port, localPort);
        try {
            this.datagramSend = new DatagramPacket(sendCode, sendCode.length, InetAddress.getByName(ipAddress), port);
            this.datagramReceive = new DatagramPacket(buff, 1024);
            this.datagramSocket = new DatagramSocket(localPort);
            this.datagramSocket.setSoTimeout(1000 * 5);
        } catch (SocketException e) {
            log.error("init datagram socket fail", e);
        } catch (UnknownHostException e) {
            log.error("unknow gateway host", e);
        }
    }
}
