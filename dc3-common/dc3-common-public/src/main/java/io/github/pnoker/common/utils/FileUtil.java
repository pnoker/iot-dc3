/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
