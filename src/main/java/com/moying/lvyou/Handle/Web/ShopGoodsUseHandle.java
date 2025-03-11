package com.moying.lvyou.Handle.Web;

import com.moying.lvyou.utils.JsonResult;
import com.moying.lvyou.utils.Jwt;
import com.moying.lvyou.utils.PageParms;
import com.moying.lvyou.utils.Utils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.text.MessageFormat;

import static com.moying.lvyou.utils.DbHelp.*;

public class ShopGoodsUseHandle {

  public static void Page(RoutingContext request, Pool client) {
    var pageIndex = Integer.parseInt(request.pathParam("pageIndex"));
    var pageParms = PageParms.getPageParms(request.body());
    int pageSize = pageParms.getPagesize();//分页条数
    var queryParms = pageParms.getQueryparms();//取得查询参数
    Jwt.getSubFromShopToken(request).onComplete(ar0 -> { //
        if (ar0.succeeded()) {
          var sub = ar0.result();
          if (sub != null) {
            var shopid = Integer.parseInt(sub.substring(5));//shopID
            // language=SQL
            var sql = "select gu.*,goods.name as goodsname from goods_use gu left join goods on gu.goodsid=goods.gid";
            // language=SQL
            var countsql = "select count(1) as records from goods_use gu left join goods on gu.goodsid=goods.gid";
            StringBuilder where = new StringBuilder(" where goods.shopid=" + shopid + " and gu.isshow=1 ");
            var orderby = pageParms.getOrderby().isEmpty() ? " order by gu.gid desc" : " order by " + pageParms.getOrderby();//排序
            //下面重组动态查询
            Tuple tuple = Tuple.tuple();
            if (queryParms != null) {
              for (String key : queryParms.keySet()) {
                if (!queryParms.get(key).isEmpty()) {
                  if (queryParms.get(key).matches("-?\\d+")) {
                    if (Integer.parseInt(queryParms.get(key)) > 0) {
                      where.append(" and goods.gid = ?");
                      tuple.addString(queryParms.get(key));
                    }
                  } else {
                    where.append(" and goods.name like ? ");
                    tuple.addString("%" + queryParms.get(key) + "%");
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
          } else {
            sendErrorResponse(request, JsonResult.fail("您没登录或没找到商家信息"));
          }
        } else {
          ar0.cause().printStackTrace();
          sendErrorResponse(request, JsonResult.fail(ar0.cause().getLocalizedMessage()));
        }
      }
    );
  }

  //核销物品
  public static void Post(RoutingContext request, Pool client) {
    var body = request.body().asJsonObject();
    Jwt.getSubFromShopToken(request).onComplete(ar -> { //
      if (ar.succeeded()) {
        var sub = ar.result();
        if (sub != null) {
          var shopid = Integer.parseInt(sub.substring(5));//shopID
          var travelid = body.getInteger("travelid");
          var roadid = body.getInteger("roadid");
          var userid = body.getInteger("userid");
          //取得商家信息
          Future<JsonObject> shopFuture = Future.future(promise -> client.preparedQuery("SELECT name FROM shop WHERE status=1 and shopid = ?").execute(Tuple.of(shopid), ar1 -> {
            if (ar1.succeeded()) {
              RowSet<Row> result = ar1.result();
              if (result.size() > 0) {
                Row row = result.iterator().next();
                promise.complete(row.toJson());
              } else {
                promise.fail("没有商家信息");
              }
            } else {
              promise.fail(ar1.cause());
            }
          }));
          //取得该线路可用核销的产品id
          Future<JsonObject> goodsidstrFuture = Future.future(promise -> client.preparedQuery("SELECT goodsidstr from road  WHERE isshow=1 and rid = ?").execute(Tuple.of(roadid), ar1 -> {
            if (ar1.succeeded()) {
              RowSet<Row> result = ar1.result();
              if (result.size() > 0) {
                Row row = result.iterator().next();
                if (!row.getValue("goodsidstr").equals("")) {
                  promise.complete(row.toJson());
                } else {
                  promise.fail("该线路没有产品");
                }
              } else {
                promise.fail("没有线路信息");
              }
            } else {
              promise.fail(ar1.cause());
            }
          }));

          //取得该用户该线路该旅行活动已核销的产品列表
          Future<JsonArray> useGoodsListFuture = Future.future(promise -> client.preparedQuery("SELECT goodsid,count FROM goods_use WHERE isshow=1 and userid = ? and travelid = ? and roadid = ?").execute(Tuple.of(userid, travelid, roadid), ar1 -> {
            if (ar1.succeeded()) {
              RowSet<Row> result = ar1.result();
              if (result.size() > 0) {
                JsonArray goods = new JsonArray();
                for (Row row : result) {
                  goods.add(row.toJson());
                }
                promise.complete(goods);
              } else {
                promise.complete(new JsonArray());
              }
            } else {
              promise.fail(ar1.cause());
            }
          }));
          // 使用 CompositeFuture 来处理多个异步查询的结果
          CompositeFuture.all(shopFuture, goodsidstrFuture, useGoodsListFuture).onComplete(ar0 -> {
            if (ar0.succeeded()) {
              JsonObject shop = shopFuture.result();
              var opername = shop.getString("name");//操作人
              JsonObject goodsidstrobj = goodsidstrFuture.result();
              var goodsidstr = goodsidstrobj.getString("goodsidstr");//该线路可用的产品id
              //找出该用户该线路该旅行活动未核销的产品列表,如果存在把第一条符合的加到已核销产品表
              //列出该商家该线路可用的产品列表
              client.preparedQuery("SELECT gid as goodsid,count FROM goods WHERE isshow=1 and shopid = ? and goods.gid in (?) order by gid desc ").execute(Tuple.of(shopid, goodsidstr), ar1 -> {
                if (ar1.succeeded()) {
                  RowSet<Row> result = ar1.result();
                  if (result.size() > 0) {
                    JsonArray useGoodsList = useGoodsListFuture.result();//该用户该线路该旅行活动已核销的产品列表
                    var usegoodsid = 0;//本次要核销的产品id
                    for (Row row : result) {
                      var goodsid = row.getString("goodsid");
                      var count = row.getInteger("count");
                      int usedCount = 0;
                      // 达到上限时提前终止
                      for (int i = 0; i < useGoodsList.size(); i++) {
                        var useGoods = useGoodsList.getJsonObject(i);
                        if (useGoods.getString("goodsid").equals(goodsid)) {
                          usedCount += useGoods.getInteger("count");
                          // 已达上限时提前跳出
                          if (usedCount >= count) break;
                        }
                      }
                      // 判断逻辑简化
                      if (usedCount < count) {
                        usegoodsid = Integer.parseInt(goodsid);
                        break; // 找到第一个可用商品即终止外层循环
                      }
                    }
                    // 如果usegoodsid不为0，则说明找到了未核销的产品，进行核销操作
                    if (usegoodsid != 0) {
                      int finalUsegoodsid = usegoodsid;
                      client.preparedQuery("INSERT INTO goods_use SET userid = ?,travelid = ?,roadid = ?,goodsid = ?,count = ?,opername = ?,isshow = ?, createtime = ?")
                        .execute(Tuple.of(userid, travelid, roadid, usegoodsid, 1, opername, 1, Utils.getNowDateTime()), ar2 -> {
                          if (ar2.succeeded()) {
                            //这里查询本次核销产品的信息并返回
                            client.preparedQuery("SELECT gid,name FROM goods WHERE isshow=1 and gid = ?")
                              .execute(Tuple.of(finalUsegoodsid), ar3 -> {
                                if (ar3.succeeded()) {
                                  if (ar3.result().size() > 0) {
                                    JsonObject goods = ar3.result().iterator().next().toJson();
                                    //核销成功,返回产品信息
                                    sendResponse(request, JsonResult.success(goods));
                                  } else {
                                    sendErrorResponse(request, JsonResult.fail("找不到相关产品"));
                                  }
                                } else {
                                  sendErrorResponse(request, JsonResult.fail(ar3.cause().getLocalizedMessage()));
                                }
                              });
                          } else {
                            sendErrorResponse(request, JsonResult.fail(ar2.cause().getLocalizedMessage()));
                          }
                        });
                    } else {
                      sendErrorResponse(request, JsonResult.fail("该用户产品已全部核销完成"));
                    }
                  } else {
                    sendErrorResponse(request, JsonResult.fail("该线路没有可核销产品"));
                  }
                } else {
                  sendErrorResponse(request, JsonResult.fail(ar1.cause().getLocalizedMessage()));
                }
              });
            } else {
              sendErrorResponse(request, JsonResult.fail(ar0.cause().getLocalizedMessage()));
            }
          });
        }
      }
    });
  }

  //设置为不可见
  public static void Delete(RoutingContext request, Pool client) {
    var id = request.request().params().get("id");
    if (id == null || id.isEmpty()) {
      sendErrorResponse(request, JsonResult.fail("ID 参数不能为空"));
      return;
    }
    client.preparedQuery("update goods_use set isshow=0 WHERE uid = ?")
      .execute(Tuple.of(id), ar -> handleDeleteResult(request, ar));
  }
}
