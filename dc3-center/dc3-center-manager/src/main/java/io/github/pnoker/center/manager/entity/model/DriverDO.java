package io.github.pnoker.center.manager.entity.model;

import com.baomidou.mybatisplus.annotation.*;
import io.github.pnoker.common.constant.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 协议驱动表
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@TableName("dc3_driver")
public class DriverDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 驱动名称
     */
    @TableField("driver_name")
    private String driverName;

    /**
     * 驱动编号
     */
    @TableField("driver_code")
    private String driverCode;

    /**
     * 驱动服务名称
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * 服务主机
     */
    @TableField("service_host")
    private String serviceHost;

    /**
     * 驱动类型标识
     */
    @TableField("driver_type_flag")
    private DriverTypeFlagEnum driverTypeFlag;

    /**
     * 驱动拓展信息
     */
    @TableField("driver_ext")
    private String driverExt;

    /**
     * 使能标识
     */
    @TableField("enable_flag")
    private EnableFlagEnum enableFlag;

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
     * 签名
     */
    @TableField("signature")
    private String signature;

    /**
     * 版本
     */
    @TableField("version")
    private Integer version;

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
    @TableField(value = "deleted")
    private Byte deleted;
}
