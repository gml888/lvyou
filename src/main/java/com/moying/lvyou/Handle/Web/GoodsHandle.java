package com.moying.lvyou.Handle.Web;

import com.moying.lvyou.Entity.Goods;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.JsonUtil;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;

public class GoodsHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT gid,name FROM goods where isshow=1 order by gid desc limit 1000").execute(ar -> handleQueryResult(request,true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select g.*,shopname from goods g left join shop s on g.shopid=s.shopid";
    // language=SQL
    var countsql = "select count(1) as records from goods g left join shop s on g.shopid=s.shopid";
    StringBuilder where = new StringBuilder(" where isshow=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by g.gid desc" : " order by " + pageParms.getOrderby();//排序
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
    client.preparedQuery("SELECT * FROM goods WHERE gid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request,false, ar));
  }

  public static void Post(RoutingContext request, Pool client) {
    var body =request.body().asJsonObject();
    Goods entity;
    try {
      entity = JsonUtil.deserialize(body, Goods.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    client.preparedQuery("INSERT INTO goods set shopid=?, name=?, pic=?,count=?,content= ?,isshow= ?,createtime=?")
      .execute(Tuple.of(
        entity.getShopid(),
        entity.getName(),
        entity.getPic(),
        entity.getCount(),
        entity.getContent(),
        1, Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("update goods set isshow=0 WHERE gid = ?")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body =request.body().asJsonObject();
    Goods entity;
    try {
      entity = JsonUtil.deserialize(body, Goods.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    client.preparedQuery("update goods set name=?,shopid=?, pic=?,count=?,content= ?  where gid=?")
      .execute(Tuple.of(
        entity.getName(),
        entity.getShopid(),
        entity.getPic(),
        entity.getCount(),
        entity.getContent(),
        entity.getGid()), ar -> handleUpdateResults(request, ar));
  }
}
