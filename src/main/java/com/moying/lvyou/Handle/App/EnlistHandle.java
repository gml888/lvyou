package com.moying.lvyou.Handle.App;

import com.moying.lvyou.Entity.Enlist;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.Jwt;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;

public class EnlistHandle {
  //列出某个活动报名人员
  public static void List(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    //如果是活动发起者本人显示全部,如果是游客则只显示公开
    Jwt.getSubFromToken(request).onComplete(ar -> {
      if (ar.succeeded()) {
        var sub = ar.result();
        if (sub != null) {
          var userid = Integer.parseInt(sub.substring(5));//用户ID
          //查询活动列表
          client.preparedQuery("SELECT userid FROM activities where aid=?").execute(Tuple.of(id), ar1 -> {
            if (ar1.succeeded()) {
              ar1.result().forEach(rs -> {
                if (rs.getInteger("userid").equals(userid)) {//活动发起者本人显示全部
                  client.preparedQuery("SELECT e.*,u.nickname,u.headface FROM enlist e left join user u on e.userid=u.userid where e.infoid=? order by eid desc limit 1000").execute(Tuple.of(id), ar2 -> handleQueryResult(request, true, ar2));
                } else {//游客则只显示公开
                  client.preparedQuery("SELECT e.*,u.nickname,u.headface FROM enlist e left join user u on e.userid=u.userid where isshow=1 and e.infoid=? order by eid desc limit 1000").execute(Tuple.of(id), ar2 -> handleQueryResult(request, true, ar2));
                }
              });
            }
          });
        }
      }
    });
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select * from enlist";
    // language=SQL
    var countsql = "select count(1) as records from enlist";
    StringBuilder where = new StringBuilder(" where 1=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by aid desc" : " order by " + pageParms.getOrderby();//排序
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
    var id = request.pathParams().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("SELECT * FROM enlist WHERE eid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request, false, ar));
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, Enlist.class);
    if (entity.getInfoid() == null || entity.getTel() == null || entity.getName() == null || entity.getMemo() == null) {
      sendErrorResponse(request, JsonResult.fail("姓名电话备注不能为空"));
      return;
    }
    Jwt.getSubFromToken(request).onComplete(ar -> {
      if (ar.succeeded()) {
        var sub = ar.result();
        if (sub != null) {
          var userid = Integer.parseInt(sub.substring(5));//用户ID
          //检测是否已报名
          client.preparedQuery("SELECT * FROM enlist WHERE infoid=? and userid=?")
            .execute(Tuple.of(entity.getInfoid(), userid), ar1 -> {
              if (ar1.succeeded()) {
                if (ar1.result().size() > 0) {
                  sendErrorResponse(request, JsonResult.fail("你已报过名了,不要重复报名"));
                } else {
                  entity.setUserid(userid);
                  client.preparedQuery("INSERT INTO enlist set userid=?, infoid=?, name=?,tel=?,memo=?, createTime= ?")
                    .execute(Tuple.of(
                      entity.getUserid(),
                      entity.getInfoid(),
                      entity.getName(),
                      entity.getTel(),
                      entity.getMemo(),
                      Utils.getNowDateTime()), ar2 -> handleInsertResult(request, ar2));
                }
              }
            });
        }
      }
    });
  }

  public static void Delete(RoutingContext request, Pool client) {
    var id = request.pathParams().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID参数不能为空"));
      return;
    }
    //先找到这条记录
    client.preparedQuery("SELECT * FROM enlist WHERE eid = ?")
      .execute(Tuple.of(id), ar -> {
        if (ar.succeeded()) {
          if (ar.result().size() > 0) {
            var entity = ar.result().iterator().next();
            if (entity.getInteger("isshow") == 1) {
              client.preparedQuery("update enlist set isshow=? WHERE eid = ?")
                .execute(Tuple.of(0, id), ar1 -> handleDeleteResult(request, ar1));
            } else {
              client.preparedQuery("update enlist set isshow=? WHERE eid = ?")
                .execute(Tuple.of(1, id), ar1 -> handleDeleteResult(request, ar1));
            }
          }
        }
      });
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.pathParams().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID参数不能为空"));
      return;
    }
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, Enlist.class);
    client.preparedQuery("update enlist set name=?, tel=?, memo=? where eid=?")
      .execute(Tuple.of(
        entity.getName(),
        entity.getTel(),
        entity.getMemo(),
        id), ar -> handleUpdateResults(request, ar));
  }
}
