package main.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The backend's view of the engine's /convert response (deserialized from JSON). No wind types. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EngineResult {
    public String status;            // "ok" | "notFound" | "decodeError"
    public String data;              // ok
    public Integer messageId;        // notFound
    public Integer protocolVersion;  // notFound
    public String error;             // decodeError
}
