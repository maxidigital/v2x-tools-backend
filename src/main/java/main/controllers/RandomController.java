package main.controllers;

import a.MessageId;
import a.P;
import a.enums.Encoding;
import a.messages.Payload;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import i.Sequence;
import i.WindException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/command")
public class RandomController {

    @GetMapping("/random")
    public ResponseEntity<String> random(
            @RequestParam String mid,
            @RequestParam(defaultValue = "UPER") String format,
            @RequestParam(defaultValue = "false") boolean minimal) {

        MessageId messageId = MessageId.createFromStringId(mid);

        if (messageId.isUnknown()) {
            return ResponseEntity.badRequest().body(P.f("Unknown messageId(%s)", messageId));
        }

        try {
            Sequence seq = MessagesApp.getInstance().createEmptyMessage(messageId);

            if (minimal) {
                seq = MessagesApp.getInstance().initialize(seq);
            } else {
                seq = MessagesApp.getInstance().randomize(seq);
            }

            Encoding encoding;
            switch (format.toUpperCase()) {
                case "WER": encoding = Encoding.WER; break;
                case "XML": encoding = Encoding.XML; break;
                case "JSON": encoding = Encoding.JSON; break;
                default: encoding = Encoding.UPER; break;
            }

            Payload payload = MessagesApp.getInstance().encode(seq, encoding);

            String response = (encoding == Encoding.XML || encoding == Encoding.JSON)
                    ? payload.toText()
                    : payload.getHexWithEncoding();

            return ResponseEntity.ok(response);

        } catch (WindException ex) {
            Logger.getLogger(RandomController.class.getName()).log(Level.SEVERE, null, ex);
            return ResponseEntity.internalServerError().body("Error generating random message: " + ex.getMessage());
        }
    }
}