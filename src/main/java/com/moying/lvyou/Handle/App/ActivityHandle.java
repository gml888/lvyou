package com.moying.lvyou.Handle.App;

import com.moying.lvyou.Entity.Activities;
import com.moying.lvyou.utils.*;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;

public class ActivityHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT * FROM activities order by aid desc limit 100").execute(ar -> handleQueryResult(request,true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select a.title,a.aid,a.pic,a.createtime,u.nickname,u.headface from activities a left join user u on a.userid=u.userid ";
    // language=SQL
    var countsql = "select count(1) as records from activities a left join user u on a.userid=u.userid ";
    StringBuilder where = new StringBuilder(" where 1=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by a.aid desc" : " order by " + pageParms.getOrderby();//排序
    //下面重组动态查询
    Tuple tuple = Tuple.tuple();
    if (queryParms != null) {
      for (String key : queryParms.keySet()) {
        if (!queryParms.get(key).isEmpty()) {
          if (key.equals("keywrod")) {
            where.append(" and (title like ? or content like ?)");
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
    client.preparedQuery("SELECT * FROM activities WHERE aid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request,false, ar));
  }

  public static void Post(RoutingContext request, Pool client) {

    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, Activities.class);
    if (entity.getTitle() == null || entity.getTitle().isEmpty() || entity.getContent() == null || entity.getContent().isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("标题及内容不能为空"));
      return;
    }
    Jwt.getSubFromToken(request).onComplete(ar -> {
        if (ar.succeeded()) {
          var sub = ar.result();
          if (sub != null) {
            var userid = Integer.parseInt(sub.substring(5));//用户ID
            client.preparedQuery("INSERT INTO activities set userid=?, title=?,content=?,createTime= ?")
              .execute(Tuple.of(
                userid,
                entity.getTitle(),
                entity.getContent(),
                Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
          }
        }else{
          sendErrorResponse(request, JsonResult.fail("用户未登录"));
        }
      });
  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM activities WHERE aid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  //用于锁定用户
  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID参数不能为空"));
      return;
    }
    var body =request.body().asJsonObject();
    Activities entity;
    try {
      entity = JsonUtil.deserialize(body,Activities.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    client.preparedQuery("update activities set islock=?,lockuser=? where aid=?")
      .execute(Tuple.of(
        entity.getIslock(),
        entity.getLockuser(),
        id), ar -> handleUpdateResults(request, ar));
  }
}
