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

import java.util.Map;

/**
 * JSON extension object for menu configuration metadata.
 * <p>
 * Extended information related to menus.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Schema(description = "JSON extension object for menu configuration metadata, holding extended information related to menus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuExt extends BaseExt {

    /**
     * Extended content.
     * <p>
     * The content can be distinguished by Type and Version.
     */
    @Schema(description = "Extended content, distinguished by Type and Version")
    private Content content;

    @Schema(description = "Extended content of the menu, carrying localized titles, icon, link and description")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {

        /**
         * Localized titles keyed by locale code (e.g. {@code zh}, {@code en}).
         * <p>
         * Authoritative source for the menu display name. When rendering, the UI picks
         * {@code titles[currentLocale]} with fallback to {@code titles["en"]}.
         */
        @Schema(description = "Localized titles keyed by locale code (e.g. zh, en); authoritative source for the menu display name, falling back to titles[\"en\"]")
        private Map<String, String> titles;

        /**
         * Icon.
         */
        @Schema(description = "Menu icon identifier")
        private String icon;

        /**
         * URL link.
         */
        @Schema(description = "Menu URL link")
        private String url;

        /**
         * Description.
         */
        @Schema(description = "Description")
        private String remark;

    }

}
