package io.github.pnoker.center.data.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.base.BaseVO;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.constant.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * Label VO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "Label", description = "标签")
public class LabelVO extends BaseVO {

    /**
     * 标签名称
     */
    @Schema(description = "标签名称")
    @NotBlank(message = "标签名称不能为空",
            groups = {Add.class})
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$",
            message = "标签名称格式无效",
            groups = {Add.class, Update.class})
    private String labelName;

    /**
     * 标签颜色
     */
    @Schema(description = "标签颜色")
    private String color;

    /**
     * 实体标识
     */
    @Schema(description = "实体标识")
    private EntityTypeFlagEnum entityTypeFlag;

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
