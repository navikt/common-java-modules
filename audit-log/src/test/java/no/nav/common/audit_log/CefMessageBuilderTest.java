package no.nav.common.audit_log;

import no.nav.common.audit_log.cef.CefMessage;
import no.nav.common.audit_log.cef.CefMessageEvent;
import no.nav.common.audit_log.cef.CefMessageSeverity;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static no.nav.common.audit_log.cef.CefMessageExtensionFields.FIELD_SOURCE_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CefMessageBuilderTest {

    @Test
    public void should_build_cef_message() {
        Map<String, String> extensions = new HashMap<>();
        extensions.put(FIELD_SOURCE_USER_ID, "Z12345");

        CefMessage expectedMessage = new CefMessage(
                0,
                "my-application",
                "AuditLogger",
                "1.0",
                CefMessageEvent.ACCESS.type,
                "NAV-ansatt har gjort oppslag på bruker",
                CefMessageSeverity.INFO.name(),
                extensions
        );

        CefMessage newMessage = CefMessage.builder()
                .applicationName("my-application")
                .event(CefMessageEvent.ACCESS)
                .description("NAV-ansatt har gjort oppslag på bruker")
                .severity(CefMessageSeverity.INFO)
                .extensions(extensions)
                .build();

        assertEquals(expectedMessage, newMessage);
    }

    @Test
    public void should_throw_if_missing_data() {
        assertThrows(NullPointerException.class, () -> {
            CefMessage.builder()
                    .applicationName("my-application")
                    .build();
        });
    }

}
