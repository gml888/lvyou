package com.moying.lvyou.Handle.App;

import com.moying.lvyou.Config.YamlConfigLoader;
import com.moying.lvyou.Entity.User;
import com.moying.lvyou.utils.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.UUID;

import static com.moying.lvyou.utils.DbHelp.*;
import static io.vertx.core.http.impl.HttpClientConnection.log;

public class UserHandle {


  public static void List(RoutingContext request, Pool client) {
    // 返回当前用户信息
    Jwt.getSubFromToken(request).onComplete(ar -> {
      if (ar.succeeded()) {
        var sub = ar.result();
        if (sub != null) {
          var userid = Integer.parseInt(sub.substring(5)); // 用户ID
          // 用户信息
          Future<JsonObject> userFuture = Future.future(promise -> client.preparedQuery("SELECT * FROM user WHERE userid = ?").execute(Tuple.of(userid), ar1 -> {
            if (ar1.succeeded()) {
              RowSet<Row> result = ar1.result();
              if (result.size() > 0) {
                Row row = result.iterator().next();
                promise.complete(row.toJson());
              } else {
                promise.fail("User not found");
              }
            } else {
              promise.fail(ar1.cause());
            }
          }));

          //我参与的徒步活动
          Future<JsonArray> travelFuture = Future.future(promise -> client.preparedQuery("SELECT t.*,r.name as roadname FROM travel t left join road r on t.rid=r.rid WHERE t.uid = ?").execute(Tuple.of(userid), ar3 -> {
            if (ar3.succeeded()) {
              RowSet<Row> result = ar3.result();
              JsonArray travel = new JsonArray();
              for (Row row : result) {
                travel.add(row.toJson());
              }
              promise.complete(travel);
            } else {
              promise.fail(ar3.cause());
            }
          }));
          //我发起的组队活动
          Future<JsonArray> activityFuture = Future.future(promise -> client.preparedQuery("SELECT * FROM activities WHERE userid = ?").execute(Tuple.of(userid), ar2 -> {
            if (ar2.succeeded()) {
              RowSet<Row> result = ar2.result();
              JsonArray activities = new JsonArray();
              for (Row row : result) {
                activities.add(row.toJson());
              }
              promise.complete(activities);
            } else {
              promise.fail(ar2.cause());
            }
          }));
          //我报名的组队活动(从报名表中取得infoid)
          Future<JsonArray> MyactivityFuture = Future.future(promise ->
            client.preparedQuery("SELECT infoid FROM enlist WHERE isshow=1 and userid = ? order by eid desc ").execute(Tuple.of(userid), ar1 -> {
              if (ar1.succeeded()) {
                var ref = new Object() {
                  String idstr = "";
                };
                ar1.result().forEach(row -> {
                  if (Objects.equals(ref.idstr, "")) {
                    ref.idstr = row.getInteger("infoid").toString();
                  } else {
                    ref.idstr = ref.idstr + "," + row.getInteger("infoid").toString();
                  }
                });

                client.preparedQuery("SELECT * FROM activities WHERE aid in (?)").execute(Tuple.of(ref.idstr), ar2 -> {
                  if (ar2.succeeded()) {
                    RowSet<Row> result = ar2.result();
                    JsonArray activities = new JsonArray();
                    for (Row row : result) {
                      activities.add(row.toJson());
                    }
                    promise.complete(activities);
                  } else {
                    promise.fail(ar2.cause());
                  }
                });
              } else {
                promise.fail(ar1.cause());
              }
            }));
          // 使用 CompositeFuture 来处理多个异步查询的结果
          CompositeFuture.all(userFuture, activityFuture, travelFuture,MyactivityFuture).onComplete(ar4 -> {
            if (ar4.succeeded()) {
              JsonObject user = userFuture.result();
              JsonArray activity = activityFuture.result();
              JsonArray myactivity = MyactivityFuture.result();
              JsonArray travel = travelFuture.result();
              // 合并结果
              user.put("activity", activity);
              user.put("myactivity", myactivity);
              user.put("travel", travel);
              // 返回合并后的结果
              sendResponse(request, JsonResult.success(user));
            } else {
              sendErrorResponse(request, JsonResult.fail(ar4.cause().getLocalizedMessage()));
            }
          });
        }
      }
    });
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select * from user";
    // language=SQL
    var countsql = "select count(1) as records from user";
    StringBuilder where = new StringBuilder(" where 1=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by userid desc" : " order by " + pageParms.getOrderby();//排序
    //下面重组动态查询
    Tuple tuple = Tuple.tuple();
    if (queryParms != null) {
      for (String key : queryParms.keySet()) {
        if (!queryParms.get(key).isEmpty()) {
          if (key.equals("userid")) {
            where.append(" and userid = ?");
            tuple.addString(queryParms.get(key));
          }
        }
      }
    }
    //log.info("where:" + where);
    client.preparedQuery(MessageFormat.format("{0}{1}{2}", countsql, where, orderby)).execute(tuple, ar -> {
      if (ar.succeeded()) {
        var total = ar.result().iterator().next().getInteger("records");//取得记录数
        //取得数据列表
        var limit = "  limit " + pageSize * (pageIndex - 1) + "," + pageSize;
        client.preparedQuery(MessageFormat.format("{0}{1}{2}{3}", sql, where, orderby, limit)).execute(tuple, ar1 -> handlePageResult(request, ar1, pageIndex, pageSize, total));
      } else {
        ar.cause().printStackTrace();
        sendErrorResponse(request, JsonResult.fail(ar.cause().getLocalizedMessage()));
      }
    });
  }

  public static void Get(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("SELECT * FROM user WHERE userid = ?").execute(Tuple.of(id), ar -> handleQueryResult(request, false, ar));
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, User.class);
    client.preparedQuery("INSERT INTO user set username=?, pwd=?,openid=?, nickname=?, headface=?, name=?,tel=?,status=?, createtime= ?").execute(Tuple.of(entity.getUsername(), entity.getPwd(), entity.getOpenid(), entity.getNickname(), entity.getHeadface(), entity.getName(), entity.getTel(), entity.getStatus(), Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM user WHERE userid = $1").execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.pathParams().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = request.body().asJsonObject();
    User entity;
    try {
      entity = JsonUtil.deserialize(body, User.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    client.preparedQuery("update user set nickname=?, headface=?, tel=?, wxnumber=?, ispublic=? where userid=?")
      .execute(Tuple.of(
        entity.getNickname(),
        entity.getHeadface(),
        entity.getTel(),
        entity.getWxnumber(),
        entity.getIspublic(),
        entity.getUserid()
      ), ar -> handleUpdateResults(request, ar));
  }

  public static void Login(RoutingContext request, JWTAuth jwtAuth, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, User.class);
    var username = entity.getUsername();
    var pwd = entity.getPwd();
    if (Objects.equals(username, "") || Objects.equals(pwd, "")) {
      sendErrorResponse(request, JsonResult.fail("用户名及密码不能为空"));
      return;
    }
    client.preparedQuery("select * from user where username=? and pwd=?").execute(Tuple.of(entity.getUsername(), Utils.md5(entity.getPwd())), ar -> {
      if (ar.succeeded()) {
        if (ar.result().iterator().hasNext()) {
          var userid = ar.result().iterator().next().getInteger("userid");
          // 生成 JWT 令牌
          JsonObject payload = new JsonObject().put("sub", userid + "_" + username).put("exp", System.currentTimeMillis() + 3600000); // 1小时后过期

          String token = jwtAuth.generateToken(payload, new JWTOptions().setAlgorithm("HS256"));
          sendResponse(request, JsonResult.success("ok", token));
          //request.response().putHeader("Content-Type", "application/json").end(new JsonObject().put("token", token).encodePrettily());
        } else {
          sendErrorResponse(request, JsonResult.fail("用户名或密码错误"));
        }
      } else {
        ar.cause().printStackTrace();
        sendErrorResponse(request, JsonResult.fail(ar.cause().getLocalizedMessage()));
      }
    });
  }

  public static void WxLogin(RoutingContext request, JWTAuth jwtAuth, Pool client, Vertx vertx) {
    var code = request.body().asJsonObject().getString("code");
    //log.info("code:" + code);
    var httpclient = vertx.createHttpClient();
    if (Objects.equals(code, "")) {
      sendErrorResponse(request, JsonResult.fail("code不能为空"));
      return;
    }
    if (Objects.equals(code, "the code is a mock one")) { //测试用
      // 生成 JWT 令牌
      JsonObject payload = new JsonObject().put("sub", "user_1").put("exp", System.currentTimeMillis() + 3600000); // 1小时后过期
      String token = jwtAuth.generateToken(payload, new JWTOptions().setAlgorithm("HS256"));
      sendResponse(request, JsonResult.success("ok", token));
      return;
    }
    YamlConfigLoader.load(vertx).onComplete(ar -> {
      if (ar.succeeded()) {
        JsonObject config = ar.result();
        String loginUrl = config.getJsonObject("wx").getString("login_url") + code;
        //log.info("loginUrl:" + loginUrl);
        httpclient.request(new RequestOptions().setAbsoluteURI(loginUrl).setMethod(HttpMethod.GET), ar1 -> {
          if (ar1.succeeded()) {
            HttpClientRequest req = ar1.result();
            // 发送请求并处理响应
            req.send(ar2 -> {
              if (ar2.succeeded()) {
                ar2.result().bodyHandler(buffer -> {
                  String responseBody = buffer.toString();
                  JsonObject jsonObject = new JsonObject(responseBody);
                  String openid = jsonObject.getString("openid");
                  if (openid != null) {
                    //查找是否存在该用户,如果存在,返回token,如果不存在,创建用户并返回token
                    client.preparedQuery("select * from user where openid=?").execute(Tuple.of(openid), ar3 -> {
                      if (ar3.succeeded()) {
                        if (ar3.result().iterator().hasNext()) {
                          var userid = ar3.result().iterator().next().getInteger("userid");
                          // 生成 JWT 令牌
                          JsonObject payload = new JsonObject().put("sub", "user_" + userid).put("exp", System.currentTimeMillis() + 3600000); // 1小时后过期
                          String token = jwtAuth.generateToken(payload, new JWTOptions().setAlgorithm("HS256"));
                          sendResponse(request, JsonResult.success("ok", token));
                        } else {
                          String uniqueId = UUID.randomUUID().toString().substring(0, 8); // 使用前8位UUID作为唯一标识
                          client.preparedQuery("INSERT INTO user set username=?, openid=?,nickname=?,status=?,createtime=?").execute(
                            Tuple.of("wx_" + uniqueId, openid, "微信用户" + uniqueId, 1, Utils.getNowDateTime()), ar4 -> {
                              if (ar4.succeeded()) {
                                client.preparedQuery("SELECT userid from user where openid=?").execute(Tuple.of(openid), ar5 -> {
                                  if (ar5.succeeded()) {
                                    var userid = ar5.result().iterator().next().getInteger("userid");  // 获取插入后的自增ID
                                    // 生成 JWT 令牌
                                    JsonObject payload = new JsonObject().put("sub", "user_" + userid).put("exp", System.currentTimeMillis() + 3600000); // 1小时后过期
                                    String token = jwtAuth.generateToken(payload, new JWTOptions().setAlgorithm("HS256"));  // 使用HS256算法生成JWT令牌
                                    sendResponse(request, JsonResult.success("ok", token));    // 返回JWT令牌
                                  }
                                });
                              } else {
                                sendErrorResponse(request, JsonResult.fail(ar4.cause().getMessage()));
                              }
                            });
                        }
                      }
                    });
                  } else {
                    sendErrorResponse(request, JsonResult.fail(responseBody));
                    log.error("Failed to get openid: " + responseBody);
                  }
                });
              } else {
                sendErrorResponse(request, JsonResult.fail(ar2.cause().getMessage()));
                log.error("Request failed: " + ar2.cause().getMessage());
              }
            });
          } else {
            sendErrorResponse(request, JsonResult.fail(ar1.cause().getMessage()));
            log.error("Request failed: " + ar1.cause().getMessage());
          }
        });
      } else {
        log.error("Failed to load configuration: " + ar.cause().getMessage());
      }
    });
  }
}
