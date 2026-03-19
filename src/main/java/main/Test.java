//****************************************************************************//
// Copyright (C) 2024 German Airspace Center - All Rights Reserved
// Unauthorized copying of this file, via any medium is strictly prohibited
// Proprietary and confidential
// Written by Maximiliano Bottazzi <maximiliano.bottazzi@dlr.de>
//****************************************************************************//
package main;

import a.MessageId;
import a.codecs.Decoder;
import a.codecs.Encoder;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import i.Sequence;
import i.WindException;

/**
 *
 * @author bott_ma
 */
public class Test
{

    public static void main(String[] args) {
        String aa = "0209cd880794048a6b50987fd1aee201890806b98a52284243671fee0d3cb0";
        System.out.println(aa.length());
    }

    public static void main22(String[] args) throws WindException {
        Sequence seq = MessagesApp.getInstance().createEmptyMessage(MessageId.SSEM_V2);

        //System.out.println(seq);
        Encoder enco = a.Config.createDefault().encoders().createPerEncoder();
        enco.put(seq);
        System.out.println(enco.getPayload().getHex());

        Decoder deco = a.Config.createDefault().decoders().createPerDecoder();

        deco.load(enco.getPayload().getBytes());
        deco.get(seq);
        //System.out.println(seq);
    }
}
