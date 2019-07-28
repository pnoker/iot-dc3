package com.pnoker.device.virtual.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FileReaderUtils {

  public File[] getFiles(final String filePathRoot) {
    File[] files = null;
    try {
      File file = ResourceUtils.getFile(filePathRoot);

      if (file.isDirectory()) {
        files = file.listFiles();
      }

    } catch (FileNotFoundException e) {
      log.error("read mock data from {} error", filePathRoot, e);
      return null;
    }
    return files;
  }

  public List<String> readLine(final String filePathRoot) throws IOException {

    List<String> lines = new ArrayList<>();

    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(new FileInputStream(filePathRoot))); //
    } catch (IOException e) {
      System.out.println("获取" + filePathRoot + "的BufferedReader 时出现异常");
    }
    String hostsFileTextLine = null;
    while ((hostsFileTextLine = br.readLine()) != null) {
      lines.add(hostsFileTextLine);
    }
    return lines;
  }
}
