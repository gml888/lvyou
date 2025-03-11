package com.moying.lvyou.Handle.Web;

import com.moying.lvyou.Entity.User;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.JsonUtil;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;
import static io.vertx.core.http.impl.HttpClientConnection.log;

public class UserHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT * FROM user order by userid desc limit 100").execute(ar -> handleQueryResult(request,true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select * from user";
    // language=SQL
    var countsql = "select count(1) as records from user";
    StringBuilder where = new StringBuilder();
    var orderby = pageParms.getOrderby().isEmpty() ? " order by userid desc" : " order by " + pageParms.getOrderby();//排序
    //下面重组动态查询
    Tuple tuple = Tuple.tuple();
    if (queryParms != null) {
      for (String key : queryParms.keySet()) {
        if (!queryParms.get(key).isEmpty()) {
            if(queryParms.get(key).matches("-?\\d+")){
              if(Integer.parseInt(queryParms.get(key))>0){
                where.append(" or userid = ?");
                tuple.addString(queryParms.get(key));
              }
            }
          where.append(" or username like ?");
            tuple.addString("%"+queryParms.get(key)+"%");
          where.append(" or nickname like ?");
            tuple.addString("%"+queryParms.get(key)+"%");
          where.append(" or tel like ?");
            tuple.addString("%"+queryParms.get(key)+"%");
        }
      }
    }

    if (tuple.size() >0) {
      where.insert(0, " where 1=2 ");
    }else {
      where.insert(0, " where 1=1 ");
    }
    //log.info("where:" + where);
    client.preparedQuery(MessageFormat.format("{0}{1}{2}", countsql, where, orderby)).execute(tuple, ar -> {
      if (ar.succeeded()) {
        var total = ar.result().iterator().next().getInteger("records");//取得记录数
        //取得数据列表
        var limit = "  limit " + pageSize * (pageIndex - 1) + "," + pageSize;
        //log.info("where:" + MessageFormat.format("{0}{1}{2}{3}", sql, where, orderby, limit));
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
    client.preparedQuery("SELECT * FROM user WHERE userid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request,false, ar));
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, User.class);
    client.preparedQuery("INSERT INTO user set username=?, pwd=?,openid=?, nickname=?, headface=?, name=?,tel=?,status=?, createtime= ?")
      .execute(Tuple.of(
        entity.getUsername(),
        entity.getPwd(),
        entity.getOpenid(),
        entity.getNickname(),
        entity.getHeadface(),
        entity.getName(),
        entity.getTel(),
        entity.getStatus(),
        Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM user WHERE userid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.pathParams().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = request.body().asJsonObject();
    User entity;
    try {
      entity = JsonUtil.deserialize(body, User.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    client.preparedQuery("update user set nickname=?, headface=?, tel=?, wxnumber=?, ispublic=? where userid=?")
      .execute(Tuple.of(
        entity.getNickname(),
        entity.getHeadface(),
        entity.getTel(),
        entity.getWxnumber(),
        entity.getIspublic(),
        entity.getUserid()
      ), ar -> handleUpdateResults(request, ar));
  }
}
