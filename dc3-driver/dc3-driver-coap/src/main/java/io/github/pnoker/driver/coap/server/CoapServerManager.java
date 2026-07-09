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

package io.github.pnoker.driver.coap.server;

import io.github.pnoker.driver.coap.entity.property.CoapProperties;
import io.github.pnoker.driver.coap.server.resource.DataResource;
import io.github.pnoker.driver.coap.service.CoapReceiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * CoAP Server Manager
 * <p>
 * Manages the CoAP server lifecycle for SERVER and BOTH modes.
 * Starts the server on application startup and shuts it down on context close.
 *
 * @author pnoker
 * @version 2026.5.0
 * @since 2026.5.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoapServerManager implements CommandLineRunner {

    private final CoapProperties coapProperties;
    private final CoapReceiveService coapReceiveService;
    private CoapServer coapServer;

    @Override
    public void run(String... args) {
        CoapProperties.ModeEnum mode = coapProperties.getMode();
        if (mode != CoapProperties.ModeEnum.SERVER && mode != CoapProperties.ModeEnum.BOTH) {
            log.info("CoAP server mode disabled, current mode: {}", mode);
            return;
        }

        Configuration configuration = new Configuration();
        configuration.set(CoapConfig.COAP_PORT, coapProperties.getServerPort());
        configuration.set(CoapConfig.EXCHANGE_LIFETIME, coapProperties.getClientTimeout(), TimeUnit.MILLISECONDS);

        coapServer = new CoapServer(configuration);
        coapServer.add(new DataResource("data", coapReceiveService));

        coapServer.start();
        log.info("CoAP server started on {}:{} (mode: {})", coapProperties.getServerHost(), coapProperties.getServerPort(), mode);
    }

    /**
     * Shut down the CoAP server.
     */
    public void shutdown() {
        if (coapServer != null) {
            coapServer.destroy();
            log.info("CoAP server shut down on {}:{}", coapProperties.getServerHost(), coapProperties.getServerPort());
        }
    }
}
