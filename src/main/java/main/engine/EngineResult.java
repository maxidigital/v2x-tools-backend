package main.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The backend's view of the engine's typed result. No wind types. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EngineResult {
    public String status;     // "ok" | "engineNotFound" | "decodeError"
    public String data;       // ok (hex-with-encoding for UPER/WER, text for json/xml)
    public String engineId;   // engineNotFound
    public String error;      // decodeError

    public boolean isOk() { return "ok".equals(status); }
    public boolean isEngineNotFound() { return "engineNotFound".equals(status); }
}
