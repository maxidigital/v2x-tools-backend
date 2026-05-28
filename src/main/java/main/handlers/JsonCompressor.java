package main.handlers;

/**
 *
 * @author bott_ma
 */
public class JsonCompressor
{

    public static String compressJson(String json) {
        StringBuilder compressed = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
                compressed.append(c);
            } else if (!inQuotes) {
                if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                    compressed.append(c);
                }
            } else {
                compressed.append(c);
            }
        }

        return compressed.toString();
    }

    public static void main(String[] args) {
        String json = "{ \"example\": \"This is a JSON string with spaces and\n line breaks\" }";
        String compressedJson = compressJson(json);
        System.out.println(compressedJson);
    }
}
