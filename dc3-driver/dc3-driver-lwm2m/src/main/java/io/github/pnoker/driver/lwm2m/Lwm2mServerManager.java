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

package io.github.pnoker.driver.lwm2m;

import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverSenderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LwM2M Server Manager.
 * <p>
 * Manages the embedded Leshan LwM2M server lifecycle, device registrations,
 * and provides read/write operations against registered devices.
 * Uses Leshan 2.0.0-M14 API.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Lwm2mServerManager implements DisposableBean {

    private final Lwm2mProperties lwm2mProperties;
    private final DriverSenderService driverSenderService;
    private final DriverMetadata driverMetadata;
    private final DeviceMetadata deviceMetadata;
    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();
    private LeshanServer server;

    /**
     * Start the LwM2M server and register device lifecycle listeners.
     */
    @PostConstruct
    public void start() {
        try {
            LeshanServerBuilder builder = new LeshanServerBuilder();
            server = builder.build();

            // Register device lifecycle listeners
            server.getRegistrationService().addListener(new RegistrationListener() {
                @Override
                public void registered(Registration registration, Registration previousReg,
                                       Collection<Observation> observations) {
                    String endpoint = registration.getEndpoint();
                    registrations.put(endpoint, registration);
                    log.info("LwM2M device registered: endpoint={}, id={}", endpoint, registration.getId());
                }

                @Override
                public void updated(RegistrationUpdate update, Registration updatedReg,
                                    Registration previousReg) {
                    String endpoint = updatedReg.getEndpoint();
                    registrations.put(endpoint, updatedReg);
                    log.info("LwM2M device updated: endpoint={}, id={}", endpoint, updatedReg.getId());
                }

                @Override
                public void unregistered(Registration registration, Collection<Observation> observations,
                                         boolean expired, Registration previousReg) {
                    String endpoint = registration.getEndpoint();
                    registrations.remove(endpoint);
                    log.info("LwM2M device unregistered: endpoint={}, expired={}", endpoint, expired);
                }
            });

            server.start();
            log.info("LwM2M server started on coap://{}:{} (secure port: {})",
                    lwm2mProperties.getServerHost(),
                    lwm2mProperties.getServerPort(),
                    lwm2mProperties.getSecurePort());
        } catch (Exception e) {
            log.error("Failed to start LwM2M server", e);
            throw new RuntimeException("LwM2M server startup failed", e);
        }
    }

    /**
     * Perform a LwM2M Read operation on a registered device.
     *
     * @param endpoint         device endpoint name
     * @param objectId         LwM2M Object ID
     * @param objectInstanceId LwM2M Object Instance ID
     * @param resourceId       LwM2M Resource ID
     * @return resource value as string, or null on failure
     */
    public String read(String endpoint, int objectId, int objectInstanceId, int resourceId) {
        Registration reg = registrations.get(endpoint);
        if (reg == null) {
            throw new IllegalStateException("LwM2M device not registered: " + endpoint);
        }

        try {
            ReadResponse response = server.send(reg, new ReadRequest(objectId, objectInstanceId, resourceId));
            if (response != null && response.isSuccess() && response.getContent() != null) {
                return response.getContent().toString();
            } else {
                String code = response != null ? response.getCode().toString() : "timeout";
                log.warn("LwM2M read failed: endpoint={}, path=/{}/{}/{}, code={}",
                        endpoint, objectId, objectInstanceId, resourceId, code);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("LwM2M read interrupted: endpoint={}, path=/{}/{}/{}",
                    endpoint, objectId, objectInstanceId, resourceId, e);
            return null;
        } catch (Exception e) {
            log.error("LwM2M read error: endpoint={}, path=/{}/{}/{}",
                    endpoint, objectId, objectInstanceId, resourceId, e);
            return null;
        }
    }

    /**
     * Perform a LwM2M Write operation on a registered device.
     *
     * @param endpoint         device endpoint name
     * @param objectId         LwM2M Object ID
     * @param objectInstanceId LwM2M Object Instance ID
     * @param resourceId       LwM2M Resource ID
     * @param value            value to write
     * @return true if the write succeeded
     */
    public boolean write(String endpoint, int objectId, int objectInstanceId, int resourceId, String value) {
        Registration reg = registrations.get(endpoint);
        if (reg == null) {
            throw new IllegalStateException("LwM2M device not registered: " + endpoint);
        }

        try {
            WriteResponse response = server.send(reg,
                    new WriteRequest(objectId, objectInstanceId, resourceId, value));
            if (response != null && response.isSuccess()) {
                log.debug("LwM2M write succeeded: endpoint={}, path=/{}/{}/{}",
                        endpoint, objectId, objectInstanceId, resourceId);
                return true;
            } else {
                String code = response != null ? response.getCode().toString() : "timeout";
                log.warn("LwM2M write failed: endpoint={}, path=/{}/{}/{}, code={}",
                        endpoint, objectId, objectInstanceId, resourceId, code);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("LwM2M write interrupted: endpoint={}, path=/{}/{}/{}",
                    endpoint, objectId, objectInstanceId, resourceId, e);
            return false;
        } catch (Exception e) {
            log.error("LwM2M write error: endpoint={}, path=/{}/{}/{}",
                    endpoint, objectId, objectInstanceId, resourceId, e);
            return false;
        }
    }

    /**
     * Cancel observations for a specific resource on a registered device.
     *
     * @param endpoint         device endpoint name
     * @param objectId         LwM2M Object ID
     * @param objectInstanceId LwM2M Object Instance ID
     * @param resourceId       LwM2M Resource ID
     */
    public void cancelObservation(String endpoint, int objectId, int objectInstanceId, int resourceId) {
        Registration reg = registrations.get(endpoint);
        if (reg == null) {
            return;
        }

        try {
            server.getObservationService().cancelObservations(reg,
                    "/" + objectId + "/" + objectInstanceId + "/" + resourceId);
            log.info("LwM2M observation cancelled: endpoint={}, path=/{}/{}/{}",
                    endpoint, objectId, objectInstanceId, resourceId);
        } catch (Exception e) {
            log.error("LwM2M cancel observation error: endpoint={}, path=/{}/{}/{}",
                    endpoint, objectId, objectInstanceId, resourceId, e);
        }
    }

    /**
     * Check if a device endpoint is currently registered.
     *
     * @param endpoint device endpoint name
     * @return true if the device is registered
     */
    public boolean isDeviceRegistered(String endpoint) {
        return registrations.containsKey(endpoint);
    }

    /**
     * Check if the LwM2M server is started and running.
     *
     * @return true if the server is started
     */
    public boolean isServerStarted() {
        return server != null;
    }

    /**
     * Stop the LwM2M server on Spring context close.
     */
    @Override
    public void destroy() {
        if (server != null) {
            server.destroy();
            log.info("LwM2M server destroyed on coap://{}:{}", lwm2mProperties.getServerHost(),
                    lwm2mProperties.getServerPort());
        }
    }
}
