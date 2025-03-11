package com.moying.lvyou.Config;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import static io.vertx.core.http.impl.HttpClientConnection.log;

public class YamlConfigLoader {
  /*
  public static Future<JsonObject> load(Vertx vertx) {
    ConfigStoreOptions yamlStore = new ConfigStoreOptions()
      .setType("file")
      .setFormat("yaml")
      .setConfig(new JsonObject().put("path", "config.yml"));
    return ConfigRetriever.create(vertx,
        new ConfigRetrieverOptions().addStore(yamlStore))
      .getConfig();
  }
   */

  public static Future<JsonObject> load(Vertx vertx) {
   // String env = System.getProperty("env", "dev");
    String env = System.getProperty("env", "prod");//设置默认环境
    log.info("当前环境:"+env);
    ConfigStoreOptions yamlStore = new ConfigStoreOptions()
      .setType("file")
      .setFormat("yaml")
      .setConfig(new JsonObject().put("path", String.format("config-%s.yml", env)));
    return ConfigRetriever.create(vertx,
        new ConfigRetrieverOptions().addStore(yamlStore))
      .getConfig();
  }

}
