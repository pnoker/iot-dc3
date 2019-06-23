package com.pnoker.device.group.wia;

import com.pnoker.common.bean.wia.HartDevice;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
public class ReceiveThread implements Runnable {
    private Map<String, HartDevice> hartDeviceMap = new HashMap<>(10);

    public ReceiveThread(List<HartDevice> hartDeviceList) {
        hartDeviceList.forEach(hartDevice -> hartDeviceMap.put(hartDevice.getLongAddress(), hartDevice));
    }

    @Override
    public void run() {

        String hexDatagram = "";
        String dataHead = "";

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
