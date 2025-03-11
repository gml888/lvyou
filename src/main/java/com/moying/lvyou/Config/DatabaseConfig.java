package com.moying.lvyou.Config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class DatabaseConfig {
  public static Pool init(Vertx vertx, JsonObject config) {
    JsonObject dbConfig = config.getJsonObject("vertx").getJsonObject("db");

    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
      .setHost(dbConfig.getString("host"))
      .setPort(dbConfig.getInteger("port"))
      .setDatabase(dbConfig.getString("database"))
      .setUser(dbConfig.getString("user"))
      .setPassword(dbConfig.getString("password"));

    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(5); // 连接池配置

    return Pool.pool(vertx, connectOptions, poolOptions);
  }
}
