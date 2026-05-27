package main.services;

import de.dlr.ts.v2x.wind_asn1_parser.Asn1Choice;
import de.dlr.ts.v2x.wind_asn1_parser.Asn1Element;
import de.dlr.ts.v2x.wind_asn1_parser.Asn1Enumerated;
import de.dlr.ts.v2x.wind_asn1_parser.Asn1Field;
import de.dlr.ts.v2x.wind_asn1_parser.Asn1Module;
import de.dlr.ts.v2x.wind_asn1_parser.Asn1NamedValue;
import de.dlr.ts.v2x.wind_asn1_parser.Asn1Parser;
import de.dlr.ts.v2x.wind_asn1_parser.Asn1Sequence;
import de.dlr.ts.v2x.wind_asn1_parser.ParseError;
import main.A;
import main.monitoring.TelegramCenter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class Asn1ParsingService {

    private final TelegramCenter telegramCenter;

    public Asn1ParsingService(TelegramCenter telegramCenter) {
        this.telegramCenter = telegramCenter;
    }

    public ConversionResult parse(String text, String clientIp, String endpoint) {
        long startTime = System.currentTimeMillis();

        try {
            if (text == null || text.trim().isEmpty())
                return ConversionResult.error("Input is empty", 400);

            Asn1Parser parser = Asn1Parser.fromText(text);
            String json = buildJson(parser);
            long responseTime = System.currentTimeMillis() - startTime;

            A.p("ASN.1 parse: client=%s, modules=%d, errors=%d, time=%dms",
                    clientIp, parser.getModules().size(), parser.getErrors().size(), responseTime);
            telegramCenter.notifyApiUsage(endpoint, clientIp, text, responseTime);

            return ConversionResult.success(json, 200);

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            A.p("ASN.1 parse error: %s", e.getMessage());
            telegramCenter.notifyError(e.getMessage(), endpoint, clientIp);
            return ConversionResult.error("Parse failed: " + e.getMessage(), 500);
        }
    }

    private String buildJson(Asn1Parser parser) {
        JSONObject root = new JSONObject();

        JSONArray errorsArr = new JSONArray();
        for (ParseError err : parser.getErrors()) {
            JSONObject e = new JSONObject();
            e.put("line", err.getLine());
            e.put("message", err.getMessage());
            errorsArr.put(e);
        }
        root.put("errors", errorsArr);

        JSONArray modulesArr = new JSONArray();
        for (Asn1Module mod : parser.getModules()) {
            modulesArr.put(moduleToJson(mod));
        }
        root.put("modules", modulesArr);

        return root.toString(2);
    }

    private JSONObject moduleToJson(Asn1Module mod) {
        JSONObject obj = new JSONObject();
        obj.put("name", mod.getModuleId().getName());

        JSONArray elements = new JSONArray();
        for (Asn1Element el : mod.getElements()) {
            elements.put(elementToJson(el));
        }
        obj.put("elements", elements);

        return obj;
    }

    private JSONObject elementToJson(Asn1Element el) {
        JSONObject obj = new JSONObject();
        obj.put("name", el.getName());

        if (el instanceof Asn1Sequence seq) {
            obj.put("kind", "SEQUENCE");
            obj.put("hasExtension", seq.hasExt());
            JSONArray fields = new JSONArray();
            for (Asn1Field f : seq.getFields())
                fields.put(fieldToJson(f));
            obj.put("fields", fields);

        } else if (el instanceof Asn1Choice choice) {
            obj.put("kind", "CHOICE");
            JSONArray options = new JSONArray();
            for (Asn1Field f : choice.getFields())
                options.put(fieldToJson(f));
            obj.put("options", options);

        } else if (el instanceof Asn1Enumerated enumerated) {
            obj.put("kind", "ENUMERATED");
            JSONArray values = new JSONArray();
            for (Asn1NamedValue nv : enumerated.getNamedValues())
                values.put(nv.getName());
            obj.put("values", values);

        } else {
            obj.put("kind", el.getClass().getSimpleName()
                    .replace("Impl", "").toUpperCase());
        }

        return obj;
    }

    private JSONObject fieldToJson(Asn1Field f) {
        JSONObject obj = new JSONObject();
        obj.put("name", f.getName());
        obj.put("type", f.getType());
        obj.put("optional", f.isOptional());
        if (f.hasDefault())
            obj.put("default", f.getDefault());
        return obj;
    }
}
