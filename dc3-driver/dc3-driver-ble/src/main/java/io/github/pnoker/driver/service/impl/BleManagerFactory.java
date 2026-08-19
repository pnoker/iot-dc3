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

import org.springframework.stereotype.Component;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;

/**
 * Owns creation of the native-backed BLE manager so driver behaviour can be
 * tested without loading TinyB or requiring a Bluetooth adapter.
 */
@Component
public class BleManagerFactory {

    /**
     * Create and start a BLE manager backed by TinyB.
     *
     * @return a started manager that tolerates transport initialization failures
     */
    public BluetoothManager create() {
        return new BluetoothManagerBuilder()
                .withTinyBTransport(true)
                .withIgnoreTransportInitErrors(true)
                .withStarted(true)
                .withDiscovering(false)
                .build();
    }
}
