/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.driver.service.impl;

import com.github.xingshuangs.iot.protocol.sl651.event.ISl651MessageListener;
import com.github.xingshuangs.iot.protocol.sl651.model.SL651BodyResponse;
import com.github.xingshuangs.iot.protocol.sl651.model.SL651Response;
import com.github.xingshuangs.iot.protocol.sl651.service.SL651Server;
import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SL651-2014 hydrological telemetry driver service.
 * <p>
 * SL651 is a server-side protocol where remote hydrological monitoring
 * stations push telemetry data to a central server. This driver starts an
 * SL651 TCP server and listens for incoming station reports. Received data
 * is parsed and forwarded to the DC3 platform.
 * </p>
 * <p>
 * Unlike PLC drivers that actively connect to devices and read/write points,
 * SL651 data arrives asynchronously and unsolicited from remote stations.
 * The {@code read} and {@code write} methods are not used in this driver.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Sl651DriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;
    private final DriverSenderService driverSenderService;

    @Value("${dc3.driver.code}")
    private String driverCode;

    @Value("${dc3.driver.sl651.port:5001}")
    private int serverPort;

    @Value("${dc3.driver.sl651.pwd:0000}")
    private String serverPwd;

    private SL651Server sl651Server;

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    @Override
    public void initial() {
        log.info("Driver initializing, protocol={}", driverCode);
        startServer();
    }

    @Override
    public void schedule() {
    }

    @Override
    public void event(MetadataEventDTO metadataEvent) {
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();

        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, deviceId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());

            if (MetadataOperateTypeEnum.DELETE.equals(operateType)) {
                stopServer();
                log.info("Driver server stopped due to device delete, protocol={}, deviceId={}",
                        driverCode, metadataEvent.getId());
            }
            if (MetadataOperateTypeEnum.ADD.equals(operateType)
                    || MetadataOperateTypeEnum.UPDATE.equals(operateType)) {
                restartServer();
                log.info("Driver server restarted due to device change, protocol={}, deviceId={}",
                        driverCode, metadataEvent.getId());
            }
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            log.info("Driver metadata event received, protocol={}, metadataType={}, operateType={}, pointId={}",
                    driverCode, metadataType, operateType, metadataEvent.getId());
        }
    }

    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                               DeviceBO device, PointBO point) {
        return null;
    }

    // ------------------------------------------------------------------------
    //  server lifecycle
    // ------------------------------------------------------------------------

    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig,
                         DeviceBO device, PointBO point, WritePointValue writePointValue) {
        return false;
    }

    private void restartServer() {
        stopServer();
        startServer();
    }

    private void startServer() {
        sl651Server = new SL651Server(serverPwd);
        sl651Server.setMessageListener(new Sl651MessageListener());
        try {
            sl651Server.start(serverPort);
            log.info("Driver SL651 server started, protocol={}, port={}, pwd={}", driverCode, serverPort, serverPwd);
        } catch (Exception e) {
            log.error("Driver SL651 server start failed, protocol={}, port={}", driverCode, serverPort, e);
        }
    }

    // ------------------------------------------------------------------------
    //  message listener
    // ------------------------------------------------------------------------

    private void stopServer() {
        if (Objects.nonNull(sl651Server) && sl651Server.isAlive()) {
            try {
                sl651Server.stop();
                log.info("Driver SL651 server stopped, protocol={}", driverCode);
            } catch (Exception e) {
                log.error("Driver SL651 server stop failed, protocol={}", driverCode, e);
            }
            sl651Server = null;
        }
    }

    private class Sl651MessageListener implements ISl651MessageListener {

        @Override
        public void onMessage(byte[] bytes, SL651Response response, List<SL651BodyResponse> bodyResponses) {
            String stationAddr = bytesToHex(response.getRemoteStationAddress());
            String funcCode = bytesToHex(response.getFunctionCode());
            log.debug("Driver SL651 message received, protocol={}, stationAddr={}, funcCode={}, bodyCount={}",
                    driverCode, stationAddr, funcCode,
                    Objects.nonNull(bodyResponses) ? bodyResponses.size() : 0);

            if (bodyResponses == null || bodyResponses.isEmpty()) {
                return;
            }
            for (int i = 0; i < bodyResponses.size(); i++) {
                SL651BodyResponse body = bodyResponses.get(i);
                List<String> elements = body.getBodyElements();
                if (elements != null) {
                    log.debug("Driver SL651 body[{}], protocol={}, stationsAddr={}, elements={}",
                            i, driverCode, stationAddr, elements);
                }
            }
        }

    }

}
