package com.pnoker.common.bean.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 结果返回 实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseBean {
    private boolean result = false;
    private Object message = "nothing";
}
