//****************************************************************************//
// Copyright (C) 2024 German Airspace Center - All Rights Reserved
// Unauthorized copying of this file, via any medium is strictly prohibited
// Proprietary and confidential
// Written by Maximiliano Bottazzi <maximiliano.bottazzi@dlr.de>
//****************************************************************************//
package main.json;

import a.enums.Encoding;
import org.json.JSONObject;

/**
 *
 * @author bott_ma
 */
public class JsonIn
{

    private final String textData;
    private final Encoding inFormat;
    private final Encoding outFormat;

    /**
     *
     * @param jsonString
     */
    public JsonIn(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);

        textData = jsonObject.getString("textData");
        String _sendFormat = jsonObject.getString("sendFormat");
        String _receiveFormat = jsonObject.getString("receiveFormat");

        this.inFormat = Encoding.guessEncoding(_sendFormat);
        this.outFormat = Encoding.guessEncoding(_receiveFormat);
    }

    /**
     *
     * @return
     */
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
