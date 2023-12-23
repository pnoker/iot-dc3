package io.github.pnoker.center.data.entity.bo;

import io.github.pnoker.common.base.BaseBO;
import io.github.pnoker.common.constant.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

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
@SuperBuilder
@NoArgsConstructor
public class AlarmRuleBO extends BaseBO {

    private String ruleName;

    /**
     * 位号ID
     */
    private Long pointId;

    /**
     * 报警类型标识
     */
    private AlarmTypeFlagEnum alarmTypeFlag;

    /**
     * 报警规则
     */
    private String alarmRule;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;
}
