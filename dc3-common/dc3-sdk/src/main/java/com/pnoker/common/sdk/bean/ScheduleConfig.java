package com.pnoker.common.sdk.bean;

import lombok.Getter;
import lombok.Setter;

/**
 * @author pnoker
 */
@Setter
@Getter
public class ScheduleConfig {
    private Boolean enable = false;
    private String corn = "* */1 * * * ?";
}
