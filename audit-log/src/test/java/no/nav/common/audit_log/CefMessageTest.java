package no.nav.common.audit_log;

import no.nav.common.audit_log.cef.CefMessage;
import no.nav.common.audit_log.cef.CefMessageEvent;
import no.nav.common.audit_log.cef.CefMessageSeverity;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static no.nav.common.audit_log.cef.CefMessageExtensionFields.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CefMessageTest {

    @Test
    public void should_create_message_with_correct_format() {
        Map<String, String> extension = new HashMap<>();
        extension.put(FIELD_SOURCE_USER_ID, "Z12345");
        extension.put(FIELD_DESTINATION_USER_ID, "12345678900");
        extension.put(FIELD_END_TIME, "1654185299215");
        extension.put(FIELD_SPROC, "0bfe8d02-93d9-4530-99ac-3debae410769");

        CefMessage cefMessage = new CefMessage(
                0,
                "my-application",
                "AuditLogger",
                "1.0",
                CefMessageEvent.ACCESS.type,
                "NAV-ansatt har gjort oppslag på bruker",
                CefMessageSeverity.INFO.name(),
                extension
        );

        String expectedStr = "CEF:0|my-application|AuditLogger|1.0|audit:access|NAV-ansatt har gjort oppslag på bruker|INFO|sproc=0bfe8d02-93d9-4530-99ac-3debae410769 duid=12345678900 end=1654185299215 suid=Z12345";

        assertEquals(expectedStr, cefMessage.toString());
    }

    @Test
    public void should_escape_fields() {
        CefMessage cefMessage = new CefMessage(
                0,
                "my-application",
                "audit \\ Logger",
                "1.0",
                CefMessageEvent.ACCESS.type,
                "Info | om eventet",
                CefMessageSeverity.INFO.name(),
                Collections.emptyMap()
        );

        String expectedStr = "CEF:0|my-application|audit \\\\ Logger|1.0|audit:access|Info \\| om eventet|INFO|";

        assertEquals(expectedStr, cefMessage.toString());
    }

    @Test
    public void should_escape_extensions() {
        Map<String, String> extension = new HashMap<>();
        extension.put(FIELD_SOURCE_USER_ID, "test=hello");
        extension.put(FIELD_DESTINATION_USER_ID, "test\\value");
        extension.put(FIELD_END_TIME, "3478329423");
        extension.put(FIELD_SPROC, "multi \n line string");

        CefMessage cefMessage = new CefMessage(
                0,
                "my-application",
                "auditLogger",
                "1.0",
                CefMessageEvent.ACCESS.type,
                "Info om eventet",
                CefMessageSeverity.INFO.name(),
                extension
        );

        String expectedStr = "CEF:0|my-application|auditLogger|1.0|audit:access|Info om eventet|INFO|sproc=multi \\n line string duid=test\\\\value end=3478329423 suid=test\\=hello";

        assertEquals(expectedStr, cefMessage.toString());
    }

}
