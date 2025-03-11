package com.moying.lvyou.utils;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;

import java.io.IOException;
import java.io.InputStream;

import static io.vertx.core.http.impl.HttpClientConnection.log;

public class Jwt {
  public static Handler<RoutingContext> authHandler;
  public static JWTAuth jwtAuth;

  public static void init(Vertx vertx) {
    //   检查 keystore 文件是否存在(项目根目录)
    //    File keystoreFile = new File("keystore.jceks"); //部署时要把文件与jar放到一起
    //    if (!keystoreFile.exists()) {
    //      throw new RuntimeException("Keystore file not found at path: " + keystoreFile.getAbsolutePath());
    //    }
    try (InputStream keystoreStream = Jwt.class.getClassLoader().getResourceAsStream("keystore.jceks")) {
      if (keystoreStream == null) {
        log.error("Keystore file not found in classpath");
        throw new RuntimeException("Keystore file not found in classpath");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // 配置 JWTAuth
    JWTAuthOptions config = new JWTAuthOptions()
      .setKeyStore(new io.vertx.ext.auth.KeyStoreOptions()
        .setPath("keystore.jceks")
        .setPassword("secret")
        .setType("jceks")
      );
    // 初始化 JWTAuth
    jwtAuth = JWTAuth.create(vertx, config);
    // JWT 验证中间件
    authHandler = JWTAuthHandler.create(jwtAuth);
    //log.info("JWT 验证中间件已初始化");
  }

  // 解析 JWT 令牌并提取 sub 内容
  public static Future<String> getSubFromToken(RoutingContext request) {
    // 从请求头中获取JWT
    String authorizationHeader = request.request().getHeader("Authorization");
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      return Future.failedFuture("Unauthorized").map(res -> {
        request.fail(401); // Unauthorized
        return null;
      });
    }
    String token = authorizationHeader.substring(7); // 去掉 "Bearer " 前缀
    try {
      JsonObject tokenInfo = new JsonObject().put("token", token);
      //noinspection deprecation
      return jwtAuth.authenticate(tokenInfo).map(user -> {
        JsonObject payload = user.principal();
        return payload.getString("sub");
      }).otherwise(t -> {
        t.printStackTrace();
        return null;
      });
    } catch (Exception e) {
      // 处理解析错误
      e.printStackTrace();
      return Future.failedFuture(e.getMessage());
    }
  }
  // 解析 JWT 令牌并提取 sub 内容
  public static Future<String> getSubFromShopToken(RoutingContext request) {
    // 从请求头中获取JWT
    String authorizationHeader = request.request().getHeader("Authorization");
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      return Future.failedFuture("Unauthorized").map(res -> {
        request.fail(401); // Unauthorized
        return null;
      });
    }
    String token = authorizationHeader.substring(7); // 去掉 "Bearer " 前缀
    try {
      JsonObject tokenInfo = new JsonObject().put("shoptoken", token);
      //noinspection deprecation
      return jwtAuth.authenticate(tokenInfo).map(user -> {
        JsonObject payload = user.principal();
        return payload.getString("sub");
      }).otherwise(t -> {
        t.printStackTrace();
        return null;
      });
    } catch (Exception e) {
      // 处理解析错误
      e.printStackTrace();
      return Future.failedFuture(e.getMessage());
    }
  }
}
