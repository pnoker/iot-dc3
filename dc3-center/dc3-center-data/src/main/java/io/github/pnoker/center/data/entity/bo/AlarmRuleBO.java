package io.github.pnoker.center.data.entity.bo;

import io.github.pnoker.common.constant.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.entity.ext.AlarmRuleExt;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>
 * 报警规则表
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlarmRuleBO extends BaseBO {

    /**
     * 报警规则名称
     */
    private String alarmRuleName;

    /**
     * 报警规则编号
     */
    private String alarmRuleCode;

    /**
     * 位号ID
     */
    private Long pointId;

    /**
     * 报警通知模板ID
     */
    private Long alarmNotifyProfileId;

    /**
     * 报警信息模板ID
     */
    private Long alarmMessageProfileId;

    /**
     * 报警类型标识
     */
    private AlarmTypeFlagEnum alarmTypeFlag;

    /**
     * 报警规则
     */
    private AlarmRuleExt alarmRuleExt;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;
}
