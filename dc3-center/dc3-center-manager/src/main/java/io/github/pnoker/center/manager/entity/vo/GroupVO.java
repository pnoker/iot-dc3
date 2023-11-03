package io.github.pnoker.center.manager.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 分组 VO
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
public class GroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 父分组ID
     */
    private Long parentGroupId;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 分组排序位置
     */
    private Integer position;

    /**
     * 分组类型标识
     */
    private Byte groupTypeFlag;

    /**
     * 使能标识
     */
    private Byte enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;

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
