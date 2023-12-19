package io.github.pnoker.center.data.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.base.BaseVO;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;

/**
 * LabelBind VO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "LabelBind", description = "标签绑定")
public class LabelBindVO extends BaseVO {

    /**
     * 标签ID
     */
    @Schema(description = "标签ID")
    @NotBlank(message = "标签ID不能为空",
            groups = {Add.class, Update.class})
    private String labelId;

    /**
     * 实体ID
     */
    @Schema(description = "实体ID")
    @NotBlank(message = "实体ID不能为空",
            groups = {Add.class, Update.class})
    private String entityId;
}
