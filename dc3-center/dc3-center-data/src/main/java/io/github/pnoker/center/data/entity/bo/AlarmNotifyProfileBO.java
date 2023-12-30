package io.github.pnoker.center.data.entity.bo;

import io.github.pnoker.center.data.entity.ext.AlarmNotifyExt;
import io.github.pnoker.common.constant.enums.AutoConfirmFlagEnum;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.entity.base.BaseBO;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class AlarmNotifyProfileBO extends BaseBO {

    /**
     * 报警通知模板名称
     */
    private String alarmNotifyName;

    /**
     * 报警通知模板编号
     */
    private String alarmNotifyCode;

    /**
     * 自动确认标识
     */
    private AutoConfirmFlagEnum autoConfirmFlag;

    /**
     * 报警通知间隔，毫秒
     */
    private Long alarmNotifyInterval;

    /**
     * 报警通知模版配置
     */
    private AlarmNotifyExt alarmNotifyExt;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;
}
