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

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import io.github.pnoker.driver.coap.entity.CoapMessage;
import io.github.pnoker.driver.coap.service.CoapReceiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CoAP Receive Service Implementation
 * <p>
 * Processes incoming CoAP messages by converting them to PointValue objects
 * and forwarding them to the DC3 platform.
 *
 * @author pnoker
 * @version 2026.5.0
 * @since 2026.5.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoapReceiveServiceImpl implements CoapReceiveService {

    private final DriverSenderService driverSenderService;

    @Override
    public void receiveValue(CoapMessage coapMessage) {
        log.debug("CoAP message received, from={}:{}, uri={}, payloadLength={}",
                coapMessage.getSourceAddress(), coapMessage.getSourcePort(),
                coapMessage.getUriPath(), payloadLengthOf(coapMessage));

        PointValue pointValue = JsonUtil.parseObject(coapMessage.getPayload(), PointValue.class);
        pointValue.setCreateTime(LocalDateTimeUtil.now());
        driverSenderService.pointValueSender(pointValue);

        log.debug("CoAP point value forwarded, from={}:{}, deviceId={}, pointId={}",
                coapMessage.getSourceAddress(), coapMessage.getSourcePort(),
                pointValue.getDeviceId(), pointValue.getPointId());
    }

    @Override
    public void receiveValues(List<CoapMessage> coapMessageList) {
        log.debug("CoAP message batch received, count={}", coapMessageList.size());

        List<PointValue> pointValues = coapMessageList.stream().map(coapMessage -> {
            PointValue pointValue = JsonUtil.parseObject(coapMessage.getPayload(), PointValue.class);
            pointValue.setCreateTime(LocalDateTimeUtil.now());
            return pointValue;
        }).toList();
        driverSenderService.pointValueSender(pointValues);

        log.debug("CoAP point value batch forwarded, count={}", pointValues.size());
    }

    private int payloadLengthOf(CoapMessage coapMessage) {
        String payload = coapMessage.getPayload();
        return payload == null ? 0 : payload.length();
    }

}
