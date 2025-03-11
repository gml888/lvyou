package com.moying.lvyou.Handle.App;

import com.moying.lvyou.Entity.Road;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.Jwt;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;

public class RoadHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT rid,name,roadpic,pic FROM road where isshow=1 order by rid desc limit 100").execute(ar1 -> handleQueryResult(request, true,ar1));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select * from road";
    // language=SQL
    var countsql = "select count(1) as records from road";
    StringBuilder where = new StringBuilder(" where 1=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by rid desc" : " order by " + pageParms.getOrderby();//排序
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
    Jwt.getSubFromToken(request).onComplete(ar0 -> {
        if (ar0.succeeded()) {
          var sub = ar0.result();
          if (sub != null) {
            var userid = Integer.parseInt(sub.substring(5));//用户ID
            client.preparedQuery("SELECT * FROM road WHERE isshow=1 and rid = ?")
              .execute(Tuple.of(id), ar -> {
                if (ar.succeeded()) {
                  var result = ar.result();
                  if (result.size() > 0) {
                    var row=result.iterator().next().toJson();
                    //查询用户是否有正在参与的活动,如果有返回tid
                    client.preparedQuery("SELECT tid FROM travel WHERE isshow=1 and isfinish=0 and uid = ?")
                      .execute(Tuple.of(userid), ar1 -> {
                        if (ar1.succeeded()) {
                          var result1 = ar1.result();
                          if (result1.size() > 0) {
                            row.put("tid", result1.iterator().next().getInteger("tid"));
                            sendResponse(request, JsonResult.success(row));
                          } else {
                            row.put("tid", 0);
                            sendResponse(request, JsonResult.success(row));
                          }
                        }else{
                          ar1.cause().printStackTrace();
                          sendErrorResponse(request, JsonResult.fail(ar1.cause().getLocalizedMessage()));
                        }
                      });
                  } else {
                    sendErrorResponse(request, JsonResult.fail("未找到该记录"));
                  }
                } else {
                  ar.cause().printStackTrace();
                  sendErrorResponse(request, JsonResult.fail());
                }
              });
          }else{
            sendErrorResponse(request, JsonResult.fail("token无效"));
          }
        }else{
          sendErrorResponse(request, JsonResult.fail("token无效"));
        }
      });


  }

  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, Road.class);

    /*
    name       varchar(50)  default '' not null comment '名称',
    pic        varchar(200) default '' not null comment '线路图片',
    roadpic    varchar(200) default '' not null comment '线路简图',
    content    text                    null comment '线路介绍',
    createtime datetime                not null on update CURRENT_TIMESTAMP comment '创建时间'
     */
    client.preparedQuery("INSERT INTO road set name=?, pic=?,roadpic=?,content= ?,createtime= ?")
      .execute(Tuple.of(
        entity.getName(),
        entity.getPic(),
        entity.getRoadpic(),
        entity.getContent(),
        Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM road WHERE rid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, Road.class);
    client.preparedQuery("update road set name=?, pic=?,roadpic=?,content= ? where rid=?")
      .execute(Tuple.of(
        entity.getName(),
        entity.getPic(),
        entity.getRoadpic(),
        entity.getContent()
      ), ar -> handleUpdateResults(request, ar));
  }
}
