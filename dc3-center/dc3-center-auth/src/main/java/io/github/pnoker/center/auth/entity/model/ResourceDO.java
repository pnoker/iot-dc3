package io.github.pnoker.center.auth.entity.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 权限资源表
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@TableName("dc3_resource")
public class ResourceDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 权限资源父级ID
     */
    @TableField("parent_resource_id")
    private Long parentResourceId;

    /**
     * 权限资源名称
     */
    @TableField("resource_name")
    private String resourceName;

    /**
     * 权限资源编号
     */
    @TableField("resource_code")
    private String resourceCode;

    /**
     * 权限资源类型标识
     */
    @TableField("resource_type_flag")
    private Byte resourceTypeFlag;

    /**
     * 权限资源范围标识
     */
    @TableField("resource_scope_flag")
    private Byte resourceScopeFlag;

    /**
     * 权限资源实体ID
     */
    @TableField("entity_id")
    private Long entityId;

    /**
     * 使能标识
     */
    @TableField("enable_flag")
    private Byte enableFlag;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 描述
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建者ID
     */
    @TableField("creator_id")
    private Long creatorId;

    /**
     * 创建者名称
     */
    @TableField("creator_name")
    private String creatorName;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 操作者ID
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 操作者名称
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 操作时间
     */
    @TableField("operate_time")
    private LocalDateTime operateTime;

    /**
     * 逻辑删除标识,0:未删除,1:已删除
     */
    @TableLogic
    @TableField("deleted")
    private Byte deleted;
}
