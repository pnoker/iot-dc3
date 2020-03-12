package com.github.pnoker.common.sdk.bean;

import lombok.Getter;
import lombok.Setter;

/**
 * 驱动配置文件 driver.schedule 字段内容
 *
 * @author pnoker
 */
@Setter
@Getter
public class ScheduleProperty {
    private ScheduleConfig read;
    private ScheduleConfig custom;
}
