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

import com.zsmartsystems.zigbee.ZigBeeNetworkManager;
import com.zsmartsystems.zigbee.dongle.telegesis.ZigBeeDongleTelegesis;
import com.zsmartsystems.zigbee.serial.ZigBeeSerialPort;
import org.springframework.stereotype.Component;

/**
 * Creates the serial-backed Zigbee network manager at the hardware boundary.
 */
@Component
public class ZigbeeNetworkManagerFactory {

    public ZigBeeNetworkManager create(String serialPort, int baudRate) {
        ZigBeeSerialPort serialPortConnection = new ZigBeeSerialPort(serialPort, baudRate,
                ZigBeeSerialPort.FlowControl.FLOWCONTROL_OUT_XONOFF);
        return new ZigBeeNetworkManager(new ZigBeeDongleTelegesis(serialPortConnection));
    }
}
