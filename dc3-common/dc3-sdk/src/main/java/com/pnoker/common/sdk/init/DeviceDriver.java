package com.pnoker.common.sdk.init;

import com.pnoker.common.model.*;
import com.pnoker.common.sdk.bean.AttributeInfo;
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

    @Getter
    @Value("${spring.application.name}")
    private String serviceName;

    @Getter
    private int port;

    @Setter
    @Getter
    private long driverId;

    @Setter
    @Getter
    private Map<Long, DriverAttribute> driverAttributeMap;

    @Setter
    @Getter
    private Map<Long, Map<String, AttributeInfo>> driverInfoMap;

    @Setter
    @Getter
    private Map<Long, PointAttribute> pointAttributeMap;

    @Setter
    @Getter
    private Map<Long, Map<Long, Map<String, AttributeInfo>>> pointInfoMap;

    @Setter
    @Getter
    private Map<Long, Profile> profileMap;

    @Setter
    @Getter
    private Map<Long, Device> deviceMap;

    /**
     * profileId,(pointId,point)
     */
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
