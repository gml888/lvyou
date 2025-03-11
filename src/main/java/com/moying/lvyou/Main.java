package com.moying.lvyou;

import com.moying.lvyou.Config.DatabaseConfig;
import com.moying.lvyou.Config.YamlConfigLoader;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.sqlclient.Pool;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@SuppressWarnings("ALL")
public class Main extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(Main.class);
  private Pool client;

  public static void main(String[] args) {
    //var aa=Uni.createFrom().item(1).onItem().transform(i -> i + 1);
    //aa.subscribe().with(System.out::println);
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new Main());
    // Launcher.executeCommand("run", MainVerticle.class.getName());
  }

  @Override
  public void start(Promise<Void> startPromise) {
    // 获取当前vertx实例
    Vertx vertx = this.getVertx();

//    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
//      .setPort(3306)
//      .setHost("localhost")
//      .setDatabase("quarkus1")
//      .setUser("root")
//      .setPassword("admin888");
//    PoolOptions poolOptions = new PoolOptions().setMaxSize(500);
//    // 初始化数据库连接
//    this.client = Pool.pool(vertx, connectOptions, poolOptions);
    // 创建HttpServer
    HttpServer server = vertx.createHttpServer();
    YamlConfigLoader.load(vertx).compose(config -> {
      //log.info(config); //true
      this.client = DatabaseConfig.init(this.vertx, config);
      // 创建路由对象
      Router mainRouter = Router.router(vertx);//主路由
      // 添加 CorsHandler 配置
      CorsHandler corsHandler = CorsHandler.create("*")
        .allowedMethod(HttpMethod.GET)
        .allowedMethod(HttpMethod.POST)
        .allowedMethod(HttpMethod.PUT)
        .allowedMethod(HttpMethod.DELETE)
        .allowedMethod(HttpMethod.OPTIONS)
        .allowedHeader("Content-Type")
        .allowedHeader("Authorization")
        .exposedHeader("X-Custom-Header")
        .allowCredentials(true);
      mainRouter.route().handler(corsHandler);
      mainRouter.route().handler(BodyHandler.create());
      //mainRouter.route("/*");
      Router webapi = Router.router(this.vertx);//wepabi子路由
      //webapi.route("/webapi/*");
      Router api = Router.router(this.vertx);//api子路由
      //api.route("/api/*");
      Api.init(this.vertx,this.client, api);
      Webapi.init(this.vertx,this.client, webapi);
      mainRouter.get("/").handler(request -> request.response().putHeader("Content-Type", "application/json").end(new JsonObject().put("msg", "/").encodePrettily()));

      // 配置静态文件路由，指向 uploads 目录
      mainRouter.route("/uploads/*").handler(StaticHandler.create("uploads"));
      // 配置上传路由
      mainRouter.post("/webapi/upload").handler(BodyHandler.create().setUploadsDirectory("temp_uploads") // 临时目录
          .setMergeFormAttributes(true).setBodyLimit(10 * 1024 * 1024) // 10MB限制
          .setDeleteUploadedFilesOnEnd(true)) // 处理结束后删除临时文件
        .handler(new FileUploadHandle()::handleFileUpload);

      // 配置上传路由
      mainRouter.post("/api/upload").handler(BodyHandler.create().setUploadsDirectory("temp_uploads") // 临时目录
          .setMergeFormAttributes(true).setBodyLimit(10 * 1024 * 1024) // 10MB限制
          .setDeleteUploadedFilesOnEnd(true)) // 处理结束后删除临时文件
        .handler(new FileUploadHandle()::handleFileUpload);

      // 配置获取水果列表路由
      // webapi.get("/list").handler(request -> new FruitHandle().getFruitList(request, this.client));
      // api.get("/list1").handler(request -> new FruitHandle().getFruitList1(request, this.client));

      mainRouter.route("/api/*").subRouter(api);
      mainRouter.route("/webapi/*").subRouter(webapi);
      //mainRouter.route().handler(ErrorHandler.create(vertx));
      mainRouter.route().handler(ErrorHandler.create(this.vertx, "NoFind.html", true));
//      mainRouter.route().failureHandler(routingContext -> {
//        routingContext.response().setStatusCode(500).end(JsonObject.of("code", 500, "msg", "服务器出错了!").encodePrettily());
//      });
      // 把请求交给路由处理
      server.requestHandler(mainRouter);
      // 启动HTTP服务器
      server.listen(8888, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          log.info("HTTP服务监听端口:8888");
        } else {
          this.client.close();
          startPromise.fail(http.cause());
          log.error("HTTP服务启动失败!", http.cause());
        }
      });
      return testConnection(this.client);
    });
  }

  private Future<Void> testConnection(Pool client) {
    // 读取文件
//    vertx.fileSystem().readFile("config.yml", result -> {
//      if (result.succeeded()) {
//        System.out.println(result.result());
//      } else {
//        System.err.println("读取文件失败: " + result.cause());
//      }
//    });
    return client.query("SELECT 1").execute().onSuccess(rs -> System.out.println("数据库连接成功")).onFailure(err -> System.out.println("数据库连接失败: " + err.getMessage())).mapEmpty();
  }
}
