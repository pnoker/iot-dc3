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

package io.github.pnoker.common.entity.ext;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User Ext
 * <p>
 * Extended information related to user social accounts.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User social extension object, holding social account information embedded inside user VO extension fields")
public class UserSocialExt extends BaseExt {

    /**
     * Extended content.
     * <p>
     * The content can be distinguished by Type and Version.
     */
    @Schema(description = "Extended content, distinguished by Type and Version")
    private Content content;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Social account content carrying the user's various social platform identifiers")
    public static class Content {

        /**
         * WeChat account.
         */
        @Schema(description = "WeChat account")
        private String wechat;

        /**
         * QQ
         */
        @Schema(description = "QQ account")
        private String qq;

        /**
         * Lark (Feishu) account.
         */
        @Schema(description = "Lark (Feishu) account")
        private String lark;

        /**
         * DingTalk account.
         */
        @Schema(description = "DingTalk account")
        private String dingTalk;

        /**
         * Social home page URL.
         */
        @Schema(description = "Social home page URL", example = "https://example.com/user/home")
        private String homeUrl;

    }

}
