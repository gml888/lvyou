package com.moying.lvyou;

import io.vertx.ext.web.RoutingContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.vertx.core.http.impl.HttpClientConnection.log;

public class FileUploadHandle {
  // 定义上传目录（建议使用绝对路径）
  private static final String UPLOAD_DIR = "uploads/";
  // 时间格式模板
  private static final DateTimeFormatter FORMATTER =
    DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
  // 日期格式模板
  private static final DateTimeFormatter DATE_FORMATTER =
    DateTimeFormatter.ofPattern("yyyyMMdd");
  // 添加以下字段控制文件类型
  private static final Map<String, String> ALLOWED_MAGIC_NUMBERS = new HashMap<>();
  // MIME 类型到扩展名的映射
  private static final Map<String, String> MIME_TYPE_TO_EXTENSION = new HashMap<>();

  static {
    ALLOWED_MAGIC_NUMBERS.put("ffd8ffe0", "image/jpeg");
    ALLOWED_MAGIC_NUMBERS.put("ffd8ffe1", "image/jpeg");
    ALLOWED_MAGIC_NUMBERS.put("89504e47", "image/png");
    ALLOWED_MAGIC_NUMBERS.put("52494646", "image/webp");

    MIME_TYPE_TO_EXTENSION.put("image/jpeg", ".jpg");
    MIME_TYPE_TO_EXTENSION.put("image/png", ".png");
    MIME_TYPE_TO_EXTENSION.put("image/webp", ".webp");
  }

  public void handleFileUpload(RoutingContext rc) {
    try {
      // 获取当前日期
      String currentDate = LocalDateTime.now().format(DATE_FORMATTER);
      // 创建日期子目录
      String dateDir = UPLOAD_DIR + currentDate + "/";
      new File(dateDir).mkdirs();

      List<String> savedFiles = new ArrayList<>();

      rc.fileUploads().forEach(upload -> {
        String originalFileName = upload.fileName();
        String contentType = upload.contentType();
        String magicNumber = getMagicNumber(upload.uploadedFileName());
        //log.info("Original file name: " + originalFileName);
        //log.info("Uploaded file path: " + upload.uploadedFileName());
       // log.info("Content type: " + contentType);
       // log.info("Magic number: " + magicNumber);

        if (ALLOWED_MAGIC_NUMBERS.containsKey(magicNumber)) { // 类型匹配
          // 根据 MIME 类型获取文件扩展名
          String fileExtension = MIME_TYPE_TO_EXTENSION.get(contentType);
          if (fileExtension == null || fileExtension.isEmpty()) {
            fileExtension = getFileExtensionFromFileName(originalFileName);
          }

          // 生成带时间戳的新文件名
          String newFileName = generateUniqueFileName(fileExtension);
          String destPath = dateDir + newFileName;

          // 重命名并移动文件
          File tempFile = new File(upload.uploadedFileName());
          File destFile = new File(destPath);

          if (tempFile.renameTo(destFile)) {
            savedFiles.add(destPath); // 保存完整路径
            //log.info("File saved: " + destPath);
          } else {
            log.error("文件重命名失败: " + originalFileName);
            // 删除临时文件
            deleteTempFile(tempFile);
          }
        } else {
          // 类型不匹配，删除临时文件
          log.error("文件类型不对: " + originalFileName);
          File tempFile = new File(upload.uploadedFileName());
          deleteTempFile(tempFile);
        }
      });

      if (savedFiles.isEmpty()) {
        sendResponse(rc, "{\"error\":\"未保存任何文件\"}");
        return;
      }

      // 返回结果
      sendResponse(rc, buildResponseJson(savedFiles));
    } catch (Exception e) {
      rc.fail(500, e);
    }
  }

  private void sendResponse(RoutingContext rc, String message) {
    if (!rc.response().ended()) {
      rc.response()
        .setStatusCode(200)
        .putHeader("Content-Type", "application/json")
        .end(message);
    }
  }

  private String generateUniqueFileName(String extension) {
    String timestamp = LocalDateTime.now().format(FORMATTER);
    String uniqueId = UUID.randomUUID().toString().substring(0, 8); // 使用前8位UUID作为唯一标识
    return timestamp + "_" + uniqueId + extension;
  }

  private String buildResponseJson(List<String> fileNames) {
    if (fileNames.isEmpty()) return "{\"error\":\"未保存任何文件\"}";

    StringBuilder json = new StringBuilder("{\"files\":[");
    fileNames.forEach(name -> json.append("\"").append(name).append("\","));
    json.deleteCharAt(json.length() - 1).append("]}");
    return json.toString();
  }

  private String getMagicNumber(String filePath) {
    try (FileInputStream fis = new FileInputStream(filePath)) {
      byte[] buffer = new byte[4];
      fis.read(buffer);
      StringBuilder sb = new StringBuilder();
      for (byte b : buffer) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (IOException e) {
      log.error("读取文件头失败: " + filePath, e);
      return "";
    }
  }

  private void deleteTempFile(File tempFile) {
    if (tempFile != null && tempFile.exists()) {
      if (tempFile.delete()) {
        log.info("临时文件已删除: " + tempFile.getAbsolutePath());
      } else {
        log.error("无法删除临时文件: " + tempFile.getAbsolutePath());
      }
    }
  }

  private String getFileExtensionFromFileName(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return "";
    }
    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex == -1) {
      return "";
    }
    return fileName.substring(lastDotIndex);
  }
}
