package com.pnoker.common.sdk.init;

import com.pnoker.common.model.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Component
public class DeviceDriver implements ApplicationListener<WebServerInitializedEvent> {

    @Setter
    @Getter
    private long driverId;

    @Getter
    @Value("${spring.application.name}")
    private String serviceName;

    @Getter
    private int port;

    @Setter
    @Getter
    private Map<Long, ConnectInfo> connectInfoMap;

    @Setter
    @Getter
    private Map<Long, ProfileInfo> profileInfoMap;

    @Setter
    @Getter
    private Map<Long, PointInfo> pointInfoMap;

    @Setter
    @Getter
    private Map<Long, Profile> profileMap;

    @Setter
    @Getter
    private Map<Long, Device> deviceMap;

    @Setter
    @Getter
    private Map<Long, Map<Long, Point>> pointMap;


    public String getHost() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            return address.getHostAddress();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent webServerInitializedEvent) {
        this.port = webServerInitializedEvent.getWebServer().getPort();
    }
}
