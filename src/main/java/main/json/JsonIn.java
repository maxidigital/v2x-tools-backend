//****************************************************************************//
// Copyright (C) 2024 German Airspace Center - All Rights Reserved
// Unauthorized copying of this file, via any medium is strictly prohibited
// Proprietary and confidential
// Written by Maximiliano Bottazzi <maximiliano.bottazzi@dlr.de>
//****************************************************************************//
package main.json;

import a.enums.Encoding;
import org.json.JSONObject;

public class JsonIn {

    private final String textData;
    private final Encoding inFormat;
    private final Encoding outFormat;

    public JsonIn(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);

        textData = jsonObject.optString("textData", null);
        String sendFormat = jsonObject.optString("sendFormat", null);
        String receiveFormat = jsonObject.optString("receiveFormat", null);

        if (textData == null)     throw new IllegalArgumentException("Missing field: textData");
        if (sendFormat == null)   throw new IllegalArgumentException("Missing field: sendFormat");
        if (receiveFormat == null) throw new IllegalArgumentException("Missing field: receiveFormat");

        this.inFormat = Encoding.guessEncoding(sendFormat);
        this.outFormat = Encoding.guessEncoding(receiveFormat);

        if (this.inFormat == null)  throw new IllegalArgumentException("Unknown format: " + sendFormat);
        if (this.outFormat == null) throw new IllegalArgumentException("Unknown format: " + receiveFormat);
    }

    public String getTextData() {
        return textData;
    }

    public Encoding getInFormat() {
        return inFormat;
    }

    public Encoding getOutFormat() {
        return outFormat;
    }
}
