package io.github.pnoker.driver.server;

import io.github.pnoker.common.enums.StatusEnum;
import io.github.pnoker.common.sdk.bean.driver.DriverContext;
import io.github.pnoker.common.sdk.service.DriverService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.*;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
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
    private DriverService driverService;

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
            log.error("read exception :{}", e.getMessage());
        }
        return "nil";
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
    public String execute(String clientEndpoint, String path, String params) {
        Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
        try {
            ExecuteRequest request = new ExecuteRequest(path, params);
            server.send(registration, request);
        } catch (RuntimeException | InterruptedException e) {
            log.error("execute exception :{}", e.getMessage());
        }
        return "";
    }

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
                driverService.deviceStatusSender(registration.getEndpoint(), StatusEnum.ONLINE);
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
                driverService.deviceStatusSender(updatedRegistration.getEndpoint(), StatusEnum.ONLINE);
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
                driverService.deviceStatusSender(registration.getEndpoint(), StatusEnum.OFFLINE);
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
