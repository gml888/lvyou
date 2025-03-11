package com.moying.lvyou.Handle.Web;

import com.moying.lvyou.Entity.TickOff;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;

public class TickOffHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT * FROM tick_off order by tickid desc limit 100").execute(ar -> handleQueryResult(request, true,ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select t.*,p.name as postionname,r.name as roadname,u.name,u.nickname from tick_off t left join road r on t.rid=r.rid left join postion p on p.pid=t.pid left join user u on u.userid=t.uid";
    // language=SQL
    var countsql = "select count(1) as records from tick_off t left join road r on t.rid=r.rid left join postion p on p.pid=t.pid left join user u on u.userid=t.uid";
    StringBuilder where = new StringBuilder(" where 1=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by tickid desc" : " order by " + pageParms.getOrderby();//排序
    //下面重组动态查询
    Tuple tuple = Tuple.tuple();
    if (queryParms != null) {
      for (String key : queryParms.keySet()) {
        if (!queryParms.get(key).isEmpty()) {
          if (key.equals("userkeyword")) {
            if(queryParms.get(key).matches("-?\\d+")){
              if(Integer.parseInt(queryParms.get(key))>0){
                where.append(" and t.uid = ?");
                tuple.addString(queryParms.get(key));
              }
            }else{
              where.append(" and u.name like ?");
              tuple.addString("%"+queryParms.get(key)+"%");
              where.append(" and u.nickname like ?");
              tuple.addString("%"+queryParms.get(key)+"%");
            }
          }
          if (key.equals("roadid")) {
            where.append(" and t.rid = ?");
            tuple.addString(queryParms.get(key));
          }
          if (key.equals("postionid")) {
            where.append(" and t.pid = ?");
            tuple.addString(queryParms.get(key));
          }
          if (key.equals("startDate")) {
            where.append(" and t.createtime >= ?");
            tuple.addString(queryParms.get(key));
          }
          if (key.equals("endDate")) {
            where.append(" and t.createtime <= ?");
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
      .execute(Tuple.of(id), ar -> handleQueryResult(request,false, ar));
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, TickOff.class);
    client.preparedQuery("INSERT INTO tick_off set uid=?, rid=?,pid=?, points=?, distance=?, time=?, createtime= ?")
      .execute(Tuple.of(
        entity.getUid(),
        entity.getRid(),
        entity.getPid(),
        entity.getPoints(),
        entity.getDistance(),
        entity.getTime(),
        Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
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
