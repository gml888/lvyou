package com.moying.lvyou;

import com.moying.lvyou.Handle.Web.*;
import com.moying.lvyou.utils.Jwt;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;

import static com.moying.lvyou.utils.Jwt.authHandler;
import static com.moying.lvyou.utils.Jwt.jwtAuth;


public class Webapi {
  public static void init(Vertx vertx, Pool client, Router webapi) {
    Jwt.init(vertx); // 调用静态初始化方法
    //公开路由
    webapi.get("/").handler(request -> request.response().putHeader("Content-Type", "application/json").end(new JsonObject().put("msg", "webapi").encodePrettily()));
    //后台用户登录
    webapi.post("/login").handler(request -> ManageHandle.Login(request, jwtAuth, client));

    //Manage
    webapi.get("/manage").handler(authHandler).handler(request -> ManageHandle.List(request, client));
    webapi.post("/manage/:pageIndex").handler(authHandler).handler(request -> ManageHandle.Page(request, client));
    webapi.post("/manage").handler(authHandler).handler(request -> ManageHandle.Post(request, client));
    webapi.delete("/manage/:id").handler(authHandler).handler(request -> ManageHandle.Delete(request, client));
    webapi.get("/manage/:id").handler(authHandler).handler(request -> ManageHandle.Get(request, client));
    webapi.put("/manage/:id").handler(authHandler).handler(request -> ManageHandle.Put(request, client));
    //Activity
    webapi.get("/activity").handler(authHandler).handler(request -> ActivityHandle.List(request, client));
    webapi.post("/activity/:pageIndex").handler(authHandler).handler(request -> ActivityHandle.Page(request, client));
    webapi.post("/activity").handler(authHandler).handler(request -> ActivityHandle.Post(request, client));
    webapi.delete("/activity/:id").handler(authHandler).handler(request -> ActivityHandle.Delete(request, client));
    webapi.get("/activity/:id").handler(authHandler).handler(request -> ActivityHandle.Get(request, client));
    webapi.put("/activity/:id").handler(authHandler).handler(request -> ActivityHandle.Put(request, client));
    //Bbs
    webapi.get("/bbs").handler(authHandler).handler(request -> BbsHandle.List(request, client));
    webapi.post("/bbs/:pageIndex").handler(authHandler).handler(request -> BbsHandle.Page(request, client));
    webapi.post("/bbs").handler(authHandler).handler(request -> BbsHandle.Post(request, client));
    webapi.delete("/bbs/:id").handler(authHandler).handler(request -> BbsHandle.Delete(request, client));
    webapi.get("/bbs/:id").handler(authHandler).handler(request -> BbsHandle.Get(request, client));
    webapi.put("/bbs/:id").handler(authHandler).handler(request -> BbsHandle.Put(request, client));

    //bbscategory
    webapi.get("/bbscategory").handler(authHandler).handler(request -> BbsCategoryHandle.List(request, client));
    webapi.post("/bbscategory/:pageIndex").handler(authHandler).handler(request -> BbsCategoryHandle.Page(request, client));
    webapi.post("/bbscategory").handler(authHandler).handler(request -> BbsCategoryHandle.Post(request, client));
    webapi.delete("/bbscategory/:id").handler(authHandler).handler(request -> BbsCategoryHandle.Delete(request, client));
    webapi.get("/bbscategory/:id").handler(authHandler).handler(request -> BbsCategoryHandle.Get(request, client));
    webapi.put("/bbscategory/:id").handler(authHandler).handler(request -> BbsCategoryHandle.Put(request, client));
    //Enlist
    webapi.get("/enlist").handler(authHandler).handler(request -> EnlistHandle.List(request, client));
    webapi.post("/enlist/:pageIndex").handler(authHandler).handler(request -> EnlistHandle.Page(request, client));
    webapi.post("/enlist").handler(authHandler).handler(request -> EnlistHandle.Post(request, client));
    webapi.delete("/enlist/:id").handler(authHandler).handler(request -> EnlistHandle.Delete(request, client));
    webapi.get("/enlist/:id").handler(authHandler).handler(request -> EnlistHandle.Get(request, client));
    webapi.put("/enlist/:id").handler(authHandler).handler(request -> EnlistHandle.Put(request, client));

    //EnlistUserid
    webapi.get("/enlistuserid").handler(authHandler).handler(request -> EnlistUseridHandle.List(request, client));
    webapi.post("/enlistuserid/:pageIndex").handler(authHandler).handler(request -> EnlistUseridHandle.Page(request, client));
    webapi.post("/enlistuserid").handler(authHandler).handler(request -> EnlistUseridHandle.Post(request, client));
    webapi.delete("/enlistuserid/:id").handler(authHandler).handler(request -> EnlistUseridHandle.Delete(request, client));
    webapi.get("/enlistuserid/:id").handler(authHandler).handler(request -> EnlistUseridHandle.Get(request, client));
    webapi.put("/enlistuserid/:id").handler(authHandler).handler(request -> EnlistUseridHandle.Put(request, client));
    //goods
    webapi.get("/goods").handler(authHandler).handler(request -> GoodsHandle.List(request, client));
    webapi.post("/goods/:pageIndex").handler(authHandler).handler(request -> GoodsHandle.Page(request, client));
    webapi.post("/goods").handler(authHandler).handler(request -> GoodsHandle.Post(request, client));
    webapi.delete("/goods/:id").handler(authHandler).handler(request -> GoodsHandle.Delete(request, client));
    webapi.get("/goods/:id").handler(authHandler).handler(request -> GoodsHandle.Get(request, client));
    webapi.put("/goods/:id").handler(authHandler).handler(request -> GoodsHandle.Put(request, client));
    //goodsuse
    webapi.get("/goodsuse").handler(authHandler).handler(request -> GoodsUseHandle.List(request, client));
    webapi.post("/goodsuse/:pageIndex").handler(authHandler).handler(request -> GoodsUseHandle.Page(request, client));
    webapi.post("/goodsuse").handler(authHandler).handler(request -> GoodsUseHandle.Post(request, client));
    webapi.delete("/goodsuse/:id").handler(authHandler).handler(request -> GoodsUseHandle.Delete(request, client));
    webapi.get("/goodsuse/:id").handler(authHandler).handler(request -> GoodsUseHandle.Get(request, client));
    webapi.put("/goodsuse/:id").handler(authHandler).handler(request -> GoodsUseHandle.Put(request, client));
    //medal
    webapi.get("/medal").handler(authHandler).handler(request -> MedalHandle.List(request, client));
    webapi.post("/medal/:pageIndex").handler(authHandler).handler(request -> MedalHandle.Page(request, client));
    webapi.post("/medal").handler(authHandler).handler(request -> MedalHandle.Post(request, client));
    webapi.delete("/medal/:id").handler(authHandler).handler(request -> MedalHandle.Delete(request, client));
    webapi.get("/medal/:id").handler(authHandler).handler(request -> MedalHandle.Get(request, client));
    webapi.put("/medal/:id").handler(authHandler).handler(request -> MedalHandle.Put(request, client));

    //medaltemplate
    webapi.get("/medaltemplate").handler(authHandler).handler(request -> MedalTemplateHandle.List(request, client));
    webapi.post("/medaltemplate/:pageIndex").handler(authHandler).handler(request -> MedalTemplateHandle.Page(request, client));
    webapi.post("/medaltemplate").handler(authHandler).handler(request -> MedalTemplateHandle.Post(request, client));
    webapi.delete("/medaltemplate/:id").handler(authHandler).handler(request -> MedalTemplateHandle.Delete(request, client));
    webapi.get("/medaltemplate/:id").handler(authHandler).handler(request -> MedalTemplateHandle.Get(request, client));
    webapi.put("/medaltemplate/:id").handler(authHandler).handler(request -> MedalTemplateHandle.Put(request, client));

    //postion
    webapi.get("/postion").handler(authHandler).handler(request -> PostionHandle.List(request, client));
    webapi.post("/postion/:pageIndex").handler(authHandler).handler(request -> PostionHandle.Page(request, client));
    webapi.post("/postion").handler(authHandler).handler(request -> PostionHandle.Post(request, client));
    webapi.delete("/postion/:id").handler(authHandler).handler(request -> PostionHandle.Delete(request, client));
    webapi.get("/postion/:id").handler(authHandler).handler(request -> PostionHandle.Get(request, client));
    webapi.put("/postion/:id").handler(authHandler).handler(request -> PostionHandle.Put(request, client));
    //postionpic
    webapi.get("/postionpic").handler(authHandler).handler(request -> PostionPicHandle.List(request, client));
    webapi.post("/postionpic/:pageIndex").handler(authHandler).handler(request -> PostionPicHandle.Page(request, client));
    webapi.post("/postionpic").handler(authHandler).handler(request -> PostionPicHandle.Post(request, client));
    webapi.delete("/postionpic/:id").handler(authHandler).handler(request -> PostionPicHandle.Delete(request, client));
    webapi.get("/postionpic/:id").handler(authHandler).handler(request -> PostionPicHandle.Get(request, client));
    webapi.put("/postionpic/:id").handler(authHandler).handler(request -> PostionPicHandle.Put(request, client));
    //reply
    webapi.get("/reply").handler(authHandler).handler(request -> ReplyHandle.List(request, client));
    webapi.post("/reply/:pageIndex").handler(authHandler).handler(request -> ReplyHandle.Page(request, client));
    webapi.post("/reply").handler(authHandler).handler(request -> ReplyHandle.Post(request, client));
    webapi.delete("/reply/:id").handler(authHandler).handler(request -> ReplyHandle.Delete(request, client));
    webapi.get("/reply/:id").handler(authHandler).handler(request -> ReplyHandle.Get(request, client));
    webapi.put("/reply/:id").handler(authHandler).handler(request -> ReplyHandle.Put(request, client));
    //road
    webapi.get("/road").handler(authHandler).handler(request -> RoadHandle.List(request, client));
    webapi.post("/road/:pageIndex").handler(authHandler).handler(request -> RoadHandle.Page(request, client));
    webapi.post("/road").handler(authHandler).handler(request -> RoadHandle.Post(request, client));
    webapi.delete("/road/:id").handler(authHandler).handler(request -> RoadHandle.Delete(request, client));
    webapi.get("/road/:id").handler(authHandler).handler(request -> RoadHandle.Get(request, client));
    webapi.put("/road/:id").handler(authHandler).handler(request -> RoadHandle.Put(request, client));

    //roadpostion
    webapi.get("/roadpostion").handler(authHandler).handler(request -> RoadPostionHandle.List(request, client));
    webapi.post("/roadpostion/:pageIndex").handler(authHandler).handler(request -> RoadPostionHandle.Page(request, client));
    webapi.post("/roadpostion").handler(authHandler).handler(request -> RoadPostionHandle.Post(request, client));
    webapi.delete("/roadpostion/:id").handler(authHandler).handler(request -> RoadPostionHandle.Delete(request, client));
    webapi.get("/roadpostion/:id").handler(authHandler).handler(request -> RoadPostionHandle.Get(request, client));
    webapi.put("/roadpostion/:id").handler(authHandler).handler(request -> RoadPostionHandle.Put(request, client));

    //roadpostionoption
    webapi.get("/roadpostionoption").handler(authHandler).handler(request -> RoadPostionOptionHandle.List(request, client));
    webapi.post("/roadpostionoption/:pageIndex").handler(authHandler).handler(request -> RoadPostionOptionHandle.Page(request, client));
    webapi.post("/roadpostionoption").handler(authHandler).handler(request -> RoadPostionOptionHandle.Post(request, client));
    webapi.delete("/roadpostionoption/:id").handler(authHandler).handler(request -> RoadPostionOptionHandle.Delete(request, client));
    webapi.get("/roadpostionoption/:id").handler(authHandler).handler(request -> RoadPostionOptionHandle.Get(request, client));
    webapi.put("/roadpostionoption/:id").handler(authHandler).handler(request -> RoadPostionOptionHandle.Put(request, client));

    //shop
    webapi.get("/shop").handler(authHandler).handler(request -> ShopHandle.List(request, client));
    webapi.post("/shop/login").handler(request -> ShopHandle.Login(request, jwtAuth, client));//商家登录核销产品及相看核销的产品
    webapi.post("/shop/:pageIndex").handler(authHandler).handler(request -> ShopHandle.Page(request, client));
    webapi.post("/shop").handler(authHandler).handler(request -> ShopHandle.Post(request, client));
    webapi.delete("/shop/:id").handler(authHandler).handler(request -> ShopHandle.Delete(request, client));
    webapi.get("/shop/:id").handler(authHandler).handler(request -> ShopHandle.Get(request, client));
    webapi.put("/shop/:id").handler(authHandler).handler(request -> ShopHandle.Put(request, client));

    webapi.post("/shop/manage/goodsuse").handler(authHandler).handler(request -> ShopGoodsUseHandle.Post(request, client));//商品核销
    webapi.post("/shop/manage/goodsuse/:pageIndex").handler(authHandler).handler(request -> ShopGoodsUseHandle.Page(request, client));//商品分页
    //tickoff
    webapi.get("/tickoff").handler(authHandler).handler(request -> TickOffHandle.List(request, client));
    webapi.post("/tickoff/:pageIndex").handler(authHandler).handler(request -> TickOffHandle.Page(request, client));
    webapi.post("/tickoff").handler(authHandler).handler(request -> TickOffHandle.Post(request, client));
    webapi.delete("/tickoff/:id").handler(authHandler).handler(request -> TickOffHandle.Delete(request, client));
    webapi.get("/tickoff/:id").handler(authHandler).handler(request -> TickOffHandle.Get(request, client));
    webapi.put("/tickoff/:id").handler(authHandler).handler(request -> TickOffHandle.Put(request, client));
    //user
    webapi.get("/user").handler(authHandler).handler(request -> UserHandle.List(request, client));
    webapi.post("/user/:pageIndex").handler(authHandler).handler(request -> UserHandle.Page(request, client));
    webapi.post("/user").handler(authHandler).handler(request -> UserHandle.Post(request, client));
    webapi.delete("/user/:id").handler(authHandler).handler(request -> UserHandle.Delete(request, client));
    webapi.get("/user/:id").handler(authHandler).handler(request -> UserHandle.Get(request, client));
    webapi.put("/user/:id").handler(authHandler).handler(request -> UserHandle.Put(request, client));
    //travel
    webapi.get("/travel").handler(authHandler).handler(request -> TravelHandle.List(request, client));
    webapi.post("/travel/:pageIndex").handler(authHandler).handler(request -> TravelHandle.Page(request, client));
    webapi.post("/travel").handler(authHandler).handler(request -> TravelHandle.Post(request, client));
    webapi.delete("/travel/:id").handler(authHandler).handler(request -> TravelHandle.Delete(request, client));
    webapi.get("/travel/:id").handler(authHandler).handler(request -> TravelHandle.Get(request, client));
    webapi.put("/travel/:id").handler(authHandler).handler(request -> TravelHandle.Put(request, client));
    //qrcode
    webapi.get("/qrcode/:pid").handler(request -> QrCodeHandle.CreateQrCode(request, vertx));
  }
}
