package io.github.pnoker.center.data.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.constant.enums.AutoConfirmFlagEnum;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.entity.common.Pages;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "AlarmNotifyProfile", description = "报警通知模板")
public class AlarmNotifyProfileQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "分页")
    private Pages page;

    // 查询字段

    /**
     * 报警通知模板名称
     */
    @Schema(description = "报警通知模板名称")
    private String alarmNotifyName;

    /**
     * 报警通知模板编号
     */
    @Schema(description = "报警通知模板编号")
    private String alarmNotifyCode;

    /**
     * 自动确认标识
     */
    @Schema(description = "自动确认标识")
    private AutoConfirmFlagEnum autoConfirmFlag;

    /**
     * 报警通知间隔，毫秒
     */
    @Schema(description = "报警通知间隔，毫秒")
    private Long alarmNotifyInterval;

    /**
     * 使能标识
     */
    @Schema(description = "使能标识")
    private EnableFlagEnum enableFlag;
}