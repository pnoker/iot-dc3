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

package io.github.pnoker.common.utils;

import cn.hutool.core.util.IdUtil;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.common.FolderConstant;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件 相关工具类
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
public class FileUtil {

    private FileUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 获取临时上传文件目录
     *
     * @return 临时上传文件目录
     */
    public static String getTempPath() {
        String path = FolderConstant.TEMP_FILE_PATH;
        if (!cn.hutool.core.io.FileUtil.exist(path) || !cn.hutool.core.io.FileUtil.isDirectory(path)) {
            cn.hutool.core.io.FileUtil.mkdir(path);
        }
        return path;
    }

    /**
     * 获取随机的xlsx文件名
     *
     * @return xlsx文件名
     */
    public static String getRandomXlsxName() {
        return IdUtil.fastSimpleUUID() + ".xlsx";
    }
}
