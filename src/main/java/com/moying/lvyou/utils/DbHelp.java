package com.moying.lvyou.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

import java.util.ArrayList;

public class DbHelp {

  public static void sendResponse(RoutingContext request, Object response) {
    request.response()
      .putHeader("Content-Type", "application/json")
      .end(Json.encodePrettily(response));
  }

  public static void sendErrorResponse(RoutingContext request, Object response) {
    request.response()
     // .setStatusCode(500)
      .putHeader("Content-Type", "application/json")
      .end(Json.encodePrettily(response));
  }

  public static void handleInsertResult(RoutingContext request, AsyncResult<RowSet<Row>> ar) {
    if (ar.succeeded()) {
      sendResponse(request, JsonResult.success());
    } else {
      ar.cause().printStackTrace();
      sendErrorResponse(request, JsonResult.fail(ar.cause().getLocalizedMessage()));
    }
  }
  public static void handleDeleteResult(RoutingContext request, AsyncResult<RowSet<Row>> ar) {
    if (ar.succeeded()) {
      sendResponse(request, JsonResult.success());
    } else {
      ar.cause().printStackTrace();
      sendErrorResponse(request, JsonResult.fail(ar.cause().getLocalizedMessage()));
    }
  }

  public static void handleUpdateResults(RoutingContext request, AsyncResult<RowSet<Row>> ar) {
    if (ar.succeeded()) {
      sendResponse(request, JsonResult.success());
    } else {
      ar.cause().printStackTrace();
      sendErrorResponse(request, JsonResult.fail(ar.cause().getLocalizedMessage()));
    }
  }

  public static void handleQueryResult(RoutingContext request, boolean islist, AsyncResult<RowSet<Row>> ar) {
    if (ar.succeeded()) {
      if (islist) {
        JsonArray jsonArray = new JsonArray();
        ar.result().forEach(rs -> jsonArray.add(rs.toJson()));
        sendResponse(request, JsonResult.success(jsonArray));
      }else {
        if (ar.result().size() > 0) {
          sendResponse(request, JsonResult.success(ar.result().iterator().next().toJson()));
        }else{
          sendResponse(request, JsonResult.fail("没有相关数据"));
        }
      }
    } else {
      ar.cause().printStackTrace();
      sendErrorResponse(request, JsonResult.fail(ar.cause().getLocalizedMessage()));
    }
  }

  /**
   * 查询分页
   * @param request RoutingContext
   * @param ar1 AsyncResult<RowSet<Row>>
   * @param pageIndex int
   * @param pageSize int
   * @param total int
   */
  public static void handlePageResult(RoutingContext request, AsyncResult<RowSet<Row>> ar1, int pageIndex, int pageSize, Integer total) {
    if (ar1.succeeded()) {
      var rslist = new ArrayList<JsonObject>();
      ar1.result().forEach(rs -> rslist.add(rs.toJson()));
      var result = PageResult.success(rslist, pageIndex, pageSize, total);
      sendResponse(request, result);
    } else {
      ar1.cause().printStackTrace();
      sendErrorResponse(request, JsonResult.fail(ar1.cause().getMessage()));
    }
  }




}
