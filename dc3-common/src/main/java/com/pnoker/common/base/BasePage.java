package com.pnoker.common.base;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 基础查询类，其中包括分页以及排序
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasePage implements Serializable {
    private static final long serialVersionUID = 4835128943097551504L;

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Boolean order = null;

    public <T> void orderBy(QueryWrapper<T> queryWrapper) {
        if (order != null) {
            if (order) {
                queryWrapper.orderByAsc("id");
            } else {
                queryWrapper.orderByDesc("id");
            }
        }
    }
}
