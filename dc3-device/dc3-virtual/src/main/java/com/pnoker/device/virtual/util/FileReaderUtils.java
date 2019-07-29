package com.pnoker.device.virtual.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
/**
 * Copyright(c) 2019. Pnoker All Rights Reserved.
 *
 * <p>Author : Charles
 *
 * <p>Email : xinguangduan@163.com
 *
 * <p>Description: 文件读取工具类
 */
@Slf4j
public final class FileReaderUtils {

  public static File[] getFiles(final String fileDirectory) {
    File[] files = null;
    try {
      File file = ResourceUtils.getFile(fileDirectory);
      if (file.isDirectory()) {
        files = file.listFiles();
      }
    } catch (FileNotFoundException e) {
      log.error("read mock data from {} error", fileDirectory, e);
      return null;
    }
    return files;
  }
}
