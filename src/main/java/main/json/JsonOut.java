//****************************************************************************//
// Copyright (C) 2024 German Airspace Center - All Rights Reserved
// Unauthorized copying of this file, via any medium is strictly prohibited
// Proprietary and confidential
// Written by Maximiliano Bottazzi <maximiliano.bottazzi@dlr.de>
//****************************************************************************//
package main.json;

/**
 *
 * @author bott_ma
 */
public class JsonOut
{

    private final int responseCode;
    private final String encoding;
    private final String data;

    public static final JsonOut DECODING_ERROR = new JsonOut(400, "", "Error decoding V2X message");
    //public static final JsonOut CONTENT_TYPE_ERROR = new JsonOut(400, "", "Error decoding V2X message");

    /**
     *
     * @param responseCode
     * @param encoding
     * @param data
     */
    public JsonOut(int responseCode, String encoding, String data) {
        this.responseCode = responseCode;
        this.encoding = encoding;
        this.data = data;
    }

    /**
     *
     * @return
     */
    public String getData() {
        return data;
    }

    /**
     *
     * @return
     */
    public int getResponseCode() {
        return responseCode;
    }

    public String getJSON() {
        StringBuilder sb = new StringBuilder();

        sb.append("{");

        // Append responseCode
        sb.append("\"responseCode\":");
        sb.append("\"").append(responseCode).append("\",");

        // Append encoding
        sb.append("\"encoding\":");
        if (encoding != null) {
            sb.append("\"").append(encoding).append("\",");
        } else {
            sb.append("null,");
        }

        // Append data
        sb.append("\"data\":");
        if (data != null) {
            String _data = data.replaceAll("\"", "\\\\\"");

            sb.append("\"").append(_data).append("\"");
        } else {
            sb.append("null");
        }

        sb.append("}");

        return sb.toString();
    }
}
