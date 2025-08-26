/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.UserIdentityExt;
import io.github.pnoker.common.entity.ext.UserSocialExt;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Auth;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * User VO
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class UserVO extends BaseVO {

    /**
     * 用户昵称
     */
    @NotBlank(message = "Nick name can't be empty",
            groups = {Add.class, Auth.class})
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$",
            message = "Invalid nick name",
            groups = {Add.class, Update.class})
    private String nickName;

    /**
     * 用户名称
     */
    @NotBlank(message = "User name can't be empty",
            groups = {Add.class, Auth.class})
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$",
            message = "Invalid user name",
            groups = {Add.class, Update.class})
    private String userName;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1([3-9])\\d{9}$",
            message = "Invalid phone",
            groups = {Add.class, Update.class})
    private String phone;

    /**
     * 邮箱
     */
    @Pattern(regexp = "^[A-Za-z0-9_.-]+@[A-Za-z0-9]+\\.[A-Za-z0-9]+$",
            message = "Invalid email",
            groups = {Add.class, Update.class})
    private String email;

    /**
     * 社交相关拓展信息
     */
    private UserSocialExt socialExt;

    /**
     * 身份相关拓展信息
     */
    private UserIdentityExt identityExt;

    /**
     * 使能标识, 0:启用, 1:禁用
     */
    private Byte enableFlag;

}
