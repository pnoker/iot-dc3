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
package io.github.pnoker.consumerscan;

import io.github.pnoker.common.mqtt.entity.MqttMessage;
import io.github.pnoker.common.mqtt.service.MqttReceiveService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Consumer-side receive service discovered only through component scanning, like a
 * driver module's {@code MqttReceiveService} implementation. Lives outside the common
 * MQTT packages so tests can control the scan order relative to the handler package.
 */
@Component
class ScanDiscoveredReceiveService implements MqttReceiveService {

    @Override
    public void receiveValue(MqttMessage mqttMessage) {
        // no-op for registration tests
    }

    @Override
    public void receiveValues(List<MqttMessage> mqttMessageList) {
        // no-op for registration tests
    }
}
