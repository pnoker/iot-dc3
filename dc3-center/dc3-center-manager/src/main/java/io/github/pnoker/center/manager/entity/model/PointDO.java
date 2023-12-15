package io.github.pnoker.center.manager.entity.model;

import com.baomidou.mybatisplus.annotation.*;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.constant.enums.PointTypeFlagEnum;
import io.github.pnoker.common.constant.enums.RwFlagEnum;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 设备位号表
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@TableName("dc3_point")
public class PointDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 位号名称
     */
    @TableField("point_name")
    private String pointName;

    /**
     * 位号编号
     */
    @TableField("point_code")
    private String pointCode;

    /**
     * 位号类型标识
     */
    @TableField("point_type_flag")
    private PointTypeFlagEnum pointTypeFlag;

    /**
     * 读写标识
     */
    @TableField("rw_flag")
    private RwFlagEnum rwFlag;

    /**
     * 基础值
     */
    @TableField("base_value")
    private BigDecimal baseValue;

    /**
     * 比例系数
     */
    @TableField("multiple")
    private BigDecimal multiple;

    /**
     * 数据精度
     */
    @TableField("value_decimal")
    private Byte valueDecimal;

    /**
     * 单位
     */
    @TableField("unit")
    private String unit;

    /**
     * 模板ID
     */
    @TableField("profile_id")
    private Long profileId;

    /**
     * 报警通知模板ID
     */
    @TableField("alarm_notify_profile_id")
    private Long alarmNotifyProfileId;

    /**
     * 报警信息模板ID
     */
    @TableField("alarm_message_profile_id")
    private Long alarmMessageProfileId;

    /**
     * 分组ID
     */
    @TableField("group_id")
    private Long groupId;

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
