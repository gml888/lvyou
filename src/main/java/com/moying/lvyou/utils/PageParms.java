package com.moying.lvyou.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;

import static io.vertx.core.http.impl.HttpClientConnection.log;

/**
 * 分页查询参数
 */
@Schema(description = "分页查询参数")
@Getter
@Setter
public class PageParms {
  /**
   * 每页条数
   */
  @Schema(description = "每页条数", required = true, defaultValue = "10")
  Integer pagesize;

  /**
   * 排序字段(如:id desc,name asc)
   */
  @Schema(description = "排序字段,多个排序字段使用逗号分开")
  String orderby;
  /**
   * 查询参数列表(如{"name":"","color": "绿"})
   */
  @Schema(description = "查询参数列表")
  Map<String, String> queryparms;


  /**
   * 获取分页参数
   * 示例：
   * {
   * "pagesize": 2,
   * "orderby": "id asc",
   * "queryparms":
   * {
   * "name": "黄瓜",
   * "color": "绿",
   * }
   * }
   *
   * @param body RequestBody
   * @return PageParms
   */
  public static PageParms getPageParms(RequestBody body) {
    if (!body.isEmpty()) {
      ObjectMapper objectMapper = new ObjectMapper();
      PageParms thisPageParms;
      try {
        thisPageParms = objectMapper.readValue(body.asJsonObject().encode(), PageParms.class);
      } catch (JsonProcessingException e) {
        log.info(e.getMessage());
        throw new RuntimeException(e);
      }
      return thisPageParms;
    }
    PageParms pageParms = new PageParms();
    pageParms.setPagesize(10);
    pageParms.setOrderby("");
    pageParms.setQueryparms(null);
    return pageParms;
  }
}
