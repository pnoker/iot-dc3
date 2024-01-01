package io.github.pnoker.center.data.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.constant.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.AlarmRuleExt;
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "AlarmRule", description = "报警规则")
public class AlarmRuleVO extends BaseVO {

    /**
     * 报警规则名称
     */
    @Schema(description = "报警规则名称")
    private String alarmRuleName;

    /**
     * 报警规则编号
     */
    @Schema(description = "报警规则编号")
    private String alarmRuleCode;

    /**
     * 位号ID
     */
    @Schema(description = "位号ID")
    private Long pointId;

    /**
     * 报警通知模板ID
     */
    @Schema(description = "报警通知模板ID")
    private Long alarmNotifyProfileId;

    /**
     * 报警信息模板ID
     */
    @Schema(description = "报警信息模板ID")
    private Long alarmMessageProfileId;

    /**
     * 报警类型标识
     */
    @Schema(description = "报警类型标识")
    private AlarmTypeFlagEnum alarmTypeFlag;

    /**
     * 报警规则
     */
    @Schema(description = "报警规则")
    private AlarmRuleExt alarmRuleExt;

    /**
     * 使能标识
     */
    @Schema(description = "使能标识")
    private EnableFlagEnum enableFlag;
}
