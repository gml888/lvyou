package com.moying.lvyou.Handle.Web;

import com.moying.lvyou.Entity.Reply;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.PageResult;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.moying.lvyou.utils.DbHelp.*;
import static io.vertx.core.http.impl.HttpClientConnection.log;

public class ReplyHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT * FROM reply order by rid desc limit 100").execute(ar -> handleQueryResult(request,true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select * from reply";
    // language=SQL
    var countsql = "select count(1) as records from reply";
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
        client.preparedQuery(MessageFormat.format("{0}{1}{2}{3}", sql, where, orderby, limit)).execute(tuple, ar1 -> {
          if (ar1.succeeded()) {
            //循环取得数据并根据类型查询取得活动或贴吧标题
            JsonArray jsonArray = new JsonArray();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            List<JsonObject> rows = new ArrayList<>();
            ar1.result().forEach(rs -> {
              var row = rs.toJson();
              var kind = row.getInteger("kind");
              var infoid = row.getInteger("infoid");
              var future = new CompletableFuture<Void>();

              switch (kind) {
                case 1:
                  client.preparedQuery("select title from activities where aid=?")
                    .execute(Tuple.of(infoid), ar2 -> {
                      if (ar2.succeeded()) {
                        if(ar2.result().size()>0){
                          row.put("title", ar2.result().iterator().next().getString("title"));
                        }else{
                          row.put("title", "找不到相关标题");
                        }
                      } else {
                        row.put("title", "找不到相关主题");
                      }
                      future.complete(null);
                    });
                  break;
                case 2:
                  client.preparedQuery("select title from bbs where bid=?")
                    .execute(Tuple.of(infoid), ar2 -> {
                      if (ar2.succeeded()) {
                        if(ar2.result().size()>0){
                          row.put("title", ar2.result().iterator().next().getString("title"));
                        }else{
                          row.put("title", "找不到相关标题");
                        }
                      } else {
                        row.put("title", "找不到相关主题");
                      }
                      future.complete(null);
                    });
                  break;
                default:
                  row.put("title", "找不到相关标题");
                  future.complete(null);
                  break;
              }
              futures.add(future);
              rows.add(row);
            });

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
              // 将 List<JsonObject> 转换为 JsonArray
              for (JsonObject row : rows) {
                jsonArray.add(row);
              }
              var result = PageResult.success(jsonArray, pageIndex, pageSize, total);
              sendResponse(request, result);
            }).exceptionally(ex -> {
              ex.printStackTrace();
              sendErrorResponse(request, JsonResult.fail(ex.getLocalizedMessage()));
              return null;
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
    client.preparedQuery("SELECT * FROM reply WHERE rid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request,false, ar));
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, Reply.class);

    /*
    kind       tinyint default 0 not null comment '类别:1.bbs2.活动',
    infoid     int     default 0 not null comment '所回复信息的id',
    content    text              null comment '内容',
    isshow     tinyint default 0 not null comment '是否显示',
    createtime datetime          not null on update CURRENT_TIMESTAMP comment '发布时间'
     */
    client.preparedQuery("INSERT INTO reply set kind=?, infoid=?,content=?,isshow= ?,createtime= ?")
      .execute(Tuple.of(
        entity.getKind(),
        entity.getInfoid(),
        entity.getContent(),
        entity.getIsshow(),
        Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
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

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = request.body().asJsonObject();
    client.preparedQuery("update reply set content=?,isshow= ? where rid=?")
      .execute(Tuple.of(
        body.getString("content"),
        body.getString("isshow"),
        id
      ), ar -> handleUpdateResults(request, ar));
  }
}
