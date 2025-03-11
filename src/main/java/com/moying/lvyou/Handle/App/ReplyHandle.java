package com.moying.lvyou.Handle.App;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.moying.lvyou.Entity.Reply;
import com.moying.lvyou.utils.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static com.moying.lvyou.utils.DbHelp.*;
import static io.vertx.core.http.impl.HttpClientConnection.log;

public class ReplyHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT * FROM reply order by rid desc limit 100").execute(ar -> handleQueryResult(request, true, ar));
  }

  //回复分页
  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select r.rid,r.content,r.zans, r.zanlist ,r.createtime,u.nickname,u.headface from reply r left join user u on r.uid=u.userid";
    // language=SQL
    var countsql = "select count(1) as records from reply r left join user u on r.uid=u.userid";
    StringBuilder where = new StringBuilder(" where r.isshow=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by r.rid desc" : " order by " + pageParms.getOrderby();//排序
    //下面重组动态查询
    Tuple tuple = Tuple.tuple();
    if (queryParms != null) {
      for (String key : queryParms.keySet()) {
        if (!queryParms.get(key).isEmpty()) {
          if (key.equals("infoid")) {
            where.append(" and r.infoid = ?");
            tuple.addString(queryParms.get(key));
          }
          if (key.equals("kind")) {
            where.append(" and r.kind = ?");
            tuple.addString(queryParms.get(key));
          }
        }
      }
    }
    //log.info("where:" + where);
    Jwt.getSubFromToken(request).onComplete(ar -> {
      if (ar.succeeded()) {
        var sub = ar.result();
        if (sub != null) {
          var userid = Integer.parseInt(sub.substring(5));//用户ID
          client.preparedQuery(MessageFormat.format("{0}{1}{2}", countsql, where, orderby)).execute(tuple, ar1 -> {
            if (ar.succeeded()) {
              var total = ar1.result().iterator().next().getInteger("records");//取得记录数
              //取得数据列表
              var limit = "  limit " + pageSize * (pageIndex - 1) + "," + pageSize;
              client.preparedQuery(MessageFormat.format("{0}{1}{2}{3}", sql, where, orderby, limit)).execute(tuple, ar2 -> {
                if (ar2.succeeded()) {

                  var rslist = new ArrayList<JsonObject>();
                  ar2.result().forEach(rs -> {
                    //检测当前用户是否点赞过该回复
                    var temprs = rs.toJson();
                    String zanlistString = temprs.getString("zanlist");
                    if (zanlistString == null || zanlistString.isEmpty()) {
                      zanlistString = "[]";
                    }
                    Gson gson = new Gson();
                    JsonArray zanlist = gson.fromJson(zanlistString, JsonArray.class);
                    temprs.put("iszan", false);
                    for (JsonElement element : zanlist) {
                      var uid = element.getAsJsonObject().get("uid").getAsInt();
                      if (uid == userid) {
                        temprs.put("iszan", true);
                        break;
                      }
                    }
                    temprs.remove("zanlist");
                    rslist.add(temprs);
                  });
                  var result = PageResult.success(rslist, pageIndex, pageSize, total);
                  sendResponse(request, result);
                } else {
                  ar2.cause().printStackTrace();
                  sendErrorResponse(request, JsonResult.fail(ar2.cause().getLocalizedMessage()));
                }
              });
            } else {
              ar1.cause().printStackTrace();
              sendErrorResponse(request, JsonResult.fail(ar1.cause().getLocalizedMessage()));
            }
          });

        }
      }
    });


  }

  public static void Get(RoutingContext request, Pool client) {
    var id = request.pathParams().get("id");
    var rid = Integer.parseInt(id);
    if (rid > 0) {
      client.preparedQuery("SELECT r.*,u.nickname FROM reply r left join user u on r.uid=u.userid WHERE r.rid = ?")
        .execute(Tuple.of(id), ar -> handleQueryResult(request, false, ar));
    } else {
      //显示该用户最后回复的一条信息
      Jwt.getSubFromToken(request).onComplete(ar -> {
        if (ar.succeeded()) {
          var sub = ar.result();
          if (sub != null) {
            var userid = Integer.parseInt(sub.substring(5));//用户ID
            client.preparedQuery("SELECT r.*,u.nickname FROM reply r left join user u on r.uid=u.userid WHERE r.uid = ? order by r.rid desc limit 1")
              .execute(Tuple.of(userid), ar1 -> handleQueryResult(request, false, ar1));
          }
        }
      });
    }
  }

  //回复bbs或enlist
  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, Reply.class);
    Jwt.getSubFromToken(request).onComplete(ar -> {
      if (ar.succeeded()) {
        var sub = ar.result();
        if (sub != null) {
          var userid = Integer.parseInt(sub.substring(5));//用户ID
          client.preparedQuery("INSERT INTO reply set uid=?, kind=?, infoid=?,content=?,isshow= ?,zans=?,zanlist=?,createtime= ?")
            .execute(Tuple.of(
              userid,
              entity.getKind(),
              entity.getInfoid(),
              entity.getContent(),
              1,
              0,
              "[]",
              Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
        }
      }
    });
  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM reply WHERE rid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  //只用于修改点赞操作(点赞或取消点赞)
  public static void Put(RoutingContext request, Pool client) {
    var id = request.pathParams().get("id");
    var body = request.body().asJsonObject();
    var act = body.getString("act");// add or del
    var rid = body.getInteger("rid");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    if (Integer.parseInt(id) != rid) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    //取得该条回复信息
    client.preparedQuery("SELECT * FROM reply WHERE isshow=1 and rid = ?")
      .execute(Tuple.of(id), ar -> {
        if (ar.succeeded()) {
          var reply = ar.result().iterator().next();
          if (reply != null) {
            var entity = Json.decodeValue(Json.encode(reply.toJson()), Reply.class);
            //log.info(entity);
            var ref = new Object() {
              String zanlist = entity.getZanlist();
            };
            if (ref.zanlist.isEmpty()) {
              ref.zanlist = "[]";
            }
            AtomicReference<Integer> zans = new AtomicReference<>(entity.getZans());
            Jwt.getSubFromToken(request).onComplete(ar1 -> {
              if (ar1.succeeded()) {
                var sub = ar1.result();
                if (sub != null) {
                  var userid = Integer.parseInt(sub.substring(5));//用户ID
                  Gson gson = new Gson();
                  JsonArray zanlist = gson.fromJson(ref.zanlist, JsonArray.class);
                  if (act.equals("add")) {//点赞
                    if(!zanlist.isEmpty()){
                      for (JsonElement element : zanlist) {
                        var uid =element.getAsJsonObject().get("uid").getAsInt();
                        if (uid == userid) {
                          sendErrorResponse(request, JsonResult.fail("已经点赞过了"));
                          return;
                        }
                      }
                    }
                    zans.set(zans.get() + 1);
                    var  jsonstr="{uid:" + userid + ",time:'" + Utils.getNowDateTime() + "'}";
                    zanlist.add(new Gson().fromJson(jsonstr, JsonElement.class));
                    ref.zanlist = Arrays.toString(zanlist.asList().toArray());
                  } else {//取消点赞
                    if(!zanlist.isEmpty()){
                      for (JsonElement element : zanlist) {
                        var uid =element.getAsJsonObject().get("uid").getAsInt();
                        if (uid == userid) {
                          zans.set(zans.get() - 1);
                          zanlist.remove(element);
                          break;
                        }
                      }
                    }
                    if(zanlist.isEmpty()){
                      ref.zanlist = "[]";
                    }else{
                      ref.zanlist = Json.encode(zanlist);
                    }
                  }
                  client.preparedQuery("update reply set zans=?, zanlist=? where rid=?")
                    .execute(Tuple.of(
                      zans,
                      ref.zanlist,
                      rid
                    ), ar2 -> handleUpdateResults(request, ar2));
                }
              }
            });
          }
        }
      });
  }
}
