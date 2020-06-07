package com.dc3.center.auth.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 用户登录限制
 *
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserLimit {
    /**
     * 登录验证错误次数
     */
    private Integer times;

    /**
     * 限制失效时间
     */
    private Date expireTime;
}
