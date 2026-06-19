package main.hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Auto-detects which saved message a payload is, by its ETSI ITS header {@code (protocolVersion, messageId)}.
 * This is the hub's V2X-specific knowledge (the engine stays universal). The index is built from the saved
 * messages' own header fixups, so no extra data is needed: a fixup {@code header:messageID = 2} both pins the
 * header on generate AND identifies the message on decode.
 *
 * Header extraction by format:
 *   UPER/WER  → the first two octets are protocolVersion and messageId (the ItsPduHeader is the first field).
 *   JSON/XML  → read the header object's protocolVersion / messageId fields (names/casing vary; we scan).
 */
@Service
public class MessageIdentifier {

    private final SavedMessageService messages;
    private final ObjectMapper mapper = new ObjectMapper();

    public MessageIdentifier(SavedMessageService messages) {
        this.messages = messages;
    }

    /** Returns the saved-message ref for {@code payload}, or throws if the header can't be read / matched. */
    public String identify(Long userId, byte[] payload, boolean binaryIn, String from) {
        int[] pm = extractHeader(payload, binaryIn, from);
        if (pm == null)
            throw new IllegalArgumentException(
                    "could not read the header to auto-detect the message (format " + from + ") — pass X-Ref explicitly");
        String name = lookup(0L, pm[0], pm[1]);                       // public messages
        if (name == null && userId != null && userId != 0L)
            name = lookup(userId, pm[0], pm[1]);                      // then the user's own
        if (name == null)
            throw new IllegalArgumentException(
                    "no message registered for protocolVersion=" + pm[0] + " messageId=" + pm[1]);
        return name;
    }

    // ── index: (protocolVersion, messageId) → message name, from each message's header fixups ──

    private String lookup(Long userId, int pv, int mid) {
        for (Map<String, Object> m : messages.list(userId)) {
            int[] h = headerOf(m);
            if (h != null && h[0] == pv && h[1] == mid)
                return String.valueOf(m.get("name"));
        }
        return null;
    }

    private int[] headerOf(Map<String, Object> m) {
        Object f = m.get("fixups");
        if (!(f instanceof JsonNode fixups) || !fixups.isArray())
            return null;
        Integer pv = null, mid = null;
        for (JsonNode fx : fixups) {
            String path = fx.path("path").asText("");
            String field = path.contains(":") ? path.substring(path.lastIndexOf(':') + 1) : path;
            String key = field.toLowerCase().replace("_", "");
            JsonNode val = fx.path("value");
            if (!val.isInt()) continue;
            if (key.contains("protocolversion")) pv = val.asInt();
            else if (key.contains("messageid")) mid = val.asInt();
        }
        return (pv != null && mid != null) ? new int[]{pv, mid} : null;
    }

    // ── header extraction from the payload ──

    private int[] extractHeader(byte[] payload, boolean binaryIn, String from) {
        String f = from == null ? "" : from.trim().toUpperCase();
        switch (f) {
            case "UPER":
            case "WER": {
                byte[] bytes = binaryIn ? payload : hexToBytes(new String(payload, StandardCharsets.UTF_8));
                if (bytes == null || bytes.length < 2) return null;
                return new int[]{bytes[0] & 0xFF, bytes[1] & 0xFF};
            }
            case "JSON": {
                try {
                    return fromHeaderObject(mapper.readTree(new String(payload, StandardCharsets.UTF_8)));
                } catch (Exception e) {
                    return null;
                }
            }
            case "XML": {
                String s = new String(payload, StandardCharsets.UTF_8);
                Integer pv = firstInt(s, "protocolVersion");
                Integer mid = firstInt(s, "messageI[dD]");
                return (pv != null && mid != null) ? new int[]{pv, mid} : null;
            }
            default:
                return null;
        }
    }

    /** Find a top-level object that carries both a protocolVersion-ish and a messageId-ish integer field. */
    private int[] fromHeaderObject(JsonNode doc) {
        if (doc == null || !doc.isObject()) return null;
        for (JsonNode v : doc) {
            if (!v.isObject()) continue;
            Integer pv = null, mid = null;
            var it = v.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String key = e.getKey().toLowerCase().replace("_", "");
                if (!e.getValue().isInt()) continue;
                if (key.contains("protocolversion")) pv = e.getValue().asInt();
                else if (key.contains("messageid")) mid = e.getValue().asInt();
            }
            if (pv != null && mid != null) return new int[]{pv, mid};
        }
        return null;
    }

    private static byte[] hexToBytes(String s) {
        if (s == null) return null;
        String hex = s.contains(":") ? s.substring(s.indexOf(':') + 1) : s;   // drop "uper:"/"wer:" prefix
        hex = hex.replaceAll("\\s", "");
        if (hex.isEmpty() || hex.length() % 2 != 0) return null;
        try {
            return a.tools.Tools.hexStringToBytes(hex);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer firstInt(String xml, String tagRegex) {
        Matcher m = Pattern.compile("<" + tagRegex + ">\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(xml);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
}
