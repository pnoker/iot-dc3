package io.github.pnoker.center.manager.entity.query;

import io.github.pnoker.common.entity.common.Pages;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Topic Query
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "TopicQuery", description = "主题-查询")
public class TopicQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分页")
    private Pages page;

    /**
     * 主题
     */
    @Schema(description = "主题")
    private String topic;

    // 查询字段

    /**
     * 设备名称
     */
    @Schema(description = "设备名称")
    private String deviceName;
}
