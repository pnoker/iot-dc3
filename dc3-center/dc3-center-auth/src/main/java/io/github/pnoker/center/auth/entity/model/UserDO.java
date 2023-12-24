package io.github.pnoker.center.auth.entity.model;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.github.pnoker.common.entity.ext.JsonExt;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@TableName("dc3_user")
public class UserDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户昵称，加密存储
     */
    @TableField("nick_name")
    private String nickName;

    /**
     * 用户名，加密存储
     */
    @TableField("user_name")
    private String userName;

    /**
     * 手机号，加密存储
     */
    @TableField("phone")
    private String phone;

    /**
     * 邮箱，加密存储
     */
    @TableField("email")
    private String email;

    /**
     * 社交相关拓展信息，加密存储
     */
    @TableField(value = "social_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt socialExt;

    /**
     * 身份相关拓展信息，加密存储
     */
    @TableField(value = "identity_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt identityExt;

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
