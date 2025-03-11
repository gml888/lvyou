package com.moying.lvyou.Handle.Web;

import com.moying.lvyou.Entity.Manage;
import com.moying.lvyou.Entity.Shop;
import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.JsonUtil;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;
import java.util.Objects;

import static com.moying.lvyou.utils.DbHelp.*;
import static io.vertx.core.http.impl.HttpClientConnection.log;

public class ShopHandle {

  public static void List(RoutingContext request, Pool client) {
    client.query("SELECT shopid,shopname FROM shop where status=1 order by shopid desc limit 10000").execute(ar -> handleQueryResult(request,true, ar));
  }

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    // language=SQL
    var sql = "select * from shop";
    // language=SQL
    var countsql = "select count(1) as records from shop";
    StringBuilder where = new StringBuilder(" where status=1");
    var orderby = pageParms.getOrderby().isEmpty() ? " order by shopid desc" : " order by " + pageParms.getOrderby();//排序
    //下面重组动态查询
    Tuple tuple = Tuple.tuple();
    if (queryParms != null) {
      for (String key : queryParms.keySet()) {
        if (!queryParms.get(key).isEmpty()) {
          where.append(" and (shopname like ?");
          tuple.addString("%"+queryParms.get(key)+"%");
          where.append(" or name like ?");
          tuple.addString("%"+queryParms.get(key)+"%");
          where.append(" or tel like ?");
          tuple.addString("%"+queryParms.get(key)+"%");
          if(queryParms.get(key).matches("-?\\d+")){
            if(Integer.parseInt(queryParms.get(key))>0){
              where.append(" or shopid = ?");
              tuple.addString(queryParms.get(key));
            }
          }
          where.append(") ");
        }
      }
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
    client.preparedQuery("SELECT * FROM shop WHERE shopid = ?")
      .execute(Tuple.of(id), ar -> handleQueryResult(request,false, ar));
  }

  public static void Post(RoutingContext request, Pool client) {
    var body = Json.encodePrettily(request.body().asJsonObject());
    var entity = Json.decodeValue(body, Shop.class);
    client.preparedQuery("INSERT INTO shop set username=?, pwd=?,openid=?, shopname=?, headface=?, name=?,tel=?,status=?, createtime= ?")
      .execute(Tuple.of(
        entity.getUsername(),
        Utils.md5(entity.getPwd()) ,
        "",
        entity.getShopname(),
        "",
        entity.getName(),
        entity.getTel(),
        1,
        Utils.getNowDateTime()), ar1 -> handleInsertResult(request, ar1));
  }


  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("update  shop set status=0 WHERE shopid = ?")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }

  public static void Put(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    var body = request.body().asJsonObject();
    Shop entity;
    try {
      entity = JsonUtil.deserialize(body, Shop.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if(entity.getPwd().length()<32){//设置了新密码
      entity.setPwd( Utils.md5(entity.getPwd()));
    }
    client.preparedQuery("update shop set shopname=?,name=?,tel=?,status=?,pwd=? where shopid=?")
      .execute(Tuple.of(
        entity.getShopname(),
        entity.getName(),
        entity.getTel(),
        entity.getStatus(),
        entity.getPwd(),
        id
      ), ar -> handleUpdateResults(request, ar));
  }

  public static void Login(RoutingContext request, JWTAuth jwtAuth, Pool client) {

    var body = Json.encodePrettily(request.body().asJsonObject());
    log.info(body);
    var entity = Json.decodeValue(body, Manage.class);
    var username = entity.getUsername();
    var password = entity.getPassword();
    if (Objects.equals(username, "") || Objects.equals(password, "")) {
      sendErrorResponse(request, JsonResult.fail("用户名及密码不能为空"));
      return;
    }
    client.preparedQuery("select * from shop where status=1 and username=? and pwd=?")
      .execute(Tuple.of(
        entity.getUsername(),
        Utils.md5(entity.getPassword())), ar -> {
        if (ar.succeeded()) {
          if(ar.result().iterator().hasNext()){
            var shopid = ar.result().iterator().next().getInteger("shopid");
            // 生成 JWT 令牌
            JsonObject payload = new JsonObject()
              .put("sub", shopid + "_" + username)
              .put("exp", System.currentTimeMillis() + 3600000); // 1小时后过期

            String token = jwtAuth.generateToken(payload, new JWTOptions().setAlgorithm("HS256"));
            sendResponse(request, JsonResult.success("ok",token));
          }else{
            sendErrorResponse(request, JsonResult.fail("用户名或密码错误"));
          }
        } else {
          ar.cause().printStackTrace();
          sendErrorResponse(request, JsonResult.fail(ar.cause().getLocalizedMessage()));
        }
      });
  }
}
