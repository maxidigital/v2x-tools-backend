package main;

import a.MessageId;
import a.codecs.MessageTreeExpander;
import a.generic.GenericSequence;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import de.dlr.ts.v2x.wind_generic.WindGeneric;
import i.Sequence;
import org.junit.jupiter.api.Test;
import wind_parser.WindParser;
import wind_parser.i.WindParserProject;

import static org.junit.jupiter.api.Assertions.*;

class WindEngineTest {

    /**
     * Verifica que WindParser resuelve cam_v2 + its_container_v2 desde el repo local,
     * y que WindGeneric.build() construye el árbol sin parsear de nuevo.
     * Imprime el árbol del mensaje para inspección visual.
     */
    @Test
    void loadCAMFromLocalRepo_parsesAndBuilds() throws Exception {
        LocalTestRepo repo = new LocalTestRepo();

        WindParser parser = new WindParser();
        WindParserProject project = parser.parseByAlias("cam_v2", repo);

        GenericSequence seq = WindGeneric.build(project, "CAM");

        assertNotNull(seq);
        assertEquals("CAM", seq.name());

        // Imprimir árbol del mensaje
        MessageTreeExpander expander = new MessageTreeExpander();
        expander.printExpandedStructures(expander.expandMessage(seq));
    }

    /**
     * Verifica que llamar a WindGeneric.build() dos veces sobre el mismo project
     * devuelve instancias independientes (no el mismo objeto).
     */
    @Test
    void buildTwice_returnsSeparateInstances() throws Exception {
        LocalTestRepo repo = new LocalTestRepo();

        WindParser parser = new WindParser();
        WindParserProject project = parser.parseByAlias("cam_v2", repo);

        GenericSequence s1 = WindGeneric.build(project, "CAM");
        GenericSequence s2 = WindGeneric.build(project, "CAM");

        assertNotNull(s1);
        assertNotNull(s2);
        assertNotSame(s1, s2);
        assertEquals("CAM", s1.name());
        assertEquals("CAM", s2.name());
    }

    /**
     * Verifica el flujo completo: registerMessage + createEmptyMessage.
     * Equivalente al comportamiento correcto de MessageLoader después del fix.
     */
    @Test
    void registerAndCreateEmptyMessage_works() throws Exception {
        LocalTestRepo repo = new LocalTestRepo();

        WindParser parser = new WindParser();
        WindParserProject project = parser.parseByAlias("cam_v2", repo);

        MessageId mid = MessageId.create(2, 2);  // messageId=2, protocolVersion=2
        MessagesApp.getInstance().registerMessage(mid, () -> WindGeneric.build(project, "CAM"));

        Sequence<?> seq = MessagesApp.getInstance().createEmptyMessage(mid);

        assertNotNull(seq);
        assertEquals("CAM", seq.name());
    }
}
