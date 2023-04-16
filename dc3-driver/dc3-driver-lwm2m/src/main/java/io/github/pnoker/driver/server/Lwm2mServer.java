/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.driver.server;

import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.driver.sdk.DriverContext;
import io.github.pnoker.driver.sdk.service.DriverSenderService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.response.*;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Lwm2mServer 已经实现coap方式接入 待实现coaps方式
 *
 * @author xwh1998
 */
@Component
@Slf4j
public class Lwm2mServer {


    @Value("${driver.custom.coap.port}")
    private Integer coapPort;
    @Value("${driver.custom.coaps.port}")
    private Integer coapsPort;

    private LeshanServer server;

    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverSenderService driverSenderService;

    public void startServer() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(null, coapPort);
        builder.setLocalSecureAddress(null, coapsPort);
        server = builder.build();
        server.getRegistrationService().addListener(getRegistrationListener());
        server.getObservationService().addListener(getObservationListener());
        server.start();
        log.debug("server start success:coap:{} coaps:{}", coapPort, coapsPort);
    }

    /**
     * 读取某个资源
     *
     * @param clientEndpoint
     * @param path
     * @return
     */
    public String readValueByPath(String clientEndpoint, String path) {
        Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
        try {
            ReadRequest request = new ReadRequest(ContentFormat.TEXT, path);
            ReadResponse response = server.send(registration, request);
            if (response.isSuccess()) {
                return String.valueOf(((LwM2mSingleResource) response.getContent()).getValue());
            }
        } catch (RuntimeException | InterruptedException e) {
            log.error("read exception :{},{},{}", clientEndpoint, path, e.getMessage());
        }
        return DefaultConstant.DEFAULT_VALUE;
    }


    /**
     * 更新/替换某个资源
     *
     * @param clientEndpoint
     * @param path
     * @param value
     * @param isUpdate       更新 or 替换
     * @return
     */
    public boolean writeValueByPath(String clientEndpoint, String path, String value, boolean isUpdate) {
        Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
        try {
            int rscId = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
            LwM2mNode node = LwM2mSingleResource.newStringResource(rscId, value);
            WriteRequest writeRequest = new WriteRequest(isUpdate ? WriteRequest.Mode.UPDATE : WriteRequest.Mode.REPLACE, ContentFormat.TEXT, path, node);
            WriteResponse response = server.send(registration, writeRequest);
            log.info("write res:{}", response);
            return response.isSuccess();
        } catch (RuntimeException | InterruptedException e) {
            log.error("write exception :{}", e.getMessage());
        }
        return false;
    }

    /**
     * 订阅资源
     */
    public boolean observe(String clientEndpoint, String path) {
        Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
        try {
            ObserveRequest request = new ObserveRequest(ContentFormat.TEXT, path);
            ObserveResponse response = server.send(registration, request, 5000);
            return response.isSuccess();
        } catch (RuntimeException | InterruptedException e) {
            log.error("observe exception :{}", e.getMessage());
        }
        return false;
    }

    /**
     * 取消订阅
     *
     * @param clientEndpoint
     * @param path
     */
    public void cancelObs(String clientEndpoint, String path) {
        Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
        server.getObservationService().cancelObservations(registration, path);
    }

    /**
     * 执行方法(有参or无参)
     *
     * @param clientEndpoint
     * @param path
     * @param params
     * @return
     */
    public boolean execute(String clientEndpoint, String path, String params) {
        Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
        try {
            ExecuteRequest request = new ExecuteRequest(path, params);
            ExecuteResponse response = server.send(registration, request);
            return response.isSuccess();
        } catch (RuntimeException | InterruptedException e) {
            log.error("execute exception :{}", e.getMessage());
        }
        return false;
    }


    /**
     * 订阅监听器
     *
     * @return
     */
    private ObservationListener getObservationListener() {
        return new ObservationListener() {
            @Override
            public void newObservation(Observation observation, Registration registration) {
                log.debug("new observation:{},{}", registration.getEndpoint(), getObservationPaths(observation));
            }

            @Override
            public void cancelled(Observation observation) {
                log.debug("cancel observation:{},{}", observation, getObservationPaths(observation));
            }

            @Override
            public void onResponse(SingleObservation singleObservation, Registration registration, ObserveResponse observeResponse) {
                /**
                 * 可以通过MQ发送到其他服务去消费或者入库
                 */
                log.debug("obs res:{}{},{}", registration.getEndpoint(), singleObservation.getPath(), observeResponse.getContent());
            }

            //复合订阅响应
            @Override
            public void onResponse(CompositeObservation compositeObservation, Registration registration, ObserveCompositeResponse observeCompositeResponse) {

            }

            //订阅出错
            @Override
            public void onError(Observation observation, Registration registration, Exception e) {
                log.debug("observation error:{},{}", getObservationPaths(observation), e.getMessage());
            }
        };
    }

    /**
     * 注册监听器
     *
     * @return
     */
    public RegistrationListener getRegistrationListener() {
        return new RegistrationListener() {
            /**
             * 设备注册
             * @param registration
             * @param previousReg
             * @param previousObservations
             */
            @Override
            public void registered(Registration registration, Registration previousReg, Collection<Observation> previousObservations) {
                log.debug("new device {} registered", registration.getEndpoint());
                driverSenderService.deviceStatusSender(registration.getEndpoint(), DeviceStatusEnum.ONLINE);
            }

            /**
             * 设备心跳包
             * @param registrationUpdate
             * @param updatedRegistration
             * @param registration1
             */
            @Override
            public void updated(RegistrationUpdate registrationUpdate, Registration updatedRegistration, Registration registration1) {
                log.debug("device is still here:{}", updatedRegistration.getEndpoint());
                driverSenderService.deviceStatusSender(updatedRegistration.getEndpoint(), DeviceStatusEnum.ONLINE);
            }

            /**
             * 设备离线
             * @param registration
             * @param observations
             * @param expired
             * @param newReg
             */
            @Override
            public void unregistered(Registration registration, Collection<Observation> observations, boolean expired, Registration newReg) {
                log.debug("device left: " + registration.getEndpoint());
                driverSenderService.deviceStatusSender(registration.getEndpoint(), DeviceStatusEnum.OFFLINE);
            }


        };
    }

    private String getObservationPaths(final Observation observation) {
        String path = null;
        if (observation instanceof SingleObservation) {
            path = ((SingleObservation) observation).getPath().toString();
        } else if (observation instanceof CompositeObservation) {
            path = ((CompositeObservation) observation).getPaths().toString();
        }
        return path;
    }
}
