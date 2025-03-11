package com.moying.lvyou.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moying.lvyou.Config.JacksonConfig;
import io.vertx.core.json.JsonObject;

public class JsonUtil {
  private static final ObjectMapper objectMapper = JacksonConfig.getObjectMapper();

  public static <T> T deserialize(JsonObject jsonObject, Class<T> clazz) throws Exception {
    return objectMapper.readValue(jsonObject.toString(), clazz);
  }
}
