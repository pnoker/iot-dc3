package com.pnoker.common.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 基础实体类
 */
@Data
public class BaseEntity implements Serializable {
    private static final long serialVersionUID = -3969004704181842099L;

    @TableId(type = IdType.AUTO)
    private long id;
}
