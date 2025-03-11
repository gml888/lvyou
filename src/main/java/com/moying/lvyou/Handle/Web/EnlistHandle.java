package com.moying.lvyou.Handle.Web;

import com.moying.lvyou.Entity.Enlist;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;

public class EnlistHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT * FROM enlist order by eid desc limit 100").execute(ar -> handleQueryResult(request, true,ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select e.*,a.title from enlist e left join activities a on e.infoid=a.aid";
    // language=SQL
    var countsql = "select count(1) as records from enlist e left join activities a on e.infoid=a.aid";
    StringBuilder where = new StringBuilder(" where 1=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by aid desc" : " order by " + pageParms.getOrderby();//排序
    //下面重组动态查询
    Tuple tuple = Tuple.tuple();
    if (queryParms != null) {
      for (String key : queryParms.keySet()) {
        if (!queryParms.get(key).isEmpty()) {
          if (key.equals("userid")) {
            where.append(" and e.userid = ?");
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
    client.preparedQuery("SELECT e.*,a.title,a.content FROM enlist e left join activities a on e.infoid=a.aid  WHERE eid = ?")
      .execute(Tuple.of(id), ar -> {
        if (ar.succeeded()) {
          if (ar.result().size() > 0) {
            //查找报名用户列表并加到字段content中
            client.preparedQuery("SELECT * FROM enlist_userid WHERE eid = ?")
              .execute(Tuple.of(id), ar1 -> {
                if (ar1.succeeded()) {
                  StringBuilder userlist = new StringBuilder();
                  ar1.result().forEach(rs -> {
                    var row = rs.toJson();
                    userlist.append("用户ID:").append(row.getString("userid")).append(",");
                    userlist.append("姓名:").append(row.getString("name")).append(",");
                    userlist.append("电话:").append(row.getString("tel")).append(",");
                    userlist.append("备注:").append(row.getString("memo").replace(",", "，")).append("|");
                  });

                  var reslut=ar.result().iterator().next().toJson().put("userlist", userlist );
                  sendResponse(request,JsonResult.success(reslut));
                }
              });
          } else {
            sendErrorResponse(request, JsonResult.fail("找不到相关记录"));
          }
        }
      });
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, Enlist.class);

    client.preparedQuery("INSERT INTO enlist set userid=?, infoid=?,isshow=?,createTime= ?")
      .execute(Tuple.of(
        entity.getUserid(),
        entity.getInfoid(),
        entity.getIsshow(),
        Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM enlist WHERE eid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, Enlist.class);
    client.preparedQuery("update enlist set userid=?, infoid=?,isshow=? where eid=?")
      .execute(Tuple.of(
        entity.getUserid(),
        entity.getInfoid(),
        entity.getIsshow(),
        id), ar -> handleUpdateResults(request, ar));
  }
}
