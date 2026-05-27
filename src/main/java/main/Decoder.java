//****************************************************************************//
// Copyright (C) 2024 German Airspace Center - All Rights Reserved
// Unauthorized copying of this file, via any medium is strictly prohibited
// Proprietary and confidential
// Written by Maximiliano Bottazzi <maximiliano.bottazzi@dlr.de>
//****************************************************************************//
package main;

import a.MessageId;
import a.enums.Encoding;
import a.messages.Payload;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import i.Sequence;
import i.WindException;
import main.json.JsonIn;
import main.json.JsonOut;

/**
 *
 * @author bott_ma
 */
public class Decoder
{

    /**
     *
     * @param bytes
     * @return
     * @throws WindException
     */
    public synchronized JsonOut decodeUPER2JSON(byte[] bytes) throws WindException {
        Payload payloadIn = Payload.create(bytes, Encoding.UPER);

        A.p("Decoding: %s", payloadIn);
        MessagesApp mapp = MessagesApp.getInstance();
        MessageId mid = mapp.extractMessageId(bytes, Encoding.UPER);
        Sequence sequence = mapp.createEmptyMessage(mid);
        sequence = mapp.decode(sequence, payloadIn);

        A.p("Encoding to %s", "JSON");
        Payload payloadOut = mapp.encode(sequence, Encoding.JSON);

        String response = payloadOut.toText();

        return new JsonOut(200, "JSON", response);
    }

    /**
     *
     * @param in
     * @return
     */
    public JsonOut decode(JsonIn in) {
        Payload payloadIn = Payload.create(in.getTextData(), in.getInFormat());

        try {
            A.p("Decoding: %s", payloadIn);
            MessagesApp mapp = MessagesApp.getInstance();
            Sequence sequence = mapp.createEmptyMessage(payloadIn.getMessageId());
            sequence = mapp.decode(sequence, payloadIn);

            //System.out.println(sequence);
            A.p("Encoding to %s", in.getOutFormat());
            Payload payloadOut = mapp.encode(sequence, in.getOutFormat());

            String response = "Error";
            if (in.getOutFormat().isUPER() || in.getOutFormat().isWER()) {
                response = payloadOut.getHexWithEncoding();
            } else if (in.getOutFormat().isJSON() || in.getOutFormat().isXML()) {
                response = payloadOut.toText();
            }

            //A.p("Responding: %s", response.replaceAll("\\n", ""));
            return new JsonOut(200, in.getOutFormat().name(), response);
        } catch (WindException ex) {
            ex.printStackTrace();
            return JsonOut.DECODING_ERROR;
        }
    }
}
