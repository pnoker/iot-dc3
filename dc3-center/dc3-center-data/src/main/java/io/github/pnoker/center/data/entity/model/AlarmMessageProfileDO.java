package io.github.pnoker.center.data.entity.model;

import com.baomidou.mybatisplus.annotation.*;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

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
@TableName("dc3_alarm_message_profile")
public class AlarmMessageProfileDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 报警标题
     */
    @TableField("alarm_title")
    private String alarmTitle;

    /**
     * 报警等级
     */
    @TableField("alarm_level")
    private Byte alarmLevel;

    /**
     * 报警内容
     */
    @TableField("alarm_content")
    private String alarmContent;

    /**
     * 报警内容拓展信息
     */
    @TableField("alarm_content_ext")
    private String alarmContentExt;

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
    @TableField("deleted")
    private Byte deleted;
}
