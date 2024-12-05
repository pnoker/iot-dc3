/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.manager.entity.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.github.pnoker.common.entity.ext.JsonExt;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
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

    @Serial
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
    private Byte pointTypeFlag;

    /**
     * 读写标识
     */
    @TableField("rw_flag")
    private Byte rwFlag;

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
     * 模版ID
     */
    @TableField("profile_id")
    private Long profileId;

    /**
     * 报警通知模版ID
     */
    @TableField("alarm_notify_profile_id")
    private Long alarmNotifyProfileId;

    /**
     * 报警信息模版ID
     */
    @TableField("alarm_message_profile_id")
    private Long alarmMessageProfileId;

    /**
     * 分组ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 位号拓展信息
     */
    @TableField(value = "point_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt pointExt;

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
     * 逻辑删除标识, 0:未删除, 1:已删除
     */
    @TableLogic
    @TableField(value = "deleted")
    private Byte deleted;
}
