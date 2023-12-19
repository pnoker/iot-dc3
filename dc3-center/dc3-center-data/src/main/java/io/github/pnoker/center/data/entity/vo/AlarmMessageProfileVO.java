package io.github.pnoker.center.data.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.base.BaseVO;
import io.github.pnoker.common.constant.enums.AlarmLevelFlagEnum;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "AlarmMessageProfile", description = "报警信息模板")
public class AlarmMessageProfileVO extends BaseVO {

    /**
     * 报警标题
     */
    @Schema(description = "报警标题")
    private String alarmTitle;

    /**
     * 报警等级
     */
    @Schema(description = "报警等级")
    private AlarmLevelFlagEnum alarmLevel;

    /**
     * 报警信息
     */
    @Schema(description = "报警信息")
    private String alarmContent;

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
