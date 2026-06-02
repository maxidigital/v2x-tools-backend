package main.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The backend's view of the engine's /convert response (deserialized from JSON). No wind types. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EngineResult {
    public String status;            // "ok" | "notLoaded" | "decodeError"
    public String data;              // ok
    public Integer messageId;        // notLoaded
    public Integer protocolVersion;  // notLoaded
    public String error;             // decodeError
}
