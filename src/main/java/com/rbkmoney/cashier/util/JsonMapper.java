package com.rbkmoney.cashier.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.Map;

public class JsonMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SneakyThrows
    public static String toJson(Map<String, String> map) {
        return MAPPER.writeValueAsString(map);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static Map<String, String> toMap(String json) {
        return MAPPER.readValue(json, Map.class);
    }
}
