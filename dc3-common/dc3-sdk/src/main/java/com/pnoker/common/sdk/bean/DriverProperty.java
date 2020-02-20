package com.pnoker.common.sdk.bean;

import com.pnoker.common.model.ConnectInfo;
import com.pnoker.common.model.ProfileInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author pnoker
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "driver")
public class DriverProperty {
    private String name;
    private String description;
    private List<ConnectInfo> connect;
    private List<ProfileInfo> profile;
}
