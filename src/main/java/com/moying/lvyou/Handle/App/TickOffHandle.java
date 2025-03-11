package com.moying.lvyou.Handle.App;

import com.moying.lvyou.Entity.TickOff;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.Jwt;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.moying.lvyou.utils.DbHelp.*;
import static io.vertx.core.http.impl.HttpClientConnection.log;

public class TickOffHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT * FROM tick_off order by tickid desc limit 100").execute(ar -> handleQueryResult(request, true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select * from tick_off";
    // language=SQL
    var countsql = "select count(1) as records from tick_off";
    StringBuilder where = new StringBuilder(" where 1=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by tickid desc" : " order by " + pageParms.getOrderby();//排序
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
    client.preparedQuery("SELECT * FROM tick_off WHERE tickid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request, false, ar));
  }

  //打卡(参数pid,lat,lng)
  public static void Post(RoutingContext request, Pool client) {
    var body = request.body().asJsonObject();
    log.info("body:" + Json.encodePrettily(body));
    var t_pid = body.getString("pid");
    var t_lat = body.getString("lat");
    var t_lng = body.getString("lng");
    if (t_pid == null || t_lat == null || t_lng == null) {
      sendErrorResponse(request, JsonResult.fail("参数不能为空"));
      return;
    }
     int mypid;
    try {
      mypid = Integer.parseInt(t_pid);
      // 继续处理
    } catch (NumberFormatException e) {
      // 处理异常情况，例如记录日志或通知用户输入无效
      log.error("无效输入: " + t_pid, e);
      sendErrorResponse(request, JsonResult.fail(Json.encodePrettily(body)));
      return;
      // 返回错误响应给客户端
    }
    var pid = mypid;
    var lat = Float.valueOf(t_lat);
    var lng = Float.valueOf(t_lng);

    //log.info("body:" + Json.encodePrettily(body));
    //调用 Jwt.getSubFromToken 获取 sub 中的userid
    Jwt.getSubFromToken(request).onComplete(ar -> {
      if (ar.succeeded()) {
        var sub = ar.result();
        if (sub != null) {
          var userid = Integer.parseInt(sub.substring(5));//用户ID
          //检测徒步线路是否存在
          //log.info("userid:" + userid);
          client.preparedQuery("SELECT tid,rid,isticket FROM travel WHERE  isshow=1 and isactive=1 and isfinish=0 and uid = ?")
            .execute(Tuple.of(userid)).onComplete(ar1 -> {
              if (ar1.succeeded()) {
                var result = ar1.result(); //取得查询结果
                if (result.size() > 0) {
                  var travel = result.iterator().next();
                  var rid = travel.getInteger("rid"); //取得徒步线路ID
                  var tid = travel.getInteger("tid"); //取得徒步活动ID
                  //取得景点的位置信息,由用户当前位置,计算出距离,看是否在打卡范围内打卡
                  //log.info("rid:" + rid);
                  client.preparedQuery("SELECT lat,lng,`range`,points FROM postion WHERE  pid=?")
                    .execute(Tuple.of(pid)).onComplete(ar2 -> {
                      if (ar2.succeeded()) {
                        var result1 = ar2.result(); //取得查询结果
                        if (result1.size() > 0) {
                          var postion = result1.iterator().next();
                          var postionlat = Float.valueOf(postion.getString("lat")); //取得位置纬度
                          var postionlng = Float.valueOf(postion.getString("lng")); //取得位置经度
                          var postionrange = postion.getInteger("range"); //取得位置范围
                          //计算用户当前位置与景点位置的距离
                          var thisdistance = Utils.getDistance(lat, lng, postionlat, postionlng);
                          //log.info("thisdistance:" + thisdistance);
                          //log.info("postionrange:" + postionrange);
                          if (thisdistance <= postionrange) { //在打卡范围内
                            //检测是否第一次打卡(入口点)
                            JsonObject tickOffData = new JsonObject();
                            tickOffData.put("userid", userid);
                            tickOffData.put("tid", tid);
                            tickOffData.put("rid", rid);
                            tickOffData.put("pid", pid);
                            tickOffData.put("lat", lat);
                            tickOffData.put("lng", lng);
                            client.preparedQuery("SELECT * FROM tick_off WHERE tid = ? and rid = ? and uid = ?")
                              .execute(Tuple.of(tid, rid, userid), ar3 -> {
                                if (ar3.succeeded()) {
                                  var result3 = ar3.result(); //取得查询结果
                                  if (result3.size() > 0) {
                                    //检测该点是否已经打卡
                                    AtomicBoolean istickoff = new AtomicBoolean(false);
                                    result3.forEach(item -> {
                                      if (Objects.equals(item.getInteger("pid"), pid)) {
                                        istickoff.set(true);
                                      }
                                    });
                                    if (istickoff.get()) {
                                      sendErrorResponse(request, JsonResult.fail("已打过了,不用重复打卡"));
                                    } else {
                                      var tickoff = result3.iterator().next();
                                      var lasttime = tickoff.getValue("createtime").toString().replace("T", " "); //取得上一个打卡点的时间
                                      var time = Utils.getTime(lasttime, Utils.getNowDateTime()); //所用时间=当前打卡的时间减去上一个打卡的时间(秒)
                                      tickOffData.put("time", time);
                                      Add(request, client, tickOffData, travel);
                                    }
                                  } else {
                                    tickOffData.put("time", 0);
                                    Add(request, client, tickOffData, travel);
                                  }
                                } else {
                                  ar3.cause().printStackTrace();
                                }
                              });
                          } else {
                            sendErrorResponse(request, JsonResult.fail("请在打卡范围内扫码打卡"));
                          }
                        }
                      } else {
                        sendErrorResponse(request, JsonResult.fail("该景点不存在或系统维护中"));
                      }
                    });
                } else {
                  sendErrorResponse(request, JsonResult.fail("您没有参与活动或报名后还未审核"));
                }
              } else {
                sendErrorResponse(request, JsonResult.fail("您没有参与活动或该景点已打过了"));
              }
            });
        }
      }
    });
  }

  public static void Add(RoutingContext request, Pool client, JsonObject tickoff, Row travel) {
    //从线路配置的景点取得距离上一个景点到该景点的距离及积分
    //log.info("tickoff:" + tickoff.encodePrettily());
    client.preparedQuery("SELECT distance,point FROM road_postion WHERE isshow=1 and rid =? and pid =? order by sn limit 1")
      .execute(Tuple.of(tickoff.getInteger("rid"), tickoff.getInteger("pid"))).onComplete(ar0 -> {
        if (ar0.succeeded()) {
          var result4 = ar0.result(); //取得查询结果
          if (result4.size() > 0) {
            var roadpostion = result4.iterator().next();
            var distance = roadpostion.getFloat("distance"); //取得距离
            var point = roadpostion.getInteger("point"); //取得积分
            tickoff.put("distance", distance);
            tickoff.put("point", point);
            log.info(tickoff);
            client.preparedQuery("INSERT INTO tick_off set uid=?, tid=?, rid=?,pid=?, points=?, distance=?,lat=?,lng=?, time=?, createtime= ?")
              .execute(Tuple.of(
                tickoff.getInteger("userid"),
                tickoff.getInteger("tid"),
                tickoff.getInteger("rid"),
                tickoff.getInteger("pid"),
                tickoff.getInteger("point"),
                tickoff.getInteger("distance"),
                tickoff.getFloat("lat"),
                tickoff.getFloat("lng"),
                tickoff.getInteger("time"),
                Utils.getNowDateTime()), ar1 -> {
                if (ar1.succeeded()) {
                  log.info(travel.getInteger("isticket"));
                  if (travel.getInteger("isticket") == 0) {
                    client.preparedQuery("UPDATE travel set isticket=? where tid=?")
                      .execute(Tuple.of(1, tickoff.getInteger("tid")), ar2 -> {
                        if (ar2.succeeded()) {
                          log.info("更新行程状态为已打卡");
                          sendResponse(request, JsonResult.success());//打卡成功
                          //因为是该活动首次打卡,不用检测是否完成,所以直接返回成功即可
                        } else {
                          ar2.cause().printStackTrace();
                        }
                      });
                  } else {
                    sendResponse(request, JsonResult.success());//打卡成功
                    //检测该打卡景点是否为该线路的最后一个景点,如果是则更新行程状态为已完成,并且要把该活动所有打卡记录的路程,时间及积分更新到行程表中
                    client.preparedQuery("SELECT sn,pid FROM road_postion WHERE isshow=1 and rid = ?  order by sn desc limit 1")
                      .execute(Tuple.of(tickoff.getInteger("rid")), ar2 -> {
                        if (ar2.succeeded()) {
                          var result = ar2.result();
                          if (result.size() > 0) {
                            var thispostion = result.iterator().next();
                            if (Objects.equals(thispostion.getInteger("pid"), tickoff.getInteger("pid"))) {
                              //循环取得打卡记录并计算路程,时间及积分
                              client.preparedQuery("SELECT sum(distance) as distance, sum(time) as time, sum(points) as points FROM tick_off WHERE rid=? and tid=?")
                                .execute(Tuple.of(tickoff.getInteger("rid"), tickoff.getInteger("tid")), ar3 -> {
                                  if (ar3.succeeded()) {
                                    var result3 = ar3.result();
                                    if (result3.size() > 0) {
                                      AtomicInteger totaldistance = new AtomicInteger();
                                      AtomicInteger totaltime = new AtomicInteger();
                                      AtomicInteger totalpoints = new AtomicInteger();
                                      result3.forEach(row -> {
                                        totaldistance.set(totaldistance.get() + row.getInteger("distance"));
                                        totaltime.set(totaltime.get() + row.getInteger("time"));
                                        totalpoints.set(totalpoints.get() + row.getInteger("points"));
                                      });
                                      // 计算速度并保留两位小数
                                      BigDecimal speed = BigDecimal.valueOf(totaldistance.get())
                                        .divide(BigDecimal.valueOf(totaltime.get()), 2, RoundingMode.HALF_UP);
                                      // 更新行程状态为已完成,同时计算出本次旅行速度方便后面排名,这里还要考虑是否完成所有景点打卡完成才更新为已完成
                                      // 现目前没有要求所有景点打卡,只要求起始点及结束点打卡
                                      // 如果要完成所有景点打卡才更新,则需要修改这里,首先得到该线路所有景点数与打卡数比较,如果相等则更新为已完成
                                      // 未完成的不参与排名,也得不到勋章
                                      client.preparedQuery("UPDATE travel set isticket=?, isfinish=?,distance=?,time=?,speed=?,points=? where tid=?")
                                        .execute(Tuple.of(1, 1, totaldistance, totaltime, speed, totalpoints, tickoff.getInteger("tid")), ar4 -> {
                                          if (ar4.succeeded()) {
                                            log.info("更新行程状态为已完成,同时计算出本次旅行速度方便后面排名");
                                            sendResponse(request, JsonResult.success());//打卡成功
                                          }
                                        });
                                    }
                                  }
                                });
                            }
                          }
                        }
                      });
                  }
                } else {
                  ar1.cause().printStackTrace();
                  sendErrorResponse(request, JsonResult.fail(ar1.cause().getLocalizedMessage()));
                }
              });
          } else {
            sendErrorResponse(request, JsonResult.fail("没有在该线路配置中找到该景点的配置信息"));
          }
        } else {
          sendErrorResponse(request, JsonResult.fail(ar0.cause().getLocalizedMessage()));
        }
      });
  }

  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM tick_off WHERE tickid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, TickOff.class);
    client.preparedQuery("update tick_off set uid=?, rid=?,pid=?, points=?, distance=?, time=? where tickid=?")
      .execute(Tuple.of(
        entity.getUid(),
        entity.getRid(),
        entity.getPid(),
        entity.getPoints(),
        entity.getDistance(),
        entity.getTime()
      ), ar -> handleUpdateResults(request, ar));
  }
}
