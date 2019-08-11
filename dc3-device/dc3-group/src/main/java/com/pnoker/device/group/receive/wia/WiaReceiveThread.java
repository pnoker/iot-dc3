package com.pnoker.device.group.receive.wia;

import com.pnoker.device.group.bean.wia.MyGateway;
import com.pnoker.device.group.util.DatagramUtils;
import com.pnoker.device.group.util.PackageProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
public class WiaReceiveThread implements Runnable {
    private MyGateway myGateway;
    private PackageProcessor p;

    @Override
    public void run() {
        byte[] receive = myGateway.getDatagramReceive().getData();
        p = new PackageProcessor(receive);
        String hexDatagram = DatagramUtils.hexDatagram(receive, receive.length);
        String dataHead = p.bytesToString(0, 1);

        switch (dataHead) {
            case "0101":
                log.info("网络报文:{}", hexDatagram);
                break;
            case "0111":
                log.info("统计报文:{}", hexDatagram);
                break;
            case "010f":
                log.info("健康报文:{}", hexDatagram);
                break;
            case "0183":
                log.info("数据报文:{}", hexDatagram);
                break;
            default:
                log.info("其他报文:{}", hexDatagram);
                break;
        }
    }
}
