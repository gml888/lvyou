package com.moying.lvyou.Config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GsonDateTimeFormatter {
  public static final Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
      if (value != null) {
        out.value(formatter.format(value));
      } else {
        out.nullValue();
      }
    }

    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
      String value = in.nextString();
      return value == null || value.isEmpty() ? null : LocalDateTime.parse(value, formatter);
    }
  }).create();
}
