package com.moying.lvyou.Handle.Web;

import com.moying.lvyou.Entity.Medal;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import com.moying.lvyou.utils.JsonUtil;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;

public class MedalHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT * FROM medal order by mid desc limit 100").execute(ar -> handleQueryResult(request, true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select m.*, u.nickname,u.name,r.name as roadname from medal m left join tour_tools.user u on m.userid = u.userid left join road r on m.roadid = r.rid";
    // language=SQL
    var countsql = "select count(1) as records from medal m left join tour_tools.user u on m.userid = u.userid left join road r on m.roadid = r.rid";
    StringBuilder where = new StringBuilder(" where m.isshow=1 and (");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by mid desc" : " order by " + pageParms.getOrderby();//排序
    //下面重组动态查询
    Tuple tuple = Tuple.tuple();
    if (queryParms != null) {
      var isfirst = true;
      for (String key : queryParms.keySet()) {
        if (!queryParms.get(key).isEmpty()) {
          if(queryParms.get(key).matches("-?\\d+")){
            if(Integer.parseInt(queryParms.get(key))>0){
              where.append(isfirst ? "" : "and ").append(" m.mid = ?");
              tuple.addString(queryParms.get(key));
              isfirst = false;
            }
          }
        }
      }
    }
    where.append(")");
    if (tuple.size() == 0) {
      where.delete(0, where.length());
      where.append(" where m.isshow=1 ");
    }
    //log.info("where1--:" + where);
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
    client.preparedQuery("SELECT * FROM medal WHERE mid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request, false, ar));
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = request.body().asJsonObject();
    try {
      var entity = JsonUtil.deserialize(body, Medal.class);
      //log.info("put:" + Json.encodePrettily(entity));
      //检测userid是否存在
      client.preparedQuery("SELECT * FROM user WHERE userid = ?")
        .execute(Tuple.of(entity.getUserid()), ar -> {
          if (ar.succeeded()) {
            if (ar.result().size() == 0) {
              sendErrorResponse(request, JsonResult.fail("用户不存在"));
            } else {
              //从勋章模板中取出图片地址
              client.preparedQuery("SELECT mt.pic FROM medal_template mt left join road rd on mt.roadid=rd.rid WHERE mt.roadid  = ?")
                .execute(Tuple.of(entity.getRoadid()), ar1 -> {
                  if (ar1.succeeded()) {
                    if (ar1.result().size() == 0) {
                      sendErrorResponse(request, JsonResult.fail("勋章模板不存在"));
                    } else {
                      client.preparedQuery("insert into medal set roadid=?, userid=?,pic=?,isshow=?,createtime=?")
                        .execute(Tuple.of(
                          entity.getRoadid(),
                          entity.getUserid(),
                          ar1.result().iterator().next().getString("pic"),
                          1,
                          Utils.getNowDateTime()
                        ), ar2 -> handleUpdateResults(request, ar2));
                    }
                  }
                });
            }
          }
        });
    } catch (Exception e) {
      e.printStackTrace();
      sendErrorResponse(request, JsonResult.fail(e.getLocalizedMessage()));
    }
  }

  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("DELETE FROM medal WHERE mid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = request.body().asJsonObject();
    try {
      var entity = JsonUtil.deserialize(body, Medal.class);
      if (Integer.parseInt(id) != entity.getMid()) {
        sendErrorResponse(request, JsonResult.fail("ID 参数不匹配"));
      }
      //检测userid是否存在
      client.preparedQuery("SELECT * FROM tour_tools.user WHERE userid = ?")
        .execute(Tuple.of(entity.getUserid()), ar -> {
          if (ar.succeeded()) {
            if (ar.result().size() == 0) {
              sendErrorResponse(request, JsonResult.fail("用户不存在"));
            } else {
              //从勋章模板中取出图片地址
              client.preparedQuery("SELECT mt.pic FROM medal_template mt left join road rd on mt.roadid=rd.rid WHERE mt.roadid  = ?")
                .execute(Tuple.of(entity.getRoadid()), ar1 -> {
                  if (ar1.succeeded()) {
                    if (ar1.result().size() == 0) {
                      sendErrorResponse(request, JsonResult.fail("勋章模板不存在"));
                    } else {
                      client.preparedQuery("update medal set roadid=?, userid=?,pic=? where mid=?")
                        .execute(Tuple.of(
                          entity.getRoadid(),
                          entity.getUserid(),
                          ar1.result().iterator().next().getString("pic"),
                          entity.getMid()), ar2 -> handleUpdateResults(request, ar2));
                    }
                  }
                });
            }
          }
        });
    } catch (Exception e) {
      e.printStackTrace();
      sendErrorResponse(request, JsonResult.fail(e.getLocalizedMessage()));
    }
  }
}
