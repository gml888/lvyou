package com.moying.lvyou.Handle.App;

import com.moying.lvyou.Entity.PostionPic;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;

public class PostionPicHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT * FROM postion_pic order by pid desc limit 100").execute(ar -> handleQueryResult(request,true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select * from postion_pic";
    // language=SQL
    var countsql = "select count(1) as records from postion_pic";
    StringBuilder where = new StringBuilder(" where 1=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by pid desc" : " order by " + pageParms.getOrderby();//排序
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
    client.preparedQuery("SELECT * FROM postion_pic WHERE pid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request, false,ar));
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, PostionPic.class);
    /*
    pid        int          default 0  not null comment '景点id',
    title      varchar(255) default '' not null comment '图片名称',
    pic        varchar(255) default '' not null comment '图片路径',
    sn         int          default 0  not null comment '编号',
     */
    client.preparedQuery("INSERT INTO postion_pic set pid=?, title=?,pic=?,sn= ?,createtime= ?")
      .execute(Tuple.of(
        entity.getPid(),
        entity.getTitle(),
        entity.getPic(),
        entity.getSn(),
        Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM postion_pic WHERE pid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, PostionPic.class);
    client.preparedQuery("update postion_pic set pid=?, title=?,pic=?,sn= ? where pid=?")
      .execute(Tuple.of(
        entity.getPid(),
        entity.getTitle(),
        entity.getPic(),
        entity.getSn()
      ), ar -> handleUpdateResults(request, ar));
  }
}
