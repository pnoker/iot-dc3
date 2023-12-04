package io.github.pnoker.center.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;

/**
 * LabelBind VO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@SuperBuilder
@RequiredArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "LabelBind", description = "标签绑定")
public class LabelBindVO extends BaseVO {

    /**
     * 标签ID
     */
    @Schema(description = "标签ID")
    @NotBlank(message = "Label id can't be empty", groups = {Insert.class, Update.class})
    private String labelId;

    /**
     * 实体ID
     */
    @Schema(description = "实体ID")
    @NotBlank(message = "Entity id can't be empty", groups = {Insert.class, Update.class})
    private String entityId;
}
