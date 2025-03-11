package com.moying.lvyou.Handle.Web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.moying.lvyou.Config.AccessTokenManager;
import com.moying.lvyou.Config.YamlConfigLoader;
import com.moying.lvyou.utils.JsonResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static io.vertx.core.http.impl.HttpClientConnection.log;


public class QrCodeHandle {
  private static final int QR_CODE_WIDTH = 860; // 小程序码宽度
  private static HttpClient client;
  private static final int QR_CODE_SIZE = QR_CODE_WIDTH / 3; // 二维码大小为小程序码宽度的1/3

  // 定义静态内部类Config
  @Getter
  private static class Config {
    private final String tokenUrl; // 取得token的url
    private final String qrcodeUrl; // 取得二维码的url
    private final String qrcodePath; // 二维码保存的路径
    private final String wxappPath; // 扫码进入小程序的路径

    public Config(String tokenUrl, String qrcodeUrl, String qrcodePath, String wxappPath) {
      this.tokenUrl = tokenUrl;
      this.qrcodeUrl = qrcodeUrl;
      this.qrcodePath = qrcodePath;
      this.wxappPath = wxappPath;
    }
  }

  // 生成小程序码
  public static void CreateQrCode(RoutingContext request, Vertx vertx) {
    client = vertx.createHttpClient();
    loadConfig(vertx).onSuccess(config -> {
      var pid = request.pathParams().get("pid"); // 景点ID

      if (pid == null || !pid.matches("\\d+")) {
        sendResponse(request, JsonResult.fail("无效的参数"));
        return;
      }

      // 场景参数
      String scene = "pid_" + pid;
      // 指定小程序码保存的路径
      String qrCodePath = Paths.get(config.getQrcodePath(), "qrcode_" + pid + ".png").toString();

      // 获取 access token
      getAccessToken(config.getTokenUrl(), accessToken -> {
        //log.info("dddddd:"+config.getTokenUrl());
        if (accessToken == null) {
          sendResponse(request, JsonResult.fail("获取access token失败"));
          return;
        }

        // 生成小程序码并保存到指定路径
        generateQrCode(config.getQrcodeUrl(), accessToken, config.getWxappPath(), scene, qrCodePath, isGenerated -> {
          if (!isGenerated) {
            sendResponse(request, JsonResult.fail("生成小程序码失败"));
            return;
          }

          // 返回生成的小程序码路径
          sendResponse(request, JsonResult.success("ok", qrCodePath));
        });
      });
    }).onFailure(throwable -> sendResponse(request, JsonResult.fail("配置加载失败: " + throwable.getMessage())));
  }

  private static void sendResponse(RoutingContext request, JsonResult<String> jsonResult) {
    //log.info(Json.encodePrettily(jsonResult));
    request.response().putHeader("content-type", "application/json; charset=utf-8").end(Json.encodePrettily(jsonResult));
  }

  private static Future<Config> loadConfig(Vertx vertx) {
    Promise<Config> promise = Promise.promise();
    YamlConfigLoader.load(vertx).onComplete(ar -> {
      if (ar.succeeded()) {
        JsonObject config = ar.result();
        String tokenUrl = config.getJsonObject("wx").getString("token_url");
        String qrcodeUrl = config.getJsonObject("wx").getString("qrcode_url");
        String qrcodePath = config.getJsonObject("wx").getString("qrcode_path");
        String wxappPath = config.getJsonObject("wx").getString("app_path");
        promise.complete(new Config(tokenUrl, qrcodeUrl, qrcodePath, wxappPath));
      } else {
        log.error("Failed to load configuration: " + ar.cause().getMessage());
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }

  public static void getAccessToken(String tokenUrl, Handler<String> handler) {
    String _accessToken = AccessTokenManager.getAccessToken();
    if(_accessToken!=null&&!_accessToken.isEmpty()){
      log.info("从缓存中取得accessToken");
      handler.handle(_accessToken);
    }else{
      AccessTokenManager.fetchAccessToken(client,tokenUrl,handler);
    }
  }

  public static void generateQrCode(String qrcodeUrl, String accessToken, String path, String scene, String filePath, Handler<Boolean> handler) {
    JsonObject requestBody = new JsonObject().put("path", path).put("check_path", false).put("width", QR_CODE_WIDTH).put("scene", scene).put("env_version", "trial");//(只有正式版才能取到scene)默认:正式版 release，体验版为 trial，开发版为 develop
    RequestOptions requestOptions = new RequestOptions().setAbsoluteURI(qrcodeUrl + accessToken).setMethod(HttpMethod.POST);
    client.request(requestOptions, ar -> {
      if (ar.succeeded()) {
        HttpClientRequest request = ar.result();
        // 发送请求并处理响应
        request.send(Json.encode(requestBody), ar1 -> {
          if (ar1.succeeded()) {
            HttpClientResponse response = ar1.result();
            response.bodyHandler(buffer -> {
              try {
                // 确保目标目录存在
                Path targetPath = Paths.get(filePath);
                Path parentDir = targetPath.getParent();
                if (parentDir != null) {
                  //noinspection ResultOfMethodCallIgnored
                  new File(parentDir.toString()).mkdirs();
                }
                // 检查响应状态码
                int statusCode = response.statusCode();
                if (statusCode != 200) {
                  log.error("Failed to generate QR code: HTTP status code " + statusCode);
                  handler.handle(false);
                  return;
                }
                //小程序码生成要小程序发布后才能生成(不然找不到页面,出现错误:{"errcode":41030,"errmsg":"invalid page rid: 67c87eb2-62950908-00bac178"})
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                  fos.write(buffer.getBytes());
                  //handler.handle(false);
                  log.info("小程序码保存到: " + filePath);
                  // 生成二维码并合并到小程序码
                  BufferedImage miniProgramQrCode = ImageIO.read(new File(filePath));
                  if (miniProgramQrCode == null) {
                    log.error("Failed to read the mini program QR code image from file: " + filePath);
                    handler.handle(false);
                    return;
                  }
                  BufferedImage qrCode;
                  try {
                    qrCode = generateQRCodeImage(URLEncoder.encode(scene, StandardCharsets.UTF_8));
                  } catch (WriterException e) {
                    throw new RuntimeException(e);
                  }
                  BufferedImage combinedImage = combineImages(miniProgramQrCode, qrCode);
                  saveImage(combinedImage, filePath);
                  handler.handle(true);
                }

              } catch (IOException e) {
                handler.handle(false);
                log.error("Failed to save QR code: " + e.getMessage());
              }
            });
          } else {
            handler.handle(false);
            log.error("Request failed: " + ar1.cause().getMessage());
          }
        });
      } else {
        handler.handle(false);
        log.error("Request setup failed: " + ar.cause().getMessage());
      }
    });
  }
  /**
   * 生成二维码图片
   */
  private static BufferedImage generateQRCodeImage(String content) throws WriterException {
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    Map<EncodeHintType, Object> hints = new HashMap<>();
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);
    return MatrixToImageWriter.toBufferedImage(bitMatrix);
  }

  /**
   * 合并小程序码和二维码
   */
  private static BufferedImage combineImages(BufferedImage miniProgramQrCode, BufferedImage qrCode) {
    int width = miniProgramQrCode.getWidth();
    int height = miniProgramQrCode.getHeight();
    BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = combinedImage.createGraphics();

    // 绘制小程序码
    g2d.drawImage(miniProgramQrCode, 0, 0, null);
    int offsetX = 20; // 向左移动20像素
    int offsetY = 20; // 向上移动20像素
    // 在右下角绘制二维码
    g2d.drawImage(qrCode, width - QR_CODE_SIZE-offsetX, height - QR_CODE_SIZE-offsetY, null);

    g2d.dispose();
    return combinedImage;
  }

  /**
   * 保存图片到文件
   */
  private static void saveImage(BufferedImage image, String outputPath) throws IOException {
    File outputFile = new File(outputPath);
    ImageIO.write(image, "png", outputFile);
  }
}
