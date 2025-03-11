package com.moying.lvyou.Handle.Web;

import com.moying.lvyou.Entity.Travel;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.JsonUtil;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;

public class TravelHandle {

  public static void List(RoutingContext request, Pool client) {
    var rid = Integer.parseInt(request.queryParams().get("rid"));//线路ID
    client.preparedQuery("SELECT t.*,r.name as roadname FROM travel t left join road r on t.rid=r.rid where t.isshow=1 and t.isfinish=1 and t.rid=? order by t.speed desc limit 10000").execute(Tuple.of(rid),ar -> handleQueryResult(request, true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "SELECT t.*,r.name as roadname from travel t left join road r on t.rid=r.rid";
    // language=SQL
    var countsql = "select count(1) as records from travel t left join road r on t.rid=r.rid";
    StringBuilder where = new StringBuilder(" where 1=1 ");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by t.tid desc" : " order by " + pageParms.getOrderby();//排序
    //下面重组动态查询
    Tuple tuple = Tuple.tuple();
    if (queryParms != null) {
      for (String key : queryParms.keySet()) {
        if (!queryParms.get(key).isEmpty()) {
          if (key.equals("userkeyword")) {
            where.append(" and u.name like ?");
            tuple.addString("%" + queryParms.get(key) + "%");
            where.append(" and u.tel like ?");
            tuple.addString("%" + queryParms.get(key) + "%");
            where.append(" and u.cardno like ?");
            tuple.addString("%" + queryParms.get(key) + "%");
          }
          if (key.equals("rid")) {
            where.append(" and t.rid = ?");
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
          if (key.equals("status")) {
            //未审核
            if (Integer.parseInt(queryParms.get(key)) == 1) {
              where.append(" and t.isactive = ?");
              tuple.addString("0");
            }
            //未开始
            if (Integer.parseInt(queryParms.get(key)) == 2) {
              where.append(" and t.isactive=1 and t.isticket = ?");
              tuple.addString("0");
            }
            //进行中
            if (Integer.parseInt(queryParms.get(key)) == 3) {
              where.append(" and t.isactive=1 and t.isticket =1 and t.isfinish =?");
              tuple.addString("0");
            }
            //已完成
            if (Integer.parseInt(queryParms.get(key)) == 4) {
              where.append(" and t.isactive=1 and t.isticket =1 and t.isfinish =?");
              tuple.addString("1");
            }

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
    client.preparedQuery("SELECT * FROM travel WHERE tid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request, false, ar));
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = request.body().asJsonObject();
    try {
      var entity = JsonUtil.deserialize(body, Travel.class);
      if (entity.getName() == null || entity.getTel().isEmpty() || entity.getCardno() == null) {
        sendErrorResponse(request, JsonResult.fail("标题及内容不能为空"));
        return;
      }
      //检测rid是否存在
      client.preparedQuery("SELECT * FROM travel WHERE uid=? and isshow=1 and isactive=0 and rid = ?")
        .execute(Tuple.of(entity.getUid(), entity.getRid()), ar -> {
          if (ar.succeeded()) {
            if (ar.result().size() == 0) {
              sendErrorResponse(request, JsonResult.fail("该线路不存在"));
            } else {
              client.preparedQuery("INSERT INTO travel set rid=?,uid=?, name=?,cardno=?,tel=?,createTime= ?")
                .execute(Tuple.of(
                  entity.getRid(),
                  entity.getUid(),
                  entity.getName(),
                  entity.getCardno(),
                  entity.getTel(),
                  Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
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
    client.preparedQuery("DELETE FROM travel WHERE tid = $1")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID参数不能为空"));
      return;
    }
    var body = request.body().asJsonObject();
    try {
      var entity = JsonUtil.deserialize(body, Travel.class);
      //检测userid是否存在
      client.preparedQuery("SELECT * FROM travel WHERE tid = ?")
        .execute(Tuple.of(entity.getTid()), ar -> {
          if (ar.succeeded()) {
            if (ar.result().size() == 0) {
              sendErrorResponse(request, JsonResult.fail("用户不存在"));
            } else {
              client.preparedQuery("update travel set isactive=?,isfinish=?,isticket=?, isshow=? where tid=?")
                .execute(Tuple.of(
                  entity.getIsactive(),
                  entity.getIsfinish(),
                  entity.getIsticket(),
                  entity.getIsshow(),
                  entity.getTid()), ar1 -> handleUpdateResults(request, ar1));
            }
          }
        });
    } catch (Exception e) {
      e.printStackTrace();
      sendErrorResponse(request, JsonResult.fail(e.getLocalizedMessage()));
    }
  }
}
