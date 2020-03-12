package com.github.pnoker.common.sdk.bean;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Pattern;

/**
 * @author pnoker
 */
@Setter
@Getter
public class ScheduleConfig {
    private Boolean enable = false;
    private String corn = "* */1 * * * ?";
}
