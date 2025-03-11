package com.moying.lvyou.Handle.App;

import com.moying.lvyou.Entity.Bbs;
import com.moying.lvyou.utils.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;
import java.util.ArrayList;

import static com.moying.lvyou.utils.DbHelp.*;

public class BbsHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT * FROM bbs order by bid desc limit 100").execute(ar -> handleQueryResult(request, true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select b.bid,b.title,b.pic,b.hot,b.rank,b.createtime,u.nickname,u.headface from bbs b left join user u on b.userid=u.userid ";
    // language=SQL
    var countsql = "select count(1) as records from bbs b left join user u on b.userid=u.userid";
    StringBuilder where = new StringBuilder(" where b.isshow=1 ");
    var orderby = "order by b.rank desc, b.hot desc, b.bid desc";//排序
    //下面重组动态查询
    Tuple tuple = Tuple.tuple();
    if (queryParms != null) {
      for (String key : queryParms.keySet()) {
        if (!queryParms.get(key).isEmpty()) {
          if (key.equals("keyword")) {
            where.append(" and (b.title like ? or b.content like ?)");
            tuple.addString("%" + queryParms.get(key) + "%");
            tuple.addString("%" + queryParms.get(key) + "%");
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
        client.preparedQuery(MessageFormat.format("{0}{1}{2}{3}", sql, where, orderby, limit)).execute(tuple, ar1 -> {
          if (ar1.succeeded()) {
            var rslist = new ArrayList<JsonObject>();
            ar1.result().forEach(rs -> rslist.add(rs.toJson()));
            var result = PageResult.success(rslist, pageIndex, pageSize, total);
            //检测当前用户是否可以发贴
            Jwt.getSubFromToken(request).onComplete(ar0 -> {
                if (ar0.succeeded()) {
                  var sub = ar0.result();
                  if (sub != null) {
                    var userid = Integer.parseInt(sub.substring(5));//用户ID
                    //检测当前用户是否是会员
                    client.preparedQuery("SELECT ismember FROM user WHERE ismember=1 and userid=?")
                      .execute(Tuple.of(userid), ar2 -> {
                        if (ar2.succeeded()) {
                          if (ar2.result().size() == 0) {
                            result.setMsg("no");
                          }
                          sendResponse(request, result);
                        }else{
                          sendErrorResponse(request, JsonResult.fail(ar2.cause().getLocalizedMessage()));
                        }
                      });
                  }else{
                    sendErrorResponse(request, JsonResult.fail("用户未登录"));
                  }
                }else{
                  sendErrorResponse(request, JsonResult.fail(ar0.cause().getLocalizedMessage()));
                }
              });


          }else{
            ar1.cause().printStackTrace();
            sendErrorResponse(request, JsonResult.fail(ar1.cause().getLocalizedMessage()));
          }
        });
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
    //显示该用户最后回复的一条信息
    Jwt.getSubFromToken(request).onComplete(ar0 -> {
        if (ar0.succeeded()) {
          var sub = ar0.result();
          if (sub != null) {
            var userid = Integer.parseInt(sub.substring(5));//用户ID
            client.preparedQuery("SELECT b.*,u.headface,u.nickname,u.tel,u.wxnumber,u.ispublic FROM bbs b left join user u on b.userid=u.userid WHERE b.isshow=1 and b.bid = ?")
              .execute(Tuple.of(id), ar -> {
                if (ar.succeeded()) {
                  if (ar.result().size() > 0) {
                    var bbs = ar.result().iterator().next().toJson();
                    client.preparedQuery("SELECT * FROM user WHERE userid=?")
                      .execute(Tuple.of(userid), ar1 -> {
                        if (ar1.succeeded()) {
                          if (ar1.result().size() > 0) {
                            bbs.put("canreply", ar1.result().iterator().next().getInteger("ismember"));//是否可以回复
                            sendResponse(request, JsonResult.success(bbs));
                          }
                        }
                      });
                  }else{
                    sendErrorResponse(request, JsonResult.fail(ar.cause().getLocalizedMessage()));
                  }
                }else{
                  ar.cause().printStackTrace();
                  sendErrorResponse(request, JsonResult.fail(ar.cause().getLocalizedMessage()));
                }
              });
          }else{
            sendErrorResponse(request, JsonResult.fail("用户未登录"));
          }
        }else{
          sendErrorResponse(request, JsonResult.fail(ar0.cause().getLocalizedMessage()));
        }
      });




  }

  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, Bbs.class);
    if (entity.getTitle() == null || entity.getTitle().isEmpty() || entity.getContent() == null || entity.getContent().isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("标题及内容不能为空"));
      return;
    }
    Jwt.getSubFromToken(request).onComplete(ar -> {
      if (ar.succeeded()) {
        var sub = ar.result();
        if (sub != null) {
          var userid = Integer.parseInt(sub.substring(5));//用户ID
          //检测标题是否存在
          client.preparedQuery("SELECT bid FROM bbs WHERE isshow=1 and title = ? and userid=?")
            .execute(Tuple.of(entity.getTitle(), userid), ar1 -> {
              if (ar1.succeeded()) {
                if (ar1.result().size() == 0) {
                  client.preparedQuery("INSERT INTO bbs set userid=?,cid=?, title=?,pic=?,piclist=?,content=?,createTime= ?,isshow=?")
                    .execute(Tuple.of(
                      userid,
                      1,
                      entity.getTitle(),
                      entity.getPic(),
                      "",
                      entity.getContent(),
                      Utils.getNowDateTime(),
                      1), ar2 -> handleInsertResult(request, ar2));
                }
              }
            });
        } else {
          sendErrorResponse(request, JsonResult.fail("用户未登录"));
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
    client.preparedQuery("DELETE FROM bbs WHERE bid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, Bbs.class);
    client.preparedQuery("update bbs set userid=?, title=?,pic=?,piclist=?,content=? where bid=?")
      .execute(Tuple.of(
        entity.getUserid(),
        entity.getTitle(),
        entity.getPic(),
        entity.getPiclist(),
        entity.getContent(),
        id), ar -> handleUpdateResults(request, ar));
  }
}
