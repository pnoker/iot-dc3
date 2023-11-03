package io.github.pnoker.center.manager.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 标签关联 VO
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
public class LabelBindVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 标签ID
     */
    private Long labelId;

    /**
     * 实体ID
     */
    private Long entityId;

    /**
     * 描述
     */
    private String remark;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 创建者名称
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 操作者ID
     */
    private Long operatorId;

    /**
     * 操作者名称
     */
    private String operatorName;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;
}
