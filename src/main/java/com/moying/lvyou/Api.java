package com.moying.lvyou;

import com.moying.lvyou.Handle.App.*;
import com.moying.lvyou.utils.Jwt;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;

import static com.moying.lvyou.utils.Jwt.authHandler;
import static com.moying.lvyou.utils.Jwt.jwtAuth;
import static io.vertx.core.http.impl.HttpClientConnection.log;

/**
 * 这里是api接口，可以定义多个api接口
 */
public class Api {
  public static void init(Vertx vertx, Pool client, Router api) {
    //log.info("Api init");
    Jwt.init(vertx); // 调用静态初始化方法
    //公开路由
    api.get("/").handler(request -> request.response().putHeader("Content-Type", "application/json").end(new JsonObject().put("msg", "api").encodePrettily()));
    //用户登录
    api.post("/login").handler(request -> UserHandle.Login(request, jwtAuth, client));
    //微信登录并取得openid,同时生成一条用户记录
    api.post("/wxlogin").handler(request -> UserHandle.WxLogin(request, jwtAuth,client,vertx));

    //User
    api.get("/user").handler(authHandler).handler(request -> UserHandle.List(request, client));
    api.post("/user/:pageIndex").handler(authHandler).handler(request -> UserHandle.Page(request, client));
    api.post("/user").handler(authHandler).handler(request -> UserHandle.Post(request, client));
    api.delete("/user/:id").handler(authHandler).handler(request -> UserHandle.Delete(request, client));
    api.get("/user/:id").handler(authHandler).handler(request -> UserHandle.Get(request, client));
    api.put("/user/:id").handler(authHandler).handler(request -> UserHandle.Put(request, client));
    //Activity
    api.get("/activity").handler(authHandler).handler(request -> ActivityHandle.List(request, client));
    api.post("/activity/:pageIndex").handler(authHandler).handler(request -> ActivityHandle.Page(request, client));
    api.post("/activity").handler(authHandler).handler(request -> ActivityHandle.Post(request, client));
    api.delete("/activity/:id").handler(authHandler).handler(request -> ActivityHandle.Delete(request, client));
    api.get("/activity/:id").handler(authHandler).handler(request -> ActivityHandle.Get(request, client));
    api.put("/activity/:id").handler(authHandler).handler(request -> ActivityHandle.Put(request, client));
    //Bbs
    api.get("/bbs").handler(authHandler).handler(request -> BbsHandle.List(request, client));
    api.post("/bbs/:pageIndex").handler(authHandler).handler(request -> BbsHandle.Page(request, client));
    api.post("/bbs").handler(authHandler).handler(request -> BbsHandle.Post(request, client));
    api.delete("/bbs/:id").handler(authHandler).handler(request -> BbsHandle.Delete(request, client));
    api.get("/bbs/:id").handler(authHandler).handler(request -> BbsHandle.Get(request, client));
    api.put("/bbs/:id").handler(authHandler).handler(request -> BbsHandle.Put(request, client));
    //Enlist
    api.get("/enlist").handler(authHandler).handler(request -> EnlistHandle.List(request, client));
    api.post("/enlist/:pageIndex").handler(authHandler).handler(request -> EnlistHandle.Page(request, client));
    api.post("/enlist").handler(authHandler).handler(request -> EnlistHandle.Post(request, client));
    api.delete("/enlist/:id").handler(authHandler).handler(request -> EnlistHandle.Delete(request, client));
    api.get("/enlist/:id").handler(authHandler).handler(request -> EnlistHandle.Get(request, client));
    api.put("/enlist/:id").handler(authHandler).handler(request -> EnlistHandle.Put(request, client));

    //EnlistUserid
    api.get("/enlistuserid").handler(authHandler).handler(request -> EnlistUseridHandle.List(request, client));
    api.post("/enlistuserid/:pageIndex").handler(authHandler).handler(request -> EnlistUseridHandle.Page(request, client));
    api.post("/enlistuserid").handler(authHandler).handler(request -> EnlistUseridHandle.Post(request, client));
    api.delete("/enlistuserid/:id").handler(authHandler).handler(request -> EnlistUseridHandle.Delete(request, client));
    api.get("/enlistuserid/:id").handler(authHandler).handler(request -> EnlistUseridHandle.Get(request, client));
    api.put("/enlistuserid/:id").handler(authHandler).handler(request -> EnlistUseridHandle.Put(request, client));
    //goods
    api.get("/goods").handler(authHandler).handler(request -> GoodsHandle.List(request, client));
    api.post("/goods/:pageIndex").handler(authHandler).handler(request -> GoodsHandle.Page(request, client));
    api.post("/goods").handler(authHandler).handler(request -> GoodsHandle.Post(request, client));
    api.delete("/goods/:id").handler(authHandler).handler(request -> GoodsHandle.Delete(request, client));
    api.get("/goods/:id").handler(authHandler).handler(request -> GoodsHandle.Get(request, client));
    api.put("/goods/:id").handler(authHandler).handler(request -> GoodsHandle.Put(request, client));
    //goodsuse
    api.get("/goodsuse").handler(authHandler).handler(request -> GoodsUseHandle.List(request, client));
    api.post("/goodsuse/:pageIndex").handler(authHandler).handler(request -> GoodsUseHandle.Page(request, client));
    api.post("/goodsuse").handler(authHandler).handler(request -> GoodsUseHandle.Post(request, client));
    api.delete("/goodsuse/:id").handler(authHandler).handler(request -> GoodsUseHandle.Delete(request, client));
    api.get("/goodsuse/:id").handler(authHandler).handler(request -> GoodsUseHandle.Get(request, client));
    api.put("/goodsuse/:id").handler(authHandler).handler(request -> GoodsUseHandle.Put(request, client));
    //medal
    api.get("/medal").handler(authHandler).handler(request -> MedalHandle.List(request, client));
    api.post("/medal/:pageIndex").handler(authHandler).handler(request -> MedalHandle.Page(request, client));
    api.post("/medal").handler(authHandler).handler(request -> MedalHandle.Post(request, client));
    api.delete("/medal/:id").handler(authHandler).handler(request -> MedalHandle.Delete(request, client));
    api.get("/medal/:id").handler(authHandler).handler(request -> MedalHandle.Get(request, client));
    api.put("/medal/:id").handler(authHandler).handler(request -> MedalHandle.Put(request, client));

    //medaltemplate
    api.get("/medaltemplate").handler(authHandler).handler(request -> MedalTemplateHandle.List(request, client));
    api.post("/medaltemplate/:pageIndex").handler(authHandler).handler(request -> MedalTemplateHandle.Page(request, client));
    api.post("/medaltemplate").handler(authHandler).handler(request -> MedalTemplateHandle.Post(request, client));
    api.delete("/medaltemplate/:id").handler(authHandler).handler(request -> MedalTemplateHandle.Delete(request, client));
    api.get("/medaltemplate/:id").handler(authHandler).handler(request -> MedalTemplateHandle.Get(request, client));
    api.put("/medaltemplate/:id").handler(authHandler).handler(request -> MedalTemplateHandle.Put(request, client));

    //postion
    api.get("/postion").handler(authHandler).handler(request -> PostionHandle.List(request, client));
    api.post("/postion/:pageIndex").handler(authHandler).handler(request -> PostionHandle.Page(request, client));
    api.post("/postion").handler(authHandler).handler(request -> PostionHandle.Post(request, client));
    api.delete("/postion/:id").handler(authHandler).handler(request -> PostionHandle.Delete(request, client));
    api.get("/postion/:id").handler(authHandler).handler(request -> PostionHandle.Get(request, client));
    api.put("/postion/:id").handler(authHandler).handler(request -> PostionHandle.Put(request, client));
    //postionpic
    api.get("/postionpic").handler(authHandler).handler(request -> PostionPicHandle.List(request, client));
    api.post("/postionpic/:pageIndex").handler(authHandler).handler(request -> PostionPicHandle.Page(request, client));
    api.post("/postionpic").handler(authHandler).handler(request -> PostionPicHandle.Post(request, client));
    api.delete("/postionpic/:id").handler(authHandler).handler(request -> PostionPicHandle.Delete(request, client));
    api.get("/postionpic/:id").handler(authHandler).handler(request -> PostionPicHandle.Get(request, client));
    api.put("/postionpic/:id").handler(authHandler).handler(request -> PostionPicHandle.Put(request, client));
    //reply
    api.get("/reply").handler(authHandler).handler(request -> ReplyHandle.List(request, client));
    api.post("/reply/:pageIndex").handler(authHandler).handler(request -> ReplyHandle.Page(request, client));
    api.post("/reply").handler(authHandler).handler(request -> ReplyHandle.Post(request, client));
    api.delete("/reply/:id").handler(authHandler).handler(request -> ReplyHandle.Delete(request, client));
    api.get("/reply/:id").handler(authHandler).handler(request -> ReplyHandle.Get(request, client));
    api.put("/reply/:id").handler(authHandler).handler(request -> ReplyHandle.Put(request, client));
    //road
    api.get("/road").handler(authHandler).handler(request -> RoadHandle.List(request, client));
    api.post("/road/:pageIndex").handler(authHandler).handler(request -> RoadHandle.Page(request, client));
    api.post("/road").handler(authHandler).handler(request -> RoadHandle.Post(request, client));
    api.delete("/road/:id").handler(authHandler).handler(request -> RoadHandle.Delete(request, client));
    api.get("/road/:id").handler(authHandler).handler(request -> RoadHandle.Get(request, client));
    api.put("/road/:id").handler(authHandler).handler(request -> RoadHandle.Put(request, client));

    //roadpostion
    api.get("/roadpostion").handler(authHandler).handler(request -> RoadPostionHandle.List(request, client));
    api.post("/roadpostion/:pageIndex").handler(authHandler).handler(request -> RoadPostionHandle.Page(request, client));
    api.post("/roadpostion").handler(authHandler).handler(request -> RoadPostionHandle.Post(request, client));
    api.delete("/roadpostion/:id").handler(authHandler).handler(request -> RoadPostionHandle.Delete(request, client));
    api.get("/roadpostion/:id").handler(authHandler).handler(request -> RoadPostionHandle.Get(request, client));
    api.put("/roadpostion/:id").handler(authHandler).handler(request -> RoadPostionHandle.Put(request, client));

    //roadpostionoption
    api.get("/roadpostionoption").handler(authHandler).handler(request -> RoadPostionOptionHandle.List(request, client));
    api.post("/roadpostionoption/:pageIndex").handler(authHandler).handler(request -> RoadPostionOptionHandle.Page(request, client));
    api.post("/roadpostionoption").handler(authHandler).handler(request -> RoadPostionOptionHandle.Post(request, client));
    api.delete("/roadpostionoption/:id").handler(authHandler).handler(request -> RoadPostionOptionHandle.Delete(request, client));
    api.get("/roadpostionoption/:id").handler(authHandler).handler(request -> RoadPostionOptionHandle.Get(request, client));
    api.put("/roadpostionoption/:id").handler(authHandler).handler(request -> RoadPostionOptionHandle.Put(request, client));
    //tickoff
    api.get("/tickoff").handler(authHandler).handler(request -> TickOffHandle.List(request, client));
    api.post("/tickoff/:pageIndex").handler(authHandler).handler(request -> TickOffHandle.Page(request, client));
    api.post("/tickoff").handler(authHandler).handler(request -> TickOffHandle.Post(request, client));
    api.delete("/tickoff/:id").handler(authHandler).handler(request -> TickOffHandle.Delete(request, client));
    api.get("/tickoff/:id").handler(authHandler).handler(request -> TickOffHandle.Get(request, client));
    api.put("/tickoff/:id").handler(authHandler).handler(request -> TickOffHandle.Put(request, client));
    //travel
    api.get("/travel/goods/:tid").handler(authHandler).handler(request -> TravelHandle.GetShopGoods(request, client));//我能在该活动中能领取的物品
    api.get("/travel/myusegoods/:tid").handler(authHandler).handler(request -> TravelHandle.GetUseGoods(request, client));//取得我已领取的物品
    api.get("/travel/ranking/:rid").handler(authHandler).handler(request -> TravelHandle.Ranking(request, client));//我的活动排名
    api.get("/travel/lastpic").handler(authHandler).handler(request -> TravelHandle.LastPostionPic(request, client));//最后打卡景点的示意图
    api.get("/travel/option").handler(authHandler).handler(request -> TravelHandle.TickOffOption(request, client)); //打卡弹出的选择项
    api.get("/travel/mylast").handler(authHandler).handler(request -> TravelHandle.getLastTravel(request, client));//我的最后一个旅行活动
    api.get("/travel").handler(authHandler).handler(request -> TravelHandle.List(request, client));
    api.post("/travel/getphone").handler(authHandler).handler(request -> TravelHandle.getPhoneNumber(request, vertx));
    api.post("/travel/:pageIndex").handler(authHandler).handler(request -> TravelHandle.Page(request, client));
    api.post("/travel").handler(authHandler).handler(request -> TravelHandle.Post(request, client));
    api.delete("/travel/:id").handler(authHandler).handler(request -> TravelHandle.Delete(request, client));
    api.get("/travel/:id").handler(authHandler).handler(request -> TravelHandle.Get(request, client));
    api.put("/travel/:id").handler(authHandler).handler(request -> TravelHandle.Put(request, client));

//    api.get("/").handler(request -> request.response().putHeader("Content-Type", "application/json").end(new JsonObject().put("msg", "api").encodePrettily()));
//
//    var productRouter= Router.router(vertx);
//    productRouter.get("/").handler(rc ->rc.response().end("product"));
//    api.mountSubRouter("/product",productRouter);
//    //api.route("/product").subRouter(productRouter); //这个不支持二级
//    api
//      .routeWithRegex("\\/product\\/(?<productType>[^\\/]+)\\/(?<productID>[^\\/]+)")
//      .method(HttpMethod.GET)
//      .handler(ctx -> {
//        String productType = ctx.pathParam("productType");
//        String productID = ctx.pathParam("productID");
//       ctx.response().end(productType+"-"+productID);
//      });
//    api.get("/product/:id").handler(request -> request.response().putHeader("Content-Type", "application/json").end(new JsonObject().put("msg", request.pathParam("id")).encodePrettily()));
  }
}
