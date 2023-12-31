package io.github.pnoker.center.data.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.constant.enums.AlarmMessageLevelFlagEnum;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.entity.common.Pages;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

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
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "AlarmMessageProfile", description = "报警信息模板")
public class AlarmMessageProfileQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "分页")
    private Pages page;

    // 查询字段

    /**
     * 报警信息模板标题
     */
    @Schema(description = "报警标题")
    private String alarmMessageTitle;

    /**
     * 报警信息模板编号
     */
    @Schema(description = "报警信息模板编号")
    private String alarmMessageCode;

    /**
     * 报警信息模板等级
     */
    @Schema(description = "报警信息模板等级")
    private AlarmMessageLevelFlagEnum alarmMessageLevel;

    /**
     * 使能标识
     */
    @Schema(description = "使能标识")
    private EnableFlagEnum enableFlag;
}
