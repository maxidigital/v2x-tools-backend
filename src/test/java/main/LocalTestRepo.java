package main;

import a.WindId;
import wind_parser.i.Asn1Repo;
import wind_parser.i.Asn1RepoException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory Asn1Repo for unit tests — no network required.
 *
 * Loads ASN.1 content from classpath resources.
 * OID lookup uses a unique numeric token per module (robust to format variations
 * between the ASN.1 parser and different OID string representations).
 */
public class LocalTestRepo implements Asn1Repo {

    private record ModuleEntry(WindId windId, String content) {}
    private record OidEntry(String token, ModuleEntry entry) {}

    private final Map<String, ModuleEntry> byAlias = new HashMap<>();
    private final List<OidEntry>           byOid   = new ArrayList<>();

    public LocalTestRepo() {
        ModuleEntry cam = new ModuleEntry(
                WindId.create("de.dlr.ts.v2x", "cam_v2", "4.0"),
                loadResource("cam_v2.asn"));
        byAlias.put("cam_v2", cam);
        byOid.add(new OidEntry("302637", cam));   // en (302637) cam — unique to CAM v2

        ModuleEntry its = new ModuleEntry(
                WindId.create("de.dlr.ts.v2x", "its_container_v2", "4.0"),
                loadResource("its_container_v2.asn"));
        byAlias.put("its_container_v2", its);
        byOid.add(new OidEntry("102894", its));   // ts (102894) cdd — unique to ITS-Container v2
    }

    @Override
    public String getByAlias(String alias) throws Asn1RepoException {
        ModuleEntry e = byAlias.get(alias);
        if (e == null) throw new Asn1RepoException("Unknown alias: " + alias);
        return e.content();
    }

    @Override
    public String getByNameAndOid(String name, String oid) throws Asn1RepoException {
        for (OidEntry e : byOid)
            if (oid.contains(e.token())) return e.entry().content();
        throw new Asn1RepoException("Unknown module: " + name + " / " + oid);
    }

    @Override
    public WindId getWindIdByAlias(String alias) throws Asn1RepoException {
        ModuleEntry e = byAlias.get(alias);
        if (e == null) throw new Asn1RepoException("Unknown alias: " + alias);
        return e.windId();
    }

    @Override
    public WindId getWindIdByNameAndOid(String name, String oid) throws Asn1RepoException {
        for (OidEntry e : byOid)
            if (oid.contains(e.token())) return e.entry().windId();
        throw new Asn1RepoException("Unknown module: " + name + " / " + oid);
    }

    private static String loadResource(String name) {
        try (InputStream is = LocalTestRepo.class.getClassLoader().getResourceAsStream(name)) {
            if (is == null) throw new RuntimeException("Test resource not found: " + name);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test resource: " + name, e);
        }
    }
}
