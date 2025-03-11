package com.moying.lvyou.Handle.Web;

import com.moying.lvyou.Entity.GoodsUse;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;

public class GoodsUseHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT * FROM goods_use order by uid desc limit 100").execute(ar -> handleQueryResult(request, true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select gu.*,goods.name as goodsname,road.name as roadname from goods_use gu left join goods on gu.goodsid=goods.gid left join road on gu.roadid=road.rid";
    // language=SQL
    var countsql = "select count(1) as records from goods_use gu left join goods on gu.goodsid=goods.gid left join road on gu.roadid=road.rid";
    StringBuilder where = new StringBuilder(" where gu.isshow=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by gu.gid desc" : " order by " + pageParms.getOrderby();//排序
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
    client.preparedQuery("update goods_use set isshow=1 WHERE uid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request, false, ar));
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, GoodsUse.class);
    client.preparedQuery("INSERT INTO goods_use set userid=?,goodsid=?, roadid=?, operid=?,opername=?,count= ?,isshow= ?, createtime= ?")
      .execute(Tuple.of(
        entity.getUserid(),
        entity.getGoodsid(),
        entity.getRoadid(),
        entity.getOperid(),
        entity.getOpername(),
        entity.getCount(),
        entity.getIsshow(),
        Utils.getNowDateTime()
      ), ar1 -> handleInsertResult(request, ar1));
  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM goods_use WHERE uid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, GoodsUse.class);
    client.preparedQuery("update goods_use set userid=?, roadid=?, goodsid=?, operid=?,opername=?,count= ? where uid=?")
      .execute(Tuple.of(
        entity.getUserid(),
        entity.getRoadid(),
        entity.getGoodsid(),
        entity.getOperid(),
        entity.getOpername(),
        entity.getCount(),
        entity.getUid()), ar -> handleUpdateResults(request, ar));
  }
}
