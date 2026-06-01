//****************************************************************************//
// Copyright (C) 2024 German Airspace Center - All Rights Reserved
// Unauthorized copying of this file, via any medium is strictly prohibited
// Proprietary and confidential
// Written by Maximiliano Bottazzi <maximiliano.bottazzi@dlr.de>
//****************************************************************************//
package main.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonOut {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int responseCode;
    private final String encoding;
    private final String data;

    public static final JsonOut DECODING_ERROR = new JsonOut(400, "", "Error decoding V2X message");

    public JsonOut(int responseCode, String encoding, String data) {
        this.responseCode = responseCode;
        this.encoding = encoding;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getJSON() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("responseCode", String.valueOf(responseCode));
        node.put("encoding", encoding != null ? encoding : "");
        node.put("data", data != null ? data : "");
        try {
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            return "{\"responseCode\":\"500\",\"encoding\":\"\",\"data\":\"Serialization error\"}";
        }
    }
}
