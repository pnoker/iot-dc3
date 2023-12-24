package io.github.pnoker.center.data.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.constant.enums.AutoConfirmFlagEnum;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "AlarmNotifyProfile", description = "报警通知模板")
public class AlarmNotifyProfileVO extends BaseVO {

    /**
     * 自动确认标识
     */
    @Schema(description = "自动确认标识")
    private AutoConfirmFlagEnum autoConfirmFlag;

    /**
     * 通知间隔，毫秒
     */
    @Schema(description = "通知间隔，毫秒")
    private Long notifyInterval;

    /**
     * 报警通知配置
     */
    @Schema(description = "报警通知配置")
    private String alarmConfig;

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
