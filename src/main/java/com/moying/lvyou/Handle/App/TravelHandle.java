package com.moying.lvyou.Handle.App;

import com.moying.lvyou.Config.AccessTokenManager;
import com.moying.lvyou.Config.YamlConfigLoader;
import com.moying.lvyou.Entity.Travel;
import com.moying.lvyou.utils.*;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.Getter;

import java.text.MessageFormat;
import java.util.ArrayList;

import static com.moying.lvyou.utils.DbHelp.*;
import static io.vertx.core.http.impl.HttpClientConnection.log;

public class TravelHandle {
  private static HttpClient httpClient;

  // 定义静态内部类Config
  @Getter
  private static class Config {
    private final String tokenUrl; // 取得token的url
    private final String phoneUrl; // 取得取手机号的url

    public Config(String tokenUrl, String qrcodeUrl) {
      this.tokenUrl = tokenUrl;
      this.phoneUrl = qrcodeUrl;
    }
  }

  // 列出所有我参与的活动
  public static void List(RoutingContext request, Pool client) {
    // 调用 Jwt.getSubFromToken 获取 sub
    Jwt.getSubFromToken(request).onComplete(ar -> {
      if (ar.succeeded()) {
        String sub = ar.result();
        if (sub != null) {
          var userid = Integer.parseInt(sub.substring(5));//用户ID
          client.preparedQuery("SELECT t.*,r.name as roadname,r.roadpic,r.pic FROM travel t left join road r on t.rid=r.rid where t.isshow=1  and t.uid=? order by t.isfinish,t.createtime desc ").execute(Tuple.of(userid), ar1 -> handleQueryResult(request, true, ar1));
        } else {
          sendErrorResponse(request, JsonResult.fail("用户未登录"));
        }
      }
    });
  }

  //我参与并已完成的旅行活动列表(用于排名)
  public static void Ranking(RoutingContext request, Pool client) {
    var rid = Integer.parseInt(request.pathParam("rid"));
    log.info("rid:" + rid);
    // 调用 Jwt.getSubFromToken 获取 sub
    Jwt.getSubFromToken(request).onComplete(ar -> {
      if (ar.succeeded()) {
        String sub = ar.result();
        if (sub != null) {
          var userid = Integer.parseInt(sub.substring(5));//用户ID
          log.info("userid:" + userid);
          //首先检测此活动我有没有参与
          client.preparedQuery("SELECT rid FROM travel  where isshow=1 and isfinish=1 and uid=? order by tid desc").execute(Tuple.of(userid), ar1 -> {
            if (ar1.succeeded()) {
              if (ar1.result().size() > 0) {
                var myrid = rid;
                if (myrid == 0) {
                  myrid = ar1.result().iterator().next().getInteger("rid");//取得我最后一次活动的线路id
                }
                client.preparedQuery("SELECT * FROM travel where isshow=1 and isfinish=1 and rid=? order by speed ").execute(Tuple.of(myrid), ar2 -> handleQueryResult(request, true, ar2));
              } else {
                sendErrorResponse(request, JsonResult.success(new JsonArray()));
              }
            } else {
              sendErrorResponse(request, JsonResult.fail(ar1.cause().getLocalizedMessage()));
            }
          });
        } else {
          sendErrorResponse(request, JsonResult.fail("用户未登录"));
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
    var sql = "SELECT t.*,r.name as roadname from travel t left join road r on t.rid=r.rid";
    // language=SQL
    var countsql = "select count(1) as records from travel t left join road r on t.rid=r.rid";
    StringBuilder where = new StringBuilder(" where 1=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by t.tid desc" : " order by " + pageParms.getOrderby();//排序
    //下面重组动态查询
    Tuple tuple = Tuple.tuple();
    if (queryParms != null) {
      for (String key : queryParms.keySet()) {
        if (!queryParms.get(key).isEmpty()) {
          if (key.equals("userkeyword")) {
            where.append(" and u.name like ?");
            tuple.addString("%" + queryParms.get(key) + "%");
            where.append(" and u.tel like ?");
            tuple.addString("%" + queryParms.get(key) + "%");
            where.append(" and u.cardno like ?");
            tuple.addString("%" + queryParms.get(key) + "%");
          }
          if (key.equals("rid")) {
            where.append(" and t.rid = ?");
            tuple.addString(queryParms.get(key));
          }
          if (key.equals("startDate")) {
            where.append(" and t.createtime >= ?");
            tuple.addString(queryParms.get(key));
          }
          if (key.equals("endDate")) {
            where.append(" and t.createtime <= ?");
            tuple.addString(queryParms.get(key));
          }
          if (key.equals("status")) {
            //未审核
            if (Integer.parseInt(queryParms.get(key)) == 1) {
              where.append(" and t.isactive = ?");
              tuple.addString("0");
            }
            //未开始
            if (Integer.parseInt(queryParms.get(key)) == 2) {
              where.append(" and t.isactive=1 and t.isticket = ?");
              tuple.addString("0");
            }
            //进行中
            if (Integer.parseInt(queryParms.get(key)) == 3) {
              where.append(" and t.isactive=1 and t.isticket =1 and t.isfinish =?");
              tuple.addString("0");
            }
            //已完成
            if (Integer.parseInt(queryParms.get(key)) == 4) {
              where.append(" and t.isactive=1 and t.isticket =1 and t.isfinish =?");
              tuple.addString("1");
            }

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
    client.preparedQuery("SELECT t.*,r.name as roadname,r.roadpic, r.pic, r.content FROM travel t left join road r on t.rid=r.rid WHERE t.isshow=1 and t.tid = ?")
      .execute(Tuple.of(id), ar -> {
        if (ar.succeeded()) {
          if (ar.result().size() > 0) {
            sendResponse(request, JsonResult.success(ar.result().iterator().next().toJson()));
          } else {
            sendErrorResponse(request, JsonResult.fail("数据不存在"));
          }
        }
      });
  }

  public static void GetShopGoods(RoutingContext request, Pool client) {
    //log.info("GetShopGoods");
    var tid = request.pathParams().get("tid");
    if (tid == null || tid.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("SELECT r.goodsidstr FROM travel t left join road r on t.rid=r.rid WHERE t.isshow=1 and t.tid = ?")
      .execute(Tuple.of(tid), ar -> {
        if (ar.succeeded()) {
          if (ar.result().size() > 0) {
            var goodsidstr = ar.result().iterator().next().getString("goodsidstr");
            if (goodsidstr != null && !goodsidstr.isEmpty()) {
              //取得可领取物品及商家信息(in不能使用参数)
               client.query("SELECT g.name as goodsname, g.count,g.content,s.shopname,s.name,s.tel FROM goods g left join shop s on g.shopid=s.shopid WHERE g.isshow=1 and g.gid in (%s)".formatted(goodsidstr))
                .execute(ar1 -> {
                  if (ar1.succeeded()) {
                    if (ar1.result().size() > 0) {
                      log.info(ar1.result().size());
                      handleQueryResult(request, true, ar1);
                    } else {
                      sendErrorResponse(request, JsonResult.fail("该线路没有商家提供物品"));
                    }
                  } else {
                    ar1.cause().printStackTrace();
                    sendErrorResponse(request, JsonResult.fail(ar1.cause().getLocalizedMessage()));
                  }
                });
            } else {
              sendErrorResponse(request, JsonResult.fail("该线路没有商家提供物品"));
            }
          } else {
            sendErrorResponse(request, JsonResult.fail("该线路没有商家提供物品"));
          }
        } else {
          ar.cause().printStackTrace();
          sendErrorResponse(request, JsonResult.fail(ar.cause().getLocalizedMessage()));
        }
      });
  }
  //取得我已领取的物品
  public static void GetUseGoods(RoutingContext request, Pool client) {
    //log.info("GetUseGoods");
    var tid = request.pathParams().get("tid");
    if (tid == null || tid.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    // 调用 Jwt.getSubFromToken 获取 sub
    Jwt.getSubFromToken(request).onComplete(ar -> {
        if (ar.succeeded()) {
          String sub = ar.result();
          if (sub != null) {
            var userid = Integer.parseInt(sub.substring(5));//用户ID
            //获取已领取的商品
            client.preparedQuery("SELECT gs.name, gu.count, gu.createtime FROM travel t left join goods_use gu on t.tid=gu.travelid left join goods gs on gu.goodsid=gs.gid WHERE t.isshow=1 and gu.isshow and t.tid = ? and t.uid=?")
              .execute(Tuple.of(tid,userid), ar1 -> {
                if (ar1.succeeded()) {
                  handleQueryResult(request, true, ar1);
                }
              });

          }else{
            sendErrorResponse(request, JsonResult.fail("用户未登录"));
          }
        }else{
          sendErrorResponse(request, JsonResult.fail("获取用户信息失败"));
        }
      });
  }
  //取得该用户最后一次旅行活动(只要报名即可)
  public static void getLastTravel(RoutingContext request, Pool client) {
    Jwt.getSubFromToken(request).onComplete(ar -> {
      if (ar.succeeded()) {
        String sub = ar.result();
        if (sub != null) {
          var userid = Integer.parseInt(sub.substring(5));//用户ID
          //log.info("userid:" + userid);
          client.preparedQuery("SELECT t.*,r.name as roadname,r.roadpic, r.pic, r.content FROM travel t left join road r on t.rid=r.rid WHERE t.isshow=1 and t.uid = ? order by t.tid desc ")
            .execute(Tuple.of(userid), ar1 -> {
              if (ar1.succeeded()) {
                if (ar1.result().size() > 0) {
                  sendResponse(request, JsonResult.success(ar1.result().iterator().next().toJson()));
                } else {
                  sendErrorResponse(request, JsonResult.fail("暂无相关旅行活动,请先报名参与"));
                }
              }
            });
        }
      }
    });
  }


  //最后一次打卡景点示意图片
  public static void LastPostionPic(RoutingContext request, Pool client) {
    var rid = request.request().params().get("rid");
    var uid = request.request().params().get("uid");
    var tid = request.request().params().get("tid");
    if (uid == null || uid.isEmpty() || rid == null || rid.isEmpty() || tid == null || tid.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("rid,uid,tid参数不能为空"));
      return;
    }
    log.info("rid:" + rid + ",uid:" + uid + ",tid:" + tid);
    client.preparedQuery("SELECT rp.pic FROM tick_off t left join road_postion rp on t.pid=rp.pid WHERE rp.isshow=1 and rp.rid=? and t.tid = ? and uid=? order by t.tickid desc limit 1")
      .execute(Tuple.of(rid, tid, uid), ar -> {
        if (ar.succeeded()) {
          if (ar.result().size() > 0) {
            sendResponse(request, JsonResult.success(ar.result().iterator().next().toJson()));
          } else {
            sendErrorResponse(request, JsonResult.fail("数据不存在"));
          }
        }
      });
  }

  //打卡时弹出的选项
  public static void TickOffOption(RoutingContext request, Pool client) {
    var rid = request.request().params().get("rid");
    var pid = request.request().params().get("pid");
    if (rid == null || rid.isEmpty() || pid == null || pid.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("rid,pid参数不能为空"));
      return;
    }
    //log.info("rid:" + rid + ",pid:" + pid);
    //线路不同,选项应该也不同(目录没有限线路)
    client.preparedQuery("SELECT rpo.* FROM road_postion_option rpo left join road_postion rp on rp.rpid=rpo.rpid WHERE rp.isshow=1 and rpo.isshow=1  and rp.pid=? order by rpo.rpoid desc")
      .execute(Tuple.of(pid), ar -> {
        if (ar.succeeded()) {
          if (ar.result().size() > 0) {
            handleQueryResult(request, true, ar);
          } else {
            sendErrorResponse(request, JsonResult.fail("数据不存在"));
          }
        }else{
          sendErrorResponse(request, JsonResult.fail(ar.cause().getLocalizedMessage()));
        }
      });
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = request.body().asJsonObject();
    try {
      var entity = JsonUtil.deserialize(body, Travel.class);
      if (entity.getName() == null || entity.getTel().isEmpty() || entity.getCardno() == null) {
        sendErrorResponse(request, JsonResult.fail("标题及内容不能为空"));
        return;
      }
      var wxnumber = body.getString("wxnumber");//微信号(更新到个人信息)
      var ispublic = body.getString("ispublic");//是否公开(更新到个人信息)
      //log.info("wxnumber:" + wxnumber + ",ispublic:" + ispublic);
      var sex = Utils.getGenderFromIdCard(entity.getCardno());//身份证号获取性别
      // 调用 Jwt.getSubFromToken 获取 sub
      Jwt.getSubFromToken(request).onComplete(ar -> {
        if (ar.succeeded()) {
          String sub = ar.result();
          if (sub != null) {
            var userid = Integer.parseInt(sub.substring(5));//用户ID
            entity.setUid(userid);
            //检测线路
            client.preparedQuery("SELECT * FROM travel WHERE uid=? and isshow=1 and isactive=0 and rid = ?")
              .execute(Tuple.of(entity.getUid(), entity.getRid()), ar1 -> {
                if (ar1.succeeded()) {
                  if (ar1.result().size() == 0) {
                    client.preparedQuery("INSERT INTO travel set rid=?,uid=?, name=?, sex=?, cardno=?,tel=?, isshow=?,isactive=?, createTime= ?")
                      .execute(Tuple.of(
                        entity.getRid(),
                        entity.getUid(),
                        entity.getName(),
                        sex,
                        entity.getCardno(),
                        entity.getTel(),
                        1,
                        0,//测试时使用,正式环境为0
                        Utils.getNowDateTime()), ar2 -> {
                        if (ar2.succeeded()) {
                          //log.info("报名成功,下面更新个人信息");
                          //更新个人信息(name,tel,ispublic,wxnumber,ismember[报名就成为会员,也可以在后台审核后再设为会员])
                          client.preparedQuery("UPDATE user set ismember=1, name=?,tel=?,ispublic=?,wxnumber=? WHERE userid=?")
                            .execute(Tuple.of(
                                entity.getName(),
                                entity.getTel(),
                                ispublic,
                                wxnumber,
                                userid), ar3 -> {
                                if (ar3.succeeded()) {
                                  log.info("更新个人信息成功,userid:" + userid);
                                } else {
                                  ar3.cause().printStackTrace();
                                  //log.info("更新个人信息失败"+userid+ar3.cause().getLocalizedMessage());
                                }
                                sendResponse(request, JsonResult.success());
                              }
                            );
                        } else {
                          ar2.cause().printStackTrace();
                          sendErrorResponse(request, JsonResult.fail(ar.cause().getLocalizedMessage()));
                        }
                      });
                  } else {
                    sendErrorResponse(request, JsonResult.fail("该线路你也报名了,请不要重复报名"));
                  }
                }
              });
          } else {
            // 处理 sub 为空的情况
            request.response().setStatusCode(401).end("Unauthorized");
          }
        } else {
          // 处理获取 sub 失败的情况
          request.response().setStatusCode(401).end("Unauthorized: " + ar.cause().getMessage());
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
      sendErrorResponse(request, JsonResult.fail(e.getLocalizedMessage()));
    }
  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM travel WHERE tid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID参数不能为空"));
      return;
    }
    var body = request.body().asJsonObject();
    try {
      var entity = JsonUtil.deserialize(body, Travel.class);
      //检测userid是否存在
      client.preparedQuery("SELECT * FROM travel WHERE tid = ?")
        .execute(Tuple.of(entity.getTid()), ar -> {
          if (ar.succeeded()) {
            if (ar.result().size() == 0) {
              sendErrorResponse(request, JsonResult.fail("用户不存在"));
            } else {
              client.preparedQuery("update travel set isactive=?,isfinish=?,isticket=?, isshow=? where tid=?")
                .execute(Tuple.of(
                  entity.getIsactive(),
                  entity.getIsfinish(),
                  entity.getIsticket(),
                  entity.getIsshow(),
                  entity.getTid()), ar1 -> handleUpdateResults(request, ar1));
            }
          }
        });
    } catch (Exception e) {
      e.printStackTrace();
      sendErrorResponse(request, JsonResult.fail(e.getLocalizedMessage()));
    }
  }

  public static void getPhoneNumber(RoutingContext request, Vertx vertx) {
    httpClient = vertx.createHttpClient();
    loadConfig(vertx).onSuccess(config -> {
      var code = request.body().asJsonObject().getString("code"); //code
      if (code == null || code.isEmpty()) {
        sendResponse(request, JsonResult.fail("code不能为空"));
        return;
      }
      // 获取 access token
      getAccessToken(config.getTokenUrl(), accessToken -> {
        //log.info("dddddd:"+config.getTokenUrl());
        if (accessToken == null) {
          sendResponse(request, JsonResult.fail("获取access token失败"));
          return;
        }
        JsonObject requestBody = new JsonObject().put("code", code);
        RequestOptions requestOptions = new RequestOptions().setAbsoluteURI(config.phoneUrl + accessToken).setMethod(HttpMethod.POST);
        httpClient.request(requestOptions, ar -> {
          if (ar.succeeded()) {
            HttpClientRequest request1 = ar.result();
            // 发送请求并处理响应
            request1.send(Json.encode(requestBody), ar1 -> {
              if (ar1.succeeded()) {
                var response = ar1.result();
                response.bodyHandler(buffer -> {
                  var result = buffer.toJsonObject();
                  if (result.containsKey("phone_info")) {
                    var phoneInfo = result.getString("phone_info");
                    log.info("phoneInfo:" + phoneInfo);
                    sendResponse(request, JsonResult.success(result));
                  } else {
                    sendResponse(request, JsonResult.fail("获取手机号失败"));
                  }
                });
              } else {
                sendResponse(request, JsonResult.fail("获取手机号失败"));
              }
            });
          }
        });
      });
    }).onFailure(throwable -> sendResponse(request, JsonResult.fail("配置加载失败: " + throwable.getMessage())));
  }

  private static Future<Config> loadConfig(Vertx vertx) {
    Promise<Config> promise = Promise.promise();
    YamlConfigLoader.load(vertx).onComplete(ar -> {
      if (ar.succeeded()) {
        JsonObject config = ar.result();
        String tokenUrl = config.getJsonObject("wx").getString("token_url");
        String phoneUrl = config.getJsonObject("wx").getString("phone_url");
        promise.complete(new Config(tokenUrl, phoneUrl));
      } else {
        log.error("Failed to load configuration: " + ar.cause().getMessage());
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }

  public static void getAccessToken(String tokenUrl, Handler<String> handler) {
    String _accessToken = AccessTokenManager.getAccessToken();
    if (_accessToken != null && !_accessToken.isEmpty()) {
      log.info("从缓存中取得accessToken");
      handler.handle(_accessToken);
    } else {
      AccessTokenManager.fetchAccessToken(httpClient, tokenUrl, handler);
    }
  }
}
