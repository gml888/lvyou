package com.moying.lvyou.Config;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

import static io.vertx.core.http.impl.HttpClientConnection.log;

public class AccessTokenManager {
  private static final ConcurrentHashMap<String, AccessToken> tokenCache = new ConcurrentHashMap<>();

  public static String getAccessToken() {
    AccessToken accessToken = tokenCache.get("AccessToken");
    long currentTime = System.currentTimeMillis();
    if (accessToken != null && currentTime < accessToken.getExpireTime()) {
      return accessToken.getToken();
    }
    return null;//超时或没有缓存
  }

  public static void fetchAccessToken(HttpClient client, String tokenUrl, Handler<String> handler) {
    log.info("没有AccessToken或过期从服务器取得并保存到缓存中");
    RequestOptions requestOptions = new RequestOptions().setAbsoluteURI(tokenUrl).setMethod(HttpMethod.GET);
    client.request(requestOptions, ar -> {
      if (ar.succeeded()) {
        HttpClientRequest request = ar.result();
        // 发送请求并处理响应
        request.send(ar1 -> {
          if (ar1.succeeded()) {
            ar1.result().bodyHandler(buffer -> {
              String responseBody = buffer.toString();
              //log.info("Response body: " + responseBody);
              JsonObject jsonObject = new JsonObject(responseBody);
              String accessToken = jsonObject.getString("access_token");
              if (accessToken != null) {
                handler.handle(accessToken);
                long currentTime = System.currentTimeMillis();
                long expireIn = 7200; // 假设 access_token 的有效期为 7200 秒
                long expireTime = currentTime + (expireIn - 200) * 1000; // 提前 200 秒刷新
                tokenCache.put("AccessToken", new AccessToken(accessToken, expireTime));
              } else {
                handler.handle(null);
                log.error("Failed to get access token: " + responseBody);
              }
            });
          } else {
            handler.handle(null);
            log.error("Request failed: " + ar1.cause().getMessage());
          }
        });
      } else {
        handler.handle(null);
        log.error("Request setup failed: " + ar.cause().getMessage());
      }
    });
  }

  @Getter
  private static class AccessToken {
    private final String token;
    private final long expireTime;

    public AccessToken(String token, long expireTime) {
      this.token = token;
      this.expireTime = expireTime;
    }

  }
}
