package io.github.pnoker.center.manager.entity.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 标签 VO
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
public class LabelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 标签名称
     */
    private String labelName;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 实体类型标识
     */
    private Byte entityTypeFlag;

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
