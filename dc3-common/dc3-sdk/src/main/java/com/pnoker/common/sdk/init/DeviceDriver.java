package com.pnoker.common.sdk.init;

import com.pnoker.common.model.Device;
import com.pnoker.common.model.Profile;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Component
public class DeviceDriver implements ApplicationListener<WebServerInitializedEvent> {

    @Setter
    @Getter
    private long id;

    @Getter
    @Value("${driver.name}")
    private String name;

    @Getter
    @Value("${spring.application.name}")
    private String serviceName;

    @Getter
    private int port;

    @Setter
    @Getter
    private List<Profile> profiles;

    @Setter
    @Getter
    private List<Device> devices;

    @Getter
    @Value("${driver.description}")
    private String description;

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
