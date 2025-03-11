package com.moying.lvyou.Handle.Web;

import com.moying.lvyou.Entity.MedalTemplate;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;

public class MedalTemplateHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT * FROM medal_template order by mid desc limit 100").execute(ar -> handleQueryResult(request,true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select * from medal_template";
    // language=SQL
    var countsql = "select count(1) as records from medal_template";
    StringBuilder where = new StringBuilder();
    var orderby = pageParms.getOrderby().isEmpty() ? " order by gid desc" : " order by " + pageParms.getOrderby();//排序
    //下面重组动态查询
    Tuple tuple = Tuple.tuple();
    if (queryParms != null) {
      for (String key : queryParms.keySet()) {
        if (!queryParms.get(key).isEmpty()) {
          if(queryParms.get(key).matches("-?\\d+")){
            if(Integer.parseInt(queryParms.get(key))>0){
              where.append(" or mid = ?");
              tuple.addString(queryParms.get(key));
            }
          }
          where.append(" or name like ?");
          tuple.addString("%"+queryParms.get(key)+"%");
        }
      }
    }
    if (tuple.size() >0) {
      where.insert(0, " where isshow=1 and 1=2 ");
    }else {
      where.insert(0, " where isshow=1 and 1=1 ");
    }
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
    client.preparedQuery("SELECT * FROM medal_template WHERE mid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request,false, ar));
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, MedalTemplate.class);
    client.preparedQuery("INSERT INTO medal_template set roadid=?, name=?,pic=?,isshow= ?,createtime=?")
      .execute(Tuple.of(
        entity.getRoadid(),
        entity.getName(),
        entity.getPic(),
        1,
        Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM medal_template WHERE mid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, MedalTemplate.class);
    if(Integer.parseInt(id)!=entity.getMid()){
      sendErrorResponse(request, JsonResult.fail("ID参数不匹配"));
    }
    client.preparedQuery("update medal_template set roadid=?, name=?,pic=?,isshow= ? where mid=?")
      .execute(Tuple.of(
        entity.getRoadid(),
        entity.getName(),
        entity.getPic(),
        entity.getIsshow(),
        entity.getMid()), ar -> handleUpdateResults(request, ar));
  }
}
