package com.pnoker.device.group.bean.wia;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Wia 网关信息
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
        try {
            this.datagramSend = new DatagramPacket(sendCode, sendCode.length, InetAddress.getByName(ipAddress), port);
            this.datagramReceive = new DatagramPacket(buff, 1024);
            this.datagramSocket = new DatagramSocket(localPort);
            this.datagramSocket.setSoTimeout(1000 * 60 * 3);
        } catch (SocketException e) {
            log.error("init datagram socket fail", e);
        } catch (UnknownHostException e) {
            log.error("unknow gateway host", e);
        }
    }
}
