package com.moying.lvyou.Handle.Web;

import com.moying.lvyou.Entity.RoadPostion;
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
import static io.vertx.core.http.impl.HttpClientConnection.log;

public class RoadPostionHandle {

  public static void List(RoutingContext request, Pool client) {
    var rid = Integer.parseInt(request.queryParams().get("rid"));
    client.preparedQuery("SELECT r.*,p.name as pname FROM road_postion r left join postion p on r.pid=p.pid where rid=? order by sn desc limit 100").execute(Tuple.of(rid), ar -> handleQueryResult(request, true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select * from road_postion";
    // language=SQL
    var countsql = "select count(1) as records from road_postion";
    StringBuilder where = new StringBuilder(" where 1=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by rpid desc" : " order by " + pageParms.getOrderby();//排序
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
    var id = Integer.parseInt(request.pathParam("id"));
    if (id == 0) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("SELECT * FROM road_postion WHERE rpid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request, false, ar));
  }

  public static void Post(RoutingContext request, Pool client)  {
    var body = request.body().asJsonObject();
    try {
      var entity = JsonUtil.deserialize(body, RoadPostion.class);
      //log.info("body:" + Json.encodePrettily(entity));
      client.preparedQuery("INSERT INTO road_postion set pid=?,rid=?,pic=?,distance=?,point=?, sn=?,isshow=?,createtime= ?")
        .execute(Tuple.of(
          entity.getPid(),
          entity.getRid(),
          entity.getPic(),
          entity.getDistance(),
          entity.getPoint(),
          entity.getSn(),
          1,
          Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
    }catch (Exception e){
      sendErrorResponse(request, JsonResult.fail(e.getLocalizedMessage()));
    }

  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = Integer.parseInt(request.pathParam("id"));
    if (id == 0) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM road_postion WHERE rpid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client)  {
    var id = Integer.parseInt(request.pathParam("id"));
    if (id == 0) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = request.body().asJsonObject();
    try {
      var entity = JsonUtil.deserialize(body, RoadPostion.class);
      client.preparedQuery("update road_postion set pid=?,pic=?,distance=?,point=?, sn=?,isshow=? where rpid=?")
        .execute(Tuple.of(
          entity.getPid(),
          entity.getPic(),
          entity.getDistance(),
          entity.getPoint(),
          entity.getSn(),
          entity.getIsshow(),
          entity.getRpid()
        ), ar -> handleUpdateResults(request, ar));
    }catch (Exception e){
      sendErrorResponse(request, JsonResult.fail(e.getLocalizedMessage()));
    }
  }
}
