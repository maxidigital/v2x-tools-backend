//****************************************************************************//
// Copyright (C) 2024 German Airspace Center - All Rights Reserved
// Unauthorized copying of this file, via any medium is strictly prohibited
// Proprietary and confidential
// Written by Maximiliano Bottazzi <maximiliano.bottazzi@dlr.de>
//****************************************************************************//
package main.gets;

import com.sun.net.httpserver.HttpExchange;
import main.A;

/**
 *
 * @author bott_ma
 */
public class GetCopyToClipboard
{

    public static void exec(HttpExchange exchange) {
        A.pt("Copied to clipboard");
    }
}
