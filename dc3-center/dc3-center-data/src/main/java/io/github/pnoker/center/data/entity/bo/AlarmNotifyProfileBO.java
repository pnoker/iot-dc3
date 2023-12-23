package io.github.pnoker.center.data.entity.bo;

import io.github.pnoker.common.base.BaseBO;
import io.github.pnoker.common.constant.enums.AutoConfirmFlagEnum;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 报警通知模板表
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AlarmNotifyProfileBO extends BaseBO {

    private String notifyProfileName;
    /**
     * 自动确认标识
     */
    private AutoConfirmFlagEnum autoConfirmFlag;

    /**
     * 通知间隔，毫秒
     */
    private Long notifyInterval;

    /**
     * 报警通知配置
     */
    private String alarmConfig;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;
}
