package io.github.pnoker.center.data.entity.bo;

import io.github.pnoker.common.base.BaseBO;
import io.github.pnoker.common.constant.enums.AlarmLevelFlagEnum;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 报警信息模板表
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AlarmMessageProfileBO extends BaseBO {

    /**
     * 报警标题
     */
    private String alarmTitle;

    /**
     * 报警等级
     */
    private AlarmLevelFlagEnum alarmLevel;

    /**
     * 报警信息
     */
    private String alarmContent;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;
}
