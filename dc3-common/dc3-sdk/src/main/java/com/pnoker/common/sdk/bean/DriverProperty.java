package com.pnoker.common.sdk.bean;

import com.pnoker.common.model.DriverAttribute;
import com.pnoker.common.model.PointAttribute;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 驱动配置文件 driver 字段内容
 *
 * @author pnoker
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "driver")
public class DriverProperty {
    private String name;
    private String description;
    private List<DriverAttribute> driverAttribute;
    private List<PointAttribute> pointAttribute;
}
