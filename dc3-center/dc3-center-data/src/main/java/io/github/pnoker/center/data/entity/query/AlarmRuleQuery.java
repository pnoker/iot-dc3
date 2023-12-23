package io.github.pnoker.center.data.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.constant.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.entity.common.Pages;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

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
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "AlarmRule", description = "报警规则")
public class AlarmRuleQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "分页")
    private Pages page;
    private String ruleName;
    /**
     * 位号ID
     */
    @Schema(description = "位号ID")
    private Long pointId;

    /**
     * 报警类型标识
     */
    @Schema(description = "报警类型标识")
    private AlarmTypeFlagEnum alarmTypeFlag;

    /**
     * 报警规则
     */
    @Schema(description = "报警规则")
    private String alarmRule;

    /**
     * 使能标识
     */
    @Schema(description = "使能标识")
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID")
    private Long tenantId;
}
